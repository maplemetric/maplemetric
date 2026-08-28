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

    /** 배포된 기본 시도 한도다. V21이 이 값을 보지 않는다는 것을 보인다. */
    private static final int MAX_ATTEMPTS = 3;

    @Container
    private final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    /**
     * 점유된 채 남은 기준일에는 아무도 가지지 않은 표를 발급한다.
     *
     * 그 실행기들은 이 표를 모르므로 결과를 기록하지 못한다. 그것이 맞는 결과다.
     * 이미 사라진 실행기의 점유이고 결과가 어떻게 됐는지 알 수 없다.
     */
    @Test
    void V21은_남은_점유에_아무도_가지지_않은_표를_발급한다() throws SQLException {
        migrateToVersion(BEFORE_CLAIM_TOKEN_VERSION);

        UUID jobId = insertJob();
        UUID dateId = insertDate(jobId, "2026-07-01", "RUNNING", 1);

        migrateToVersion(CLAIM_TOKEN_VERSION);

        Map<String, Object> date = fetchDate(dateId);

        assertThat(date.get("claim_token")).isNotNull();

        // 상태와 시도 횟수는 건드리지 않는다. 회수 경로가 판단할 몫이다.
        assertThat(date.get("status")).isEqualTo("RUNNING");
        assertThat(date.get("attempt_count")).isEqualTo(1);
        assertThat(date.get("started_at")).isNotNull();
    }

    /**
     * 시도 한도에 닿은 점유도 여기서 닫지 않는다.
     *
     * 닫으려면 시도 한도를 알아야 하는데 마이그레이션은 설정을 읽을 수 없다.
     * 기본값으로 짐작하면 한도를 줄여 쓰는 환경에서 틀린다. 시작 시각이 이미
     * 지났으므로 다음 실행의 회수 경로가 설정된 한도로 판단한다.
     */
    @Test
    void V21은_시도_한도_판단을_회수_경로에_맡긴다() throws SQLException {
        migrateToVersion(BEFORE_CLAIM_TOKEN_VERSION);

        UUID jobId = insertJob();
        UUID dateId = insertDate(jobId, "2026-07-01", "RUNNING", MAX_ATTEMPTS);

        migrateToVersion(CLAIM_TOKEN_VERSION);

        Map<String, Object> date = fetchDate(dateId);

        assertThat(date.get("status")).isEqualTo("RUNNING");
        assertThat(date.get("attempt_count")).isEqualTo(MAX_ATTEMPTS);

        // Job 집계를 건드리지 않았으므로 회수 결과와 어긋나지 않는다.
        assertThat(fetchJob(jobId).get("failed_date_count")).isEqualTo(0);
        assertThat(fetchJob(jobId).get("status")).isEqualTo("PENDING");
    }

    /** 점유마다 다른 표를 준다. 같으면 하나를 알아낸 쪽이 다른 것도 건드린다. */
    @Test
    void V21은_점유마다_다른_표를_발급한다() throws SQLException {
        migrateToVersion(BEFORE_CLAIM_TOKEN_VERSION);

        UUID jobId = insertJob();
        UUID first = insertDate(jobId, "2026-07-01", "RUNNING", 1);
        UUID second = insertDate(jobId, "2026-07-02", "RUNNING", 1);

        migrateToVersion(CLAIM_TOKEN_VERSION);

        assertThat(fetchDate(first).get("claim_token"))
                .isNotEqualTo(fetchDate(second).get("claim_token"));
    }

    /** 점유 중이 아닌 기준일은 표 없이 그대로 둔다. */
    @Test
    void V21은_점유_중이_아닌_기준일에_표를_주지_않는다() throws SQLException {
        migrateToVersion(BEFORE_CLAIM_TOKEN_VERSION);

        UUID jobId = insertJob();
        UUID dateId = insertDate(jobId, "2026-07-01", "PENDING", 0);

        migrateToVersion(CLAIM_TOKEN_VERSION);

        assertThat(fetchDate(dateId).get("claim_token")).isNull();
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
