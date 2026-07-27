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
class JobWorldSlugBackfillMigrationTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final String SLUG_COLUMN_SCHEMA_VERSION = "11";

    private static final String SLUG_BACKFILL_SCHEMA_VERSION = "12";

    private static final List<ExpectedSlug> EXPECTED_JOB_SLUGS = List.of(
            new ExpectedSlug("다크나이트", "dark-knight"),
            new ExpectedSlug("팔라딘", "paladin"),
            new ExpectedSlug("히어로", "hero"),
            new ExpectedSlug("비숍", "bishop"),
            new ExpectedSlug("아크메이지(불,독)", "arch-mage-fire-poison"),
            new ExpectedSlug("아크메이지(썬,콜)", "arch-mage-ice-lightning"),
            new ExpectedSlug("보우마스터", "bowmaster"),
            new ExpectedSlug("신궁", "marksman"),
            new ExpectedSlug("패스파인더", "pathfinder"),
            new ExpectedSlug("나이트로드", "night-lord"),
            new ExpectedSlug("듀얼블레이더", "dual-blader"),
            new ExpectedSlug("섀도어", "shadower"),
            new ExpectedSlug("바이퍼", "viper"),
            new ExpectedSlug("캐논슈터", "cannon-shooter"),
            new ExpectedSlug("캡틴", "captain"),
            new ExpectedSlug("미하일", "mihile"),
            new ExpectedSlug("소울마스터", "soul-master"),
            new ExpectedSlug("플레임위자드", "flame-wizard"),
            new ExpectedSlug("윈드브레이커", "wind-breaker"),
            new ExpectedSlug("나이트워커", "night-walker"),
            new ExpectedSlug("스트라이커", "striker"),
            new ExpectedSlug("블래스터", "blaster"),
            new ExpectedSlug("배틀메이지", "battle-mage"),
            new ExpectedSlug("와일드헌터", "wild-hunter"),
            new ExpectedSlug("메카닉", "mechanic"),
            new ExpectedSlug("제논", "xenon"),
            new ExpectedSlug("데몬슬레이어", "demon-slayer"),
            new ExpectedSlug("데몬어벤져", "demon-avenger"),
            new ExpectedSlug("아란", "aran"),
            new ExpectedSlug("루미너스", "luminous"),
            new ExpectedSlug("에반", "evan"),
            new ExpectedSlug("메르세데스", "mercedes"),
            new ExpectedSlug("팬텀", "phantom"),
            new ExpectedSlug("은월", "eunwol"),
            new ExpectedSlug("카이저", "kaiser"),
            new ExpectedSlug("카인", "kain"),
            new ExpectedSlug("카데나", "cadena"),
            new ExpectedSlug("엔젤릭버스터", "angelic-buster"),
            new ExpectedSlug("아델", "adele"),
            new ExpectedSlug("일리움", "illium"),
            new ExpectedSlug("칼리", "khali"),
            new ExpectedSlug("아크", "ark"),
            new ExpectedSlug("렌", "ren"),
            new ExpectedSlug("라라", "lara"),
            new ExpectedSlug("호영", "hoyoung"),
            new ExpectedSlug("제로", "zero"),
            new ExpectedSlug("키네시스", "kinesis"),
            new ExpectedSlug("레테", "lethe")
    );

    private static final List<ExpectedSlug> EXPECTED_WORLD_SLUGS = List.of(
            new ExpectedSlug("오로라", "aurora"),
            new ExpectedSlug("레드", "red"),
            new ExpectedSlug("이노시스", "enosis"),
            new ExpectedSlug("유니온", "union"),
            new ExpectedSlug("스카니아", "scania"),
            new ExpectedSlug("루나", "luna"),
            new ExpectedSlug("제니스", "zenith"),
            new ExpectedSlug("크로아", "croa"),
            new ExpectedSlug("베라", "bera"),
            new ExpectedSlug("엘리시움", "elysium"),
            new ExpectedSlug("아케인", "arcane"),
            new ExpectedSlug("노바", "nova"),
            new ExpectedSlug("에오스", "eos"),
            new ExpectedSlug("핼리오스", "helios")
    );

    @Container
    private final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Test
    void V12는_직업과_월드_Slug를_명시적_매핑으로_Backfill한다() throws SQLException {
        migrateToVersion(SLUG_BACKFILL_SCHEMA_VERSION);

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            assertThat(fetchSlugs(
                    statement,
                    "SELECT job_name, job_slug FROM p_job ORDER BY display_order"
            )).containsExactlyElementsOf(EXPECTED_JOB_SLUGS);
            assertThat(fetchSlugs(
                    statement,
                    "SELECT world_name, world_slug FROM p_world ORDER BY display_order"
            )).containsExactlyElementsOf(EXPECTED_WORLD_SLUGS);

            assertThat(count(
                    statement,
                    """
                    SELECT COUNT(*)
                    FROM p_job
                    WHERE job_slug IS NULL
                       OR job_slug !~ '^[a-z0-9]+(-[a-z0-9]+)*$'
                    """
            )).isZero();
            assertThat(count(
                    statement,
                    """
                    SELECT COUNT(*)
                    FROM p_world
                    WHERE world_slug IS NULL
                       OR world_slug !~ '^[a-z0-9]+(-[a-z0-9]+)*$'
                    """
            )).isZero();
            assertThat(count(statement, "SELECT COUNT(DISTINCT job_slug) FROM p_job"))
                    .isEqualTo(EXPECTED_JOB_SLUGS.size());
            assertThat(count(statement, "SELECT COUNT(DISTINCT world_slug) FROM p_world"))
                    .isEqualTo(EXPECTED_WORLD_SLUGS.size());
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
            assertThat(count(
                    statement,
                    """
                    SELECT COUNT(*)
                    FROM information_schema.table_constraints constraint_info
                    JOIN information_schema.constraint_column_usage column_info
                      ON column_info.constraint_schema = constraint_info.constraint_schema
                     AND column_info.constraint_name = constraint_info.constraint_name
                    WHERE constraint_info.table_schema = 'public'
                      AND constraint_info.constraint_type = 'UNIQUE'
                      AND (
                          (constraint_info.table_name = 'p_job'
                              AND column_info.column_name = 'job_slug')
                          OR (constraint_info.table_name = 'p_world'
                              AND column_info.column_name = 'world_slug')
                      )
                    """
            )).isZero();
        }
    }

    @Test
    void 기존_직업_Slug가_있으면_V12는_실패한다() throws SQLException {
        migrateToVersion(SLUG_COLUMN_SCHEMA_VERSION);

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    """
                    UPDATE p_job
                    SET job_slug = 'existing-job-slug'
                    WHERE display_order = 1
                    """
            );
        }

        assertThatThrownBy(() -> migrateToVersion(SLUG_BACKFILL_SCHEMA_VERSION))
                .isInstanceOf(FlywayException.class);

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            assertThat(count(
                    statement,
                    """
                    SELECT COUNT(*)
                    FROM p_job
                    WHERE job_slug = 'existing-job-slug'
                    """
            )).isOne();
            assertThat(count(
                    statement,
                    "SELECT COUNT(*) FROM p_job WHERE job_slug IS NOT NULL"
            )).isOne();
        }
    }

    @Test
    void 기존_월드_Slug가_있으면_V12는_실패한다() throws SQLException {
        migrateToVersion(SLUG_COLUMN_SCHEMA_VERSION);

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    """
                    UPDATE p_world
                    SET world_slug = 'existing-world-slug'
                    WHERE display_order = 1
                    """
            );
        }

        assertThatThrownBy(() -> migrateToVersion(SLUG_BACKFILL_SCHEMA_VERSION))
                .isInstanceOf(FlywayException.class);

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            assertThat(count(
                    statement,
                    """
                    SELECT COUNT(*)
                    FROM p_world
                    WHERE world_slug = 'existing-world-slug'
                    """
            )).isOne();
            assertThat(count(
                    statement,
                    "SELECT COUNT(*) FROM p_world WHERE world_slug IS NOT NULL"
            )).isOne();
        }
    }

    @Test
    void 미매핑_Canonical_행이_있으면_V12는_전체_Backfill을_Rollback한다() throws SQLException {
        migrateToVersion(SLUG_COLUMN_SCHEMA_VERSION);

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    """
                    INSERT INTO p_job (job_name, job_group, display_order)
                    VALUES ('미매핑 직업', '테스트', 999)
                    """
            );
        }

        assertThatThrownBy(() -> migrateToVersion(SLUG_BACKFILL_SCHEMA_VERSION))
                .isInstanceOf(FlywayException.class);

        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            assertThat(count(
                    statement,
                    "SELECT COUNT(*) FROM p_job WHERE job_slug IS NOT NULL"
            )).isZero();
            assertThat(count(
                    statement,
                    "SELECT COUNT(*) FROM p_world WHERE world_slug IS NOT NULL"
            )).isZero();
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

    private List<ExpectedSlug> fetchSlugs(
            Statement statement,
            String sql
    ) throws SQLException {
        List<ExpectedSlug> slugs = new ArrayList<>();

        try (ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                slugs.add(new ExpectedSlug(
                        resultSet.getString(1),
                        resultSet.getString(2)
                ));
            }
        }

        return slugs;
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

    private record ExpectedSlug(
            String name,
            String slug
    ) {
    }
}
