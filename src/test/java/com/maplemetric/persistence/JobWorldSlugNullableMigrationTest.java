package com.maplemetric.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class JobWorldSlugNullableMigrationTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final String PREVIOUS_SCHEMA_VERSION = "10";

    private static final int EXPECTED_JOB_COUNT = 48;

    private static final int EXPECTED_WORLD_COUNT = 14;

    @Container
    private final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Test
    void V11은_직업과_월드_Slug를_Nullable_Varchar80으로_추가한다() throws SQLException {
        migrateToVersion(PREVIOUS_SCHEMA_VERSION);

        try (Connection connection = connect()) {
            assertThat(columnExists(connection, "p_job", "job_slug")).isFalse();
            assertThat(columnExists(connection, "p_world", "world_slug")).isFalse();
        }

        migrateToLatest();

        try (Connection connection = connect()) {
            assertThat(columnDefinition(connection, "p_job", "job_slug"))
                    .isEqualTo(new ColumnDefinition(
                            "character varying",
                            80,
                            true,
                            null
                    ));
            assertThat(columnDefinition(connection, "p_world", "world_slug"))
                    .isEqualTo(new ColumnDefinition(
                            "character varying",
                            80,
                            true,
                            null
                    ));
        }
    }

    @Test
    void V11은_기존_직업과_월드_행을_Backfill하지_않는다() throws SQLException {
        migrateToLatest();

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            assertThat(count(statement, "SELECT COUNT(*) FROM p_job"))
                    .isEqualTo(EXPECTED_JOB_COUNT);
            assertThat(count(statement, "SELECT COUNT(*) FROM p_world"))
                    .isEqualTo(EXPECTED_WORLD_COUNT);
            assertThat(count(statement, "SELECT COUNT(*) FROM p_job WHERE job_slug IS NOT NULL"))
                    .isZero();
            assertThat(count(statement, "SELECT COUNT(*) FROM p_world WHERE world_slug IS NOT NULL"))
                    .isZero();
        }
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

    private boolean columnExists(
            Connection connection,
            String tableName,
            String columnName
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = ?
                      AND column_name = ?
                )
                """
        )) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        }
    }

    private ColumnDefinition columnDefinition(
            Connection connection,
            String tableName,
            String columnName
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT data_type,
                       character_maximum_length,
                       is_nullable,
                       column_default
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = ?
                """
        )) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return new ColumnDefinition(
                        resultSet.getString("data_type"),
                        resultSet.getInt("character_maximum_length"),
                        "YES".equals(resultSet.getString("is_nullable")),
                        resultSet.getString("column_default")
                );
            }
        }
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

    private record ColumnDefinition(
            String dataType,
            int maximumLength,
            boolean nullable,
            String defaultValue
    ) {
    }
}
