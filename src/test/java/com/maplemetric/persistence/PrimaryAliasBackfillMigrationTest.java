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
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class PrimaryAliasBackfillMigrationTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final String ALIAS_SCHEMA_VERSION = "9";

    private static final int EXPECTED_JOB_COUNT = 48;

    private static final int EXPECTED_WORLD_COUNT = 14;

    @Container
    private final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Test
    void Canonical직업과월드마다PRIMARY_Alias를하나씩생성한다() throws SQLException {
        migrateToLatest();

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            assertThat(count(statement, "SELECT COUNT(*) FROM p_job_alias"))
                    .isEqualTo(EXPECTED_JOB_COUNT);
            assertThat(count(statement, "SELECT COUNT(*) FROM p_world_alias"))
                    .isEqualTo(EXPECTED_WORLD_COUNT);

            assertThat(count(
                    statement,
                    """
                    SELECT COUNT(*) FROM p_job_alias
                    WHERE alias_type <> 'PRIMARY'
                    """
            )).isZero();
            assertThat(count(
                    statement,
                    """
                    SELECT COUNT(*) FROM p_world_alias
                    WHERE alias_type <> 'PRIMARY'
                    """
            )).isZero();

            assertThat(count(
                    statement,
                    "SELECT COUNT(DISTINCT job_id) FROM p_job_alias"
            )).isEqualTo(EXPECTED_JOB_COUNT);
            assertThat(count(
                    statement,
                    "SELECT COUNT(DISTINCT world_id) FROM p_world_alias"
            )).isEqualTo(EXPECTED_WORLD_COUNT);

            assertThat(count(
                    statement,
                    "SELECT COUNT(*) FROM p_job_alias WHERE deleted_at IS NOT NULL"
            )).isZero();
            assertThat(count(
                    statement,
                    "SELECT COUNT(*) FROM p_world_alias WHERE deleted_at IS NOT NULL"
            )).isZero();
        }
    }

    @Test
    void PRIMARY_Alias이름이Canonical이름과정확히일치한다() throws SQLException {
        migrateToLatest();

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            assertThat(count(
                    statement,
                    """
                    SELECT COUNT(*)
                    FROM p_job_alias a
                    JOIN p_job j ON j.job_id = a.job_id
                    WHERE a.alias_name <> j.job_name
                    """
            )).isZero();

            assertThat(count(
                    statement,
                    """
                    SELECT COUNT(*)
                    FROM p_world_alias a
                    JOIN p_world w ON w.world_id = a.world_id
                    WHERE a.alias_name <> w.world_name
                    """
            )).isZero();
        }
    }

    @Test
    void p_job_alias에_기존_행이_있으면_Backfill이_실패한다() throws SQLException {
        migrateToVersion(ALIAS_SCHEMA_VERSION);

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    """
                    INSERT INTO p_job_alias (job_id, alias_name, alias_type)
                    SELECT job_id, '운영에서_먼저_들어온_직업_Alias', 'MANUAL'
                    FROM p_job
                    LIMIT 1
                    """
            );
        }

        assertThatThrownBy(this::migrateToLatest)
                .isInstanceOf(FlywayException.class);
    }

    @Test
    void p_world_alias에_기존_행이_있으면_Backfill이_실패한다() throws SQLException {
        migrateToVersion(ALIAS_SCHEMA_VERSION);

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    """
                    INSERT INTO p_world_alias (world_id, alias_name, alias_type)
                    SELECT world_id, '먼저_들어온_월드_Alias', 'MANUAL'
                    FROM p_world
                    LIMIT 1
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
}
