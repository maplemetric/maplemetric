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
class JobWorldSlugConstraintMigrationTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final String SLUG_BACKFILL_SCHEMA_VERSION = "12";

    private static final String SLUG_CONSTRAINT_SCHEMA_VERSION = "13";

    private static final String NOT_NULL_VIOLATION_SQL_STATE = "23502";

    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

    private static final List<ExpectedConstraint> EXPECTED_CONSTRAINTS = List.of(
            new ExpectedConstraint("p_job", "uk_p_job_job_slug"),
            new ExpectedConstraint("p_world", "uk_p_world_world_slug")
    );

    @Container
    private final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Test
    void V13은_직업과_월드_Slug에_Unique와_NotNull을_적용한다() throws SQLException {
        migrateToVersion(SLUG_CONSTRAINT_SCHEMA_VERSION);

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            assertThat(fetchSlugUniqueConstraints(statement))
                    .containsExactlyElementsOf(EXPECTED_CONSTRAINTS);
            assertThat(count(
                    statement,
                    """
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND (
                          (table_name = 'p_job' AND column_name = 'job_slug')
                          OR (table_name = 'p_world' AND column_name = 'world_slug')
                      )
                      AND is_nullable = 'NO'
                    """
            )).isEqualTo(2);
        }
    }

    @Test
    void V13은_NULL과_중복_Slug_쓰기를_거부한다() throws SQLException {
        migrateToVersion(SLUG_CONSTRAINT_SCHEMA_VERSION);

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            assertSqlState(
                    () -> statement.executeUpdate(
                            "UPDATE p_job SET job_slug = NULL WHERE job_slug = 'hero'"
                    ),
                    NOT_NULL_VIOLATION_SQL_STATE
            );
            assertSqlState(
                    () -> statement.executeUpdate(
                            "UPDATE p_world SET world_slug = NULL WHERE world_slug = 'scania'"
                    ),
                    NOT_NULL_VIOLATION_SQL_STATE
            );
            assertSqlState(
                    () -> statement.executeUpdate(
                            "UPDATE p_job SET job_slug = 'hero' WHERE job_slug = 'paladin'"
                    ),
                    UNIQUE_VIOLATION_SQL_STATE
            );
            assertSqlState(
                    () -> statement.executeUpdate(
                            "UPDATE p_world SET world_slug = 'scania' WHERE world_slug = 'luna'"
                    ),
                    UNIQUE_VIOLATION_SQL_STATE
            );
        }
    }

    @Test
    void NULL_Slug가_있으면_V13은_제약을_적용하지_않는다() throws SQLException {
        migrateToVersion(SLUG_BACKFILL_SCHEMA_VERSION);

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "UPDATE p_job SET job_slug = NULL WHERE job_slug = 'hero'"
            );
        }

        assertThatThrownBy(() -> migrateToVersion(SLUG_CONSTRAINT_SCHEMA_VERSION))
                .isInstanceOf(FlywayException.class);

        assertConstraintsNotApplied();
    }

    @Test
    void 중복_Slug가_있으면_V13은_제약을_적용하지_않는다() throws SQLException {
        migrateToVersion(SLUG_BACKFILL_SCHEMA_VERSION);

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "UPDATE p_job SET job_slug = 'hero' WHERE job_slug = 'paladin'"
            );
        }

        assertThatThrownBy(() -> migrateToVersion(SLUG_CONSTRAINT_SCHEMA_VERSION))
                .isInstanceOf(FlywayException.class);

        assertConstraintsNotApplied();
    }

    private void assertConstraintsNotApplied() throws SQLException {
        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            assertThat(fetchSlugUniqueConstraints(statement)).isEmpty();
            assertThat(count(
                    statement,
                    """
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND (
                          (table_name = 'p_job' AND column_name = 'job_slug')
                          OR (table_name = 'p_world' AND column_name = 'world_slug')
                      )
                      AND is_nullable = 'YES'
                    """
            )).isEqualTo(2);
        }
    }

    private void assertSqlState(
            SqlOperation operation,
            String expectedSqlState
    ) {
        assertThatThrownBy(operation::execute)
                .isInstanceOfSatisfying(
                        SQLException.class,
                        exception -> assertThat(exception.getSQLState())
                                .isEqualTo(expectedSqlState)
                );
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

    private List<ExpectedConstraint> fetchSlugUniqueConstraints(
            Statement statement
    ) throws SQLException {
        List<ExpectedConstraint> constraints = new ArrayList<>();

        try (ResultSet resultSet = statement.executeQuery(
                """
                SELECT table_name, constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND constraint_type = 'UNIQUE'
                  AND constraint_name IN (
                      'uk_p_job_job_slug',
                      'uk_p_world_world_slug'
                  )
                ORDER BY table_name
                """
        )) {
            while (resultSet.next()) {
                constraints.add(new ExpectedConstraint(
                        resultSet.getString("table_name"),
                        resultSet.getString("constraint_name")
                ));
            }
        }

        return constraints;
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

    @FunctionalInterface
    private interface SqlOperation {

        void execute() throws SQLException;
    }

    private record ExpectedConstraint(
            String tableName,
            String constraintName
    ) {
    }
}
