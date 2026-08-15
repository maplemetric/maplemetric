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
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 캐릭터명 유일 제약이 이미 데이터가 있는 스키마 위로 올라오는지 확인한다.
 *
 * 최신 스키마를 새로 만들어 검증하는 테스트는 중복이 있는 기존 DB가 V19로 올라가는
 * 경로를 증명하지 못한다. 여기서는 V18까지 올린 뒤 중복을 만들고 V19를 적용한다.
 */
@Testcontainers
class CharacterSectionSnapshotNameUniqueMigrationTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final String BEFORE_NAME_UNIQUE_VERSION = "18";

    private static final String NAME_UNIQUE_VERSION = "19";

    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

    private static final String NAME = "이름1";

    @Container
    private final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Test
    void V19는_같은_이름의_중복_저장본_중_가장_최근_것만_남긴다() throws SQLException {
        migrateToVersion(BEFORE_NAME_UNIQUE_VERSION);

        insertSnapshot("ocid-old", NAME, "2026-08-01T00:00:00Z");
        insertSnapshot("ocid-new", NAME, "2026-08-15T00:00:00Z");
        insertSnapshot("ocid-older", NAME, "2026-07-01T00:00:00Z");

        migrateToVersion(NAME_UNIQUE_VERSION);

        assertThat(fetchOcids(NAME)).containsExactly("ocid-new");
    }

    /**
     * 수집 시각이 같으면 남길 행을 시각만으로 정할 수 없다.
     *
     * 기준이 결정적이지 않으면 실행할 때마다 다른 행이 남는다. 남는 행이 무엇인지보다
     * 정확히 하나만 남고 Migration이 성공하는 것이 중요하다.
     */
    @Test
    void V19는_수집_시각이_같아도_하나만_남긴다() throws SQLException {
        migrateToVersion(BEFORE_NAME_UNIQUE_VERSION);

        insertSnapshot("ocid-a", NAME, "2026-08-01T00:00:00Z");
        insertSnapshot("ocid-b", NAME, "2026-08-01T00:00:00Z");
        insertSnapshot("ocid-c", NAME, "2026-08-01T00:00:00Z");

        migrateToVersion(NAME_UNIQUE_VERSION);

        assertThat(fetchOcids(NAME)).hasSize(1);
    }

    /**
     * 구간이 다르면 같은 이름이라도 서로 다른 저장본이다.
     */
    @Test
    void V19는_구간이_다른_같은_이름을_지우지_않는다() throws SQLException {
        migrateToVersion(BEFORE_NAME_UNIQUE_VERSION);

        insertSnapshot("ocid-a", NAME, "PROFILE", "2026-08-01T00:00:00Z");
        insertSnapshot("ocid-a", NAME, "EQUIPMENT", "2026-08-01T00:00:00Z");
        insertSnapshot("ocid-a", NAME, "SKILL", "2026-08-01T00:00:00Z");

        migrateToVersion(NAME_UNIQUE_VERSION);

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            assertThat(count(
                    statement,
                    """
                    SELECT COUNT(*)
                    FROM p_character_section_snapshot
                    WHERE character_name = '%s'
                    """.formatted(NAME)
            )).isEqualTo(3);
        }
    }

    @Test
    void V19_이후에는_같은_이름과_구간을_두_번_만들지_못한다() throws SQLException {
        migrateToVersion(NAME_UNIQUE_VERSION);

        insertSnapshot("ocid-a", NAME, "2026-08-01T00:00:00Z");

        assertThatThrownBy(() ->
                insertSnapshot("ocid-b", NAME, "2026-08-02T00:00:00Z")
        )
                .isInstanceOfSatisfying(
                        SQLException.class,
                        exception -> assertThat(exception.getSQLState())
                                .isEqualTo(UNIQUE_VIOLATION_SQL_STATE)
                );
    }

    /**
     * 유일 제약이 같은 컬럼 순서의 인덱스를 만들므로 기존 일반 인덱스는 남을 이유가 없다.
     */
    @Test
    void V19는_유일_제약을_추가하고_중복된_일반_인덱스를_제거한다() throws SQLException {
        migrateToVersion(NAME_UNIQUE_VERSION);

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            assertThat(count(
                    statement,
                    """
                    SELECT COUNT(*)
                    FROM information_schema.table_constraints
                    WHERE table_schema = 'public'
                      AND constraint_type = 'UNIQUE'
                      AND constraint_name
                          = 'uk_p_character_section_snapshot_name_section'
                    """
            )).isEqualTo(1);

            assertThat(count(
                    statement,
                    """
                    SELECT COUNT(*)
                    FROM pg_indexes
                    WHERE schemaname = 'public'
                      AND indexname
                          = 'idx_p_character_section_snapshot_name'
                    """
            )).isZero();
        }
    }

    private void insertSnapshot(
            String ocid,
            String characterName,
            String fetchedAt
    ) throws SQLException {
        insertSnapshot(ocid, characterName, "PROFILE", fetchedAt);
    }

    private void insertSnapshot(
            String ocid,
            String characterName,
            String section,
            String fetchedAt
    ) throws SQLException {
        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    """
                    INSERT INTO p_character_section_snapshot
                        (ocid, character_name, section, payload, fetched_at)
                    VALUES ('%s', '%s', '%s', '{}'::jsonb, '%s')
                    """.formatted(ocid, characterName, section, fetchedAt)
            );
        }
    }

    private List<String> fetchOcids(String characterName) throws SQLException {
        List<String> ocids = new ArrayList<>();

        try (Connection connection = connect();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        """
                        SELECT ocid
                        FROM p_character_section_snapshot
                        WHERE character_name = '%s'
                          AND section = 'PROFILE'
                        ORDER BY ocid
                        """.formatted(characterName)
                )) {
            while (resultSet.next()) {
                ocids.add(resultSet.getString("ocid"));
            }
        }

        return ocids;
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
