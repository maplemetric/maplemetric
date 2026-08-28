package com.maplemetric.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 점유 표가 이미 점유된 기준일이 있는 스키마 위로 올라오는지 확인한다.
 *
 * 최신 스키마를 새로 만들어 검증하는 테스트는 점유 중인 기준일이 있는 기존 DB가
 * V21로 올라가는 경로를 증명하지 못한다. 여기서는 V20까지 올린 뒤 점유 중인 기준일을
 * 만들고 V21을 적용한다.
 */
@Testcontainers
class BackfillClaimTokenMigrationTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final String BEFORE_CLAIM_TOKEN_VERSION = "20";

    private static final String CLAIM_TOKEN_VERSION = "21";

    private static final String CHECK_VIOLATION_SQL_STATE = "23514";

    /** 배포된 기본 시도 한도다. V21이 이 값으로 판단한다. */
    private static final int MAX_ATTEMPTS = 3;

    @Container
    private final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    /**
     * 시도가 남은 점유는 다시 잡히게 풀어 둔다.
     *
     * 그 실행기는 표를 모르므로 결과를 기록하지 못한다. 그대로 두면 아무도 손대지
     * 못한 채 남는다.
     */
    @Test
    void V21은_시도가_남은_점유를_다시_잡히게_되돌린다() throws SQLException {
        migrateToVersion(BEFORE_CLAIM_TOKEN_VERSION);

        UUID jobId = insertJob();
        UUID dateId = insertDate(jobId, "2026-07-01", "RUNNING", 1);

        migrateToVersion(CLAIM_TOKEN_VERSION);

        Map<String, Object> date = fetchDate(dateId);

        assertThat(date.get("status")).isEqualTo("PENDING");
        assertThat(date.get("claim_token")).isNull();
        assertThat(date.get("finished_at")).isNull();

        // 실제로 한 번 시작했던 사실은 남는다.
        assertThat(date.get("attempt_count")).isEqualTo(1);
        assertThat(date.get("started_at")).isNotNull();
    }

    /**
     * 시도 한도에 닿은 점유는 실패로 닫는다.
     *
     * 되돌리면 이미 한도를 다 쓴 기준일이 다시 잡혀 외부를 한 번 더 호출한다.
     * 실행기의 회수 경로는 이 경우를 실패로 닫으므로 마이그레이션도 같아야 한다.
     */
    @Test
    void V21은_시도_한도에_닿은_점유를_실패로_닫는다() throws SQLException {
        migrateToVersion(BEFORE_CLAIM_TOKEN_VERSION);

        UUID jobId = insertJob();
        UUID dateId = insertDate(jobId, "2026-07-01", "RUNNING", MAX_ATTEMPTS);

        migrateToVersion(CLAIM_TOKEN_VERSION);

        Map<String, Object> date = fetchDate(dateId);

        assertThat(date.get("status")).isEqualTo("FAILED");
        assertThat(date.get("last_error_type")).isEqualTo("UNKNOWN");
        assertThat(date.get("claim_token")).isNull();
        assertThat(date.get("finished_at")).isNotNull();
    }

    /** 실패로 닫으면 Job 집계도 함께 올린다. 어긋나면 Job이 닫히지 않는다. */
    @Test
    void V21은_실패로_닫은_기준일을_Job_집계에_반영한다() throws SQLException {
        migrateToVersion(BEFORE_CLAIM_TOKEN_VERSION);

        UUID jobId = insertJob();
        insertDate(jobId, "2026-07-01", "RUNNING", MAX_ATTEMPTS);
        insertDate(jobId, "2026-07-02", "RUNNING", MAX_ATTEMPTS);

        migrateToVersion(CLAIM_TOKEN_VERSION);

        assertThat(fetchJob(jobId).get("failed_date_count")).isEqualTo(2);
    }

    /**
     * 남은 기준일이 없으면 Job도 닫는다.
     *
     * Job을 닫는 것은 결과를 기록하는 경로가 하는 일이다. 마지막 기준일을 여기서
     * 닫으면 그 경로를 더 지나지 않아 Job이 열린 채로 남는다.
     */
    @Test
    void V21은_남은_기준일이_없으면_Job도_닫는다() throws SQLException {
        migrateToVersion(BEFORE_CLAIM_TOKEN_VERSION);

        UUID jobId = insertJob();
        insertDate(jobId, "2026-07-01", "RUNNING", MAX_ATTEMPTS);

        migrateToVersion(CLAIM_TOKEN_VERSION);

        Map<String, Object> job = fetchJob(jobId);

        assertThat(job.get("status")).isEqualTo("FAILED");
        assertThat(job.get("finished_at")).isNotNull();
    }

    /** 아직 잡을 기준일이 남아 있으면 Job을 닫지 않는다. */
    @Test
    void V21은_남은_기준일이_있으면_Job을_닫지_않는다() throws SQLException {
        migrateToVersion(BEFORE_CLAIM_TOKEN_VERSION);

        UUID jobId = insertJob();
        insertDate(jobId, "2026-07-01", "RUNNING", MAX_ATTEMPTS);
        insertDate(jobId, "2026-07-02", "PENDING", 0);

        migrateToVersion(CLAIM_TOKEN_VERSION);

        Map<String, Object> job = fetchJob(jobId);

        assertThat(job.get("status")).isEqualTo("PENDING");
        assertThat(job.get("finished_at")).isNull();
    }

    /** 점유 중이 아닌 기준일에 표가 남으면 표의 의미가 흐려진다. */
    @Test
    void V21_이후에는_점유_중이_아닌_기준일에_표를_넣지_못한다()
            throws SQLException {
        migrateToVersion(CLAIM_TOKEN_VERSION);

        UUID jobId = insertJob();

        assertThatThrownBy(() -> insertDateWithToken(
                jobId,
                "PENDING",
                UUID.randomUUID()
        ))
                .isInstanceOfSatisfying(
                        SQLException.class,
                        exception -> assertThat(exception.getSQLState())
                                .isEqualTo(CHECK_VIOLATION_SQL_STATE)
                );
    }

    /** 점유 중인 기준일에 표가 없으면 누구도 결과를 기록하지 못한다. */
    @Test
    void V21_이후에는_점유_중인_기준일에_표가_없을_수_없다() throws SQLException {
        migrateToVersion(CLAIM_TOKEN_VERSION);

        UUID jobId = insertJob();

        assertThatThrownBy(() -> insertDateWithToken(
                jobId,
                "RUNNING",
                null
        ))
                .isInstanceOfSatisfying(
                        SQLException.class,
                        exception -> assertThat(exception.getSQLState())
                                .isEqualTo(CHECK_VIOLATION_SQL_STATE)
                );
    }

    private UUID insertJob() throws SQLException {
        UUID jobId = UUID.randomUUID();

        execute(
                "INSERT INTO p_overall_ranking_backfill_job"
                        + " (backfill_job_id, requested_from,"
                        + " requested_to, status)"
                        + " VALUES ('" + jobId + "', DATE '2026-07-01',"
                        + " DATE '2026-07-03', 'PENDING')"
        );

        return jobId;
    }

    private UUID insertDate(
            UUID jobId,
            String snapshotDate,
            String status,
            int attemptCount
    ) throws SQLException {
        UUID dateId = UUID.randomUUID();

        execute(
                "INSERT INTO p_overall_ranking_backfill_date"
                        + " (backfill_date_id, backfill_job_id, snapshot_date,"
                        + " status, attempt_count, started_at)"
                        + " VALUES ('" + dateId + "', '" + jobId + "',"
                        + " DATE '" + snapshotDate + "', '" + status + "',"
                        + " " + attemptCount + ", CURRENT_TIMESTAMP)"
        );

        return dateId;
    }

    private void insertDateWithToken(
            UUID jobId,
            String status,
            UUID claimToken
    ) throws SQLException {
        String token = claimToken == null
                ? "NULL"
                : "'" + claimToken + "'";

        execute(
                "INSERT INTO p_overall_ranking_backfill_date"
                        + " (backfill_date_id, backfill_job_id, snapshot_date,"
                        + " status, attempt_count, claim_token)"
                        + " VALUES ('" + UUID.randomUUID() + "',"
                        + " '" + jobId + "', DATE '2026-07-01',"
                        + " '" + status + "', 0, " + token + ")"
        );
    }

    private Map<String, Object> fetchDate(UUID dateId) throws SQLException {
        return fetchSingleRow(
                "SELECT status, attempt_count, last_error_type, claim_token,"
                        + " started_at, finished_at"
                        + " FROM p_overall_ranking_backfill_date"
                        + " WHERE backfill_date_id = '" + dateId + "'"
        );
    }

    private Map<String, Object> fetchJob(UUID jobId) throws SQLException {
        return fetchSingleRow(
                "SELECT status, failed_date_count, finished_at"
                        + " FROM p_overall_ranking_backfill_job"
                        + " WHERE backfill_job_id = '" + jobId + "'"
        );
    }

    private Map<String, Object> fetchSingleRow(String sql)
            throws SQLException {
        try (Connection connection = connect();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();

            Map<String, Object> row = new LinkedHashMap<>();
            int columnCount = resultSet.getMetaData().getColumnCount();

            for (int column = 1; column <= columnCount; column++) {
                row.put(
                        resultSet.getMetaData().getColumnName(column),
                        resultSet.getObject(column)
                );
            }

            return row;
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void migrateToVersion(String version) {
        flywayConfig().target(version).load().migrate();
    }

    private FluentConfiguration flywayConfig() {
        return Flyway.configure()
                .dataSource(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                );
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
    }
}
