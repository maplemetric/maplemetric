package com.maplemetric.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class JobWorldSeedMigrationTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    @Container
    private final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Test
    void 빈_테이블에_Canonical_Seed가_정상_삽입된다() throws SQLException {
        Flyway.configure()
                .dataSource(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                )
                .load()
                .migrate();

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            assertThat(count(statement, "SELECT COUNT(*) FROM p_job"))
                    .isEqualTo(48);
            assertThat(count(statement, "SELECT COUNT(*) FROM p_world"))
                    .isEqualTo(14);
            assertThat(count(
                    statement,
                    "SELECT COUNT(*) FROM p_job WHERE job_branch IS NULL"
            )).isEqualTo(6);
            assertThat(count(
                    statement,
                    "SELECT COUNT(DISTINCT job_name) FROM p_job"
            )).isEqualTo(48);
            assertThat(count(
                    statement,
                    "SELECT COUNT(DISTINCT world_name) FROM p_world"
            )).isEqualTo(14);
        }
    }

    @Test
    void 기존_행이_있으면_Seed_Migration이_실패한다() throws SQLException {
        Flyway.configure()
                .dataSource(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                )
                .target("7")
                .load()
                .migrate();

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    """
                    INSERT INTO p_job (job_name, job_group, display_order)
                    VALUES ('운영에서_먼저_들어온_직업', '전사', 0)
                    """
            );
        }

        Flyway flyway = Flyway.configure()
                .dataSource(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                )
                .load();

        assertThatThrownBy(flyway::migrate)
                .isInstanceOf(FlywayException.class);
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
}
