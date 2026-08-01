package com.maplemetric.character.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maplemetric.character.application.port.out.SaveCharacterSectionSnapshotPort;
import com.maplemetric.character.application.port.out.SaveCharacterSectionSnapshotPort.CharacterSection;
import com.maplemetric.character.application.port.out.SaveCharacterSectionSnapshotPort.CharacterSectionSnapshot;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest(
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true"
        }
)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(CharacterSectionSnapshotPersistenceAdapter.class)
class CharacterSectionSnapshotPersistenceTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final String OCID = "test-ocid";

    private static final String CHARACTER_NAME = "감점";

    private static final Instant FETCHED_AT =
            Instant.parse("2026-08-01T00:00:00Z");

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private SaveCharacterSectionSnapshotPort port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 구간별로저장하고ocid로읽는다() {
        port.save(
                OCID,
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                """
                {"combatPower":"116871666"}
                """,
                FETCHED_AT
        );
        port.save(
                OCID,
                CHARACTER_NAME,
                CharacterSection.EQUIPMENT,
                """
                {"itemEquipment":[]}
                """,
                FETCHED_AT
        );

        CharacterSectionSnapshot profile =
                port.findByOcid(OCID, CharacterSection.PROFILE)
                        .orElseThrow();

        assertThat(profile.characterName()).isEqualTo(CHARACTER_NAME);
        assertThat(profile.section())
                .isEqualTo(CharacterSection.PROFILE);
        assertThat(profile.payload()).contains("116871666");
        assertThat(profile.fetchedAt()).isEqualTo(FETCHED_AT);

        assertThat(port.findByOcid(OCID, CharacterSection.EQUIPMENT))
                .isPresent();

        // 열지 않은 탭 구간은 저장본이 없다.
        assertThat(port.findByOcid(OCID, CharacterSection.SKILL))
                .isEmpty();
    }

    @Test
    void 같은구간을다시저장하면덮어쓴다() {
        port.save(
                OCID,
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                """
                {"combatPower":"100"}
                """,
                FETCHED_AT
        );

        Instant refreshedAt = FETCHED_AT.plusSeconds(3600);

        port.save(
                OCID,
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                """
                {"combatPower":"200"}
                """,
                refreshedAt
        );

        entityManager.flush();

        CharacterSectionSnapshot stored =
                port.findByOcid(OCID, CharacterSection.PROFILE)
                        .orElseThrow();

        assertThat(stored.payload()).contains("200");
        assertThat(stored.fetchedAt()).isEqualTo(refreshedAt);

        Integer rows = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM p_character_section_snapshot
                 WHERE ocid = ? AND section = 'PROFILE'
                """,
                Integer.class,
                OCID
        );

        // 이력이 아니라 마지막 결과다. 행이 늘지 않는다.
        assertThat(rows).isEqualTo(1);
    }

    @Test
    void 캐릭터명이바뀌면갱신때같이바뀐다() {
        port.save(
                OCID,
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                "{}",
                FETCHED_AT
        );

        port.save(
                OCID,
                "새이름",
                CharacterSection.PROFILE,
                "{}",
                FETCHED_AT.plusSeconds(60)
        );

        entityManager.flush();

        assertThat(port.findByOcid(OCID, CharacterSection.PROFILE))
                .get()
                .extracting(snapshot -> snapshot.characterName())
                .isEqualTo("새이름");
    }

    @Test
    void 캐릭터명으로도읽는다() {
        port.save(
                OCID,
                CHARACTER_NAME,
                CharacterSection.SKILL,
                """
                {"hexa":{}}
                """,
                FETCHED_AT
        );

        entityManager.flush();

        Optional<CharacterSectionSnapshot> found =
                port.findByCharacterName(
                        CHARACTER_NAME,
                        CharacterSection.SKILL
                );

        assertThat(found)
                .get()
                .extracting(snapshot -> snapshot.ocid())
                .isEqualTo(OCID);
    }

    @Test
    void 같은ocid와구간을두번만들지못한다() {
        port.save(
                OCID,
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                "{}",
                FETCHED_AT
        );

        entityManager.flush();

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO p_character_section_snapshot
                    (ocid, character_name, section, payload, fetched_at)
                VALUES (?, ?, 'PROFILE', '{}'::jsonb, now())
                """,
                OCID,
                CHARACTER_NAME
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 정의되지않은구간은저장하지못한다() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO p_character_section_snapshot
                    (ocid, character_name, section, payload, fetched_at)
                VALUES (?, ?, 'UNKNOWN', '{}'::jsonb, now())
                """,
                OCID,
                CHARACTER_NAME
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 빈조회결과는저장하지못한다() {
        assertThatThrownBy(() -> port.save(
                OCID,
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                "  ",
                FETCHED_AT
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
