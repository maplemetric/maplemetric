package com.maplemetric.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class JobWorldSeedMigrationTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final List<ExpectedJob> EXPECTED_JOBS = List.of(
            new ExpectedJob("다크나이트", "모험가", "전사", true, 1),
            new ExpectedJob("팔라딘", "모험가", "전사", true, 2),
            new ExpectedJob("히어로", "모험가", "전사", true, 3),
            new ExpectedJob("비숍", "모험가", "마법사", true, 4),
            new ExpectedJob("아크메이지(불,독)", "모험가", "마법사", true, 5),
            new ExpectedJob("아크메이지(썬,콜)", "모험가", "마법사", true, 6),
            new ExpectedJob("보우마스터", "모험가", "궁수", true, 7),
            new ExpectedJob("신궁", "모험가", "궁수", true, 8),
            new ExpectedJob("패스파인더", "모험가", "궁수", true, 9),
            new ExpectedJob("나이트로드", "모험가", "도적", true, 10),
            new ExpectedJob("듀얼블레이더", "모험가", "도적", true, 11),
            new ExpectedJob("섀도어", "모험가", "도적", true, 12),
            new ExpectedJob("바이퍼", "모험가", "해적", true, 13),
            new ExpectedJob("캐논슈터", "모험가", "해적", true, 14),
            new ExpectedJob("캡틴", "모험가", "해적", true, 15),
            new ExpectedJob("미하일", "시그너스", "전사", true, 16),
            new ExpectedJob("소울마스터", "시그너스", "전사", true, 17),
            new ExpectedJob("플레임위자드", "시그너스", "마법사", true, 18),
            new ExpectedJob("윈드브레이커", "시그너스", "궁수", true, 19),
            new ExpectedJob("나이트워커", "시그너스", "도적", true, 20),
            new ExpectedJob("스트라이커", "시그너스", "해적", true, 21),
            new ExpectedJob("블래스터", "레지스탕스", "전사", true, 22),
            new ExpectedJob("배틀메이지", "레지스탕스", "마법사", true, 23),
            new ExpectedJob("와일드헌터", "레지스탕스", "궁수", true, 24),
            new ExpectedJob("메카닉", "레지스탕스", "해적", true, 25),
            new ExpectedJob("제논", "레지스탕스", "해적", true, 26),
            new ExpectedJob("데몬슬레이어", "데몬", "전사", true, 27),
            new ExpectedJob("데몬어벤져", "데몬", "전사", true, 28),
            new ExpectedJob("아란", "영웅", "전사", true, 29),
            new ExpectedJob("루미너스", "영웅", "마법사", true, 30),
            new ExpectedJob("에반", "영웅", "마법사", true, 31),
            new ExpectedJob("메르세데스", "영웅", "궁수", true, 32),
            new ExpectedJob("팬텀", "영웅", "도적", true, 33),
            new ExpectedJob("은월", "영웅", "해적", true, 34),
            new ExpectedJob("카이저", "노바", "전사", true, 35),
            new ExpectedJob("카인", "노바", "궁수", true, 36),
            new ExpectedJob("카데나", "노바", "도적", true, 37),
            new ExpectedJob("엔젤릭버스터", "노바", "해적", true, 38),
            new ExpectedJob("아델", "레프", "전사", true, 39),
            new ExpectedJob("일리움", "레프", "마법사", true, 40),
            new ExpectedJob("칼리", "레프", "도적", true, 41),
            new ExpectedJob("아크", "레프", "해적", true, 42),
            new ExpectedJob("렌", "아니마", "전사", true, 43),
            new ExpectedJob("라라", "아니마", "마법사", true, 44),
            new ExpectedJob("호영", "아니마", "도적", true, 45),
            new ExpectedJob("제로", "초월자", "전사", true, 46),
            new ExpectedJob("키네시스", "프렌즈 월드", "마법사", true, 47),
            new ExpectedJob("레테", "마족", "마법사", true, 48)
    );

    private static final List<ExpectedWorld> EXPECTED_WORLDS = List.of(
            new ExpectedWorld("오로라", "ACTIVE", 1),
            new ExpectedWorld("레드", "ACTIVE", 2),
            new ExpectedWorld("이노시스", "ACTIVE", 3),
            new ExpectedWorld("유니온", "ACTIVE", 4),
            new ExpectedWorld("스카니아", "ACTIVE", 5),
            new ExpectedWorld("루나", "ACTIVE", 6),
            new ExpectedWorld("제니스", "ACTIVE", 7),
            new ExpectedWorld("크로아", "ACTIVE", 8),
            new ExpectedWorld("베라", "ACTIVE", 9),
            new ExpectedWorld("엘리시움", "ACTIVE", 10),
            new ExpectedWorld("아케인", "ACTIVE", 11),
            new ExpectedWorld("노바", "ACTIVE", 12),
            new ExpectedWorld("에오스", "ACTIVE", 13),
            new ExpectedWorld("핼리오스", "ACTIVE", 14)
    );

    @Container
    private final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Test
    void 빈_테이블에_승인된_Canonical_Seed가_그대로_삽입된다() throws SQLException {
        migrateToLatest();

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            assertThat(fetchJobs(statement))
                    .containsExactlyElementsOf(EXPECTED_JOBS);
            assertThat(fetchWorlds(statement))
                    .containsExactlyElementsOf(EXPECTED_WORLDS);

            assertThat(count(statement, "SELECT COUNT(DISTINCT job_name) FROM p_job"))
                    .isEqualTo(EXPECTED_JOBS.size());
            assertThat(count(statement, "SELECT COUNT(DISTINCT world_name) FROM p_world"))
                    .isEqualTo(EXPECTED_WORLDS.size());
            assertThat(count(statement, "SELECT COUNT(*) FROM p_job WHERE job_group IS NULL"))
                    .isZero();
            assertThat(count(statement, "SELECT COUNT(*) FROM p_job WHERE job_branch IS NULL"))
                    .isZero();
        }
    }

    @Test
    void p_job에_기존_행이_있으면_Seed_Migration이_실패한다() throws SQLException {
        migrateToVersion("7");

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    """
                    INSERT INTO p_job (job_name, job_group, display_order)
                    VALUES ('운영에서_먼저_들어온_직업', '전사', 0)
                    """
            );
        }

        assertThatThrownBy(this::migrateToLatest)
                .isInstanceOf(FlywayException.class);
    }

    @Test
    void p_world에_기존_행이_있으면_Seed_Migration이_실패한다() throws SQLException {
        migrateToVersion("7");

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    """
                    INSERT INTO p_world (world_name, display_order, status)
                    VALUES ('운영에서_먼저_들어온_월드', 0, 'ACTIVE')
                    """
            );
        }

        assertThatThrownBy(this::migrateToLatest)
                .isInstanceOf(FlywayException.class);
    }

    private void migrateToLatest() {
        flywayConfig().load().migrate();
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

    private int count(
            Statement statement,
            String sql
    ) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private List<ExpectedJob> fetchJobs(
            Statement statement
    ) throws SQLException {
        List<ExpectedJob> jobs = new ArrayList<>();

        try (ResultSet resultSet = statement.executeQuery(
                """
                SELECT job_name, job_group, job_branch, is_available, display_order
                FROM p_job
                ORDER BY display_order
                """
        )) {
            while (resultSet.next()) {
                jobs.add(new ExpectedJob(
                        resultSet.getString("job_name"),
                        resultSet.getString("job_group"),
                        resultSet.getString("job_branch"),
                        resultSet.getBoolean("is_available"),
                        resultSet.getInt("display_order")
                ));
            }
        }

        return jobs;
    }

    private List<ExpectedWorld> fetchWorlds(
            Statement statement
    ) throws SQLException {
        List<ExpectedWorld> worlds = new ArrayList<>();

        try (ResultSet resultSet = statement.executeQuery(
                """
                SELECT world_name, status, display_order
                FROM p_world
                ORDER BY display_order
                """
        )) {
            while (resultSet.next()) {
                worlds.add(new ExpectedWorld(
                        resultSet.getString("world_name"),
                        resultSet.getString("status"),
                        resultSet.getInt("display_order")
                ));
            }
        }

        return worlds;
    }

    private record ExpectedJob(
            String jobName,
            String jobGroup,
            String jobBranch,
            boolean available,
            int displayOrder
    ) {
    }

    private record ExpectedWorld(
            String worldName,
            String status,
            int displayOrder
    ) {
    }
}
