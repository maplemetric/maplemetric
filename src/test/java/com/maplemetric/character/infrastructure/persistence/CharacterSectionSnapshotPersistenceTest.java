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
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
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
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
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

    record TestPayload(String value) {
    }

    @Test
    void 구간별로저장하고ocid로읽는다() {
        port.save(
                OCID,
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                new TestPayload("116871666"),
                FETCHED_AT
        );
        port.save(
                OCID,
                CHARACTER_NAME,
                CharacterSection.EQUIPMENT,
                new TestPayload("equipment"),
                FETCHED_AT
        );

        CharacterSectionSnapshot<TestPayload> profile =
                port.findByOcid(
                        OCID,
                        CharacterSection.PROFILE,
                        TestPayload.class
                ).orElseThrow();

        assertThat(profile.characterName()).isEqualTo(CHARACTER_NAME);
        assertThat(profile.section()).isEqualTo(CharacterSection.PROFILE);
        assertThat(profile.payload().value()).isEqualTo("116871666");
        assertThat(profile.fetchedAt()).isEqualTo(FETCHED_AT);

        assertThat(port.findByOcid(
                OCID,
                CharacterSection.EQUIPMENT,
                TestPayload.class
        )).isPresent();

        // 열지 않은 탭 구간은 저장본이 없다.
        assertThat(port.findByOcid(
                OCID,
                CharacterSection.SKILL,
                TestPayload.class
        )).isEmpty();
    }

    @Test
    void 같은구간을다시저장하면덮어쓴다() {
        port.save(
                OCID,
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                new TestPayload("100"),
                FETCHED_AT
        );

        Instant refreshedAt = FETCHED_AT.plusSeconds(3600);

        port.save(
                OCID,
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                new TestPayload("200"),
                refreshedAt
        );

        entityManager.flush();

        CharacterSectionSnapshot<TestPayload> stored =
                port.findByOcid(
                        OCID,
                        CharacterSection.PROFILE,
                        TestPayload.class
                ).orElseThrow();

        assertThat(stored.payload().value()).isEqualTo("200");
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
                new TestPayload("value"),
                FETCHED_AT
        );

        port.save(
                OCID,
                "새이름",
                CharacterSection.PROFILE,
                new TestPayload("value"),
                FETCHED_AT.plusSeconds(60)
        );

        entityManager.flush();

        assertThat(port.findByOcid(
                OCID,
                CharacterSection.PROFILE,
                TestPayload.class
        ))
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
                new TestPayload("hexa"),
                FETCHED_AT
        );

        entityManager.flush();

        Optional<CharacterSectionSnapshot<TestPayload>> found =
                port.findByCharacterName(
                        CHARACTER_NAME,
                        CharacterSection.SKILL,
                        TestPayload.class
                );

        assertThat(found)
                .get()
                .extracting(snapshot -> snapshot.ocid())
                .isEqualTo(OCID);
    }

    @Test
    void 읽지못하는저장본은없는것으로본다() {
        // 응답 계약이 바뀌어 예전 저장본을 더 이상 읽지 못하는 상황이다.
        jdbcTemplate.update(
                """
                INSERT INTO p_character_section_snapshot
                    (ocid, character_name, section, payload, fetched_at)
                VALUES (?, ?, 'PROFILE', '[1,2,3]'::jsonb, now())
                """,
                OCID,
                CHARACTER_NAME
        );

        // 조회 전체를 실패시키지 않고 없는 것으로 봐서 다시 수집하게 한다.
        assertThat(port.findByOcid(
                OCID,
                CharacterSection.PROFILE,
                TestPayload.class
        )).isEmpty();
    }

    @Test
    void 같은ocid와구간을두번만들지못한다() {
        port.save(
                OCID,
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                new TestPayload("value"),
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

    /**
     * 개명으로 빈 이름은 다른 캐릭터가 가져갈 수 있다.
     *
     * 이름을 이어받았는데 이전 소유자의 저장본이 남으면 같은 이름의 행이 둘이 되고,
     * 그때부터 그 이름의 조회는 다중 결과로 실패한다.
     */
    @Test
    void 이름을이어받으면이전소유자의저장본을비운다() {
        port.save(
                "이전-ocid",
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                new TestPayload("이전"),
                FETCHED_AT
        );

        port.save(
                "새-ocid",
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                new TestPayload("새것"),
                FETCHED_AT.plusSeconds(60)
        );

        entityManager.flush();

        assertThat(port.findByOcid(
                "이전-ocid",
                CharacterSection.PROFILE,
                TestPayload.class
        )).isEmpty();

        assertThat(port.findByCharacterName(
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                TestPayload.class
        ))
                .get()
                .extracting(snapshot -> snapshot.ocid())
                .isEqualTo("새-ocid");
    }

    /**
     * 늦게 도착한 오래된 수집이 방금 쓰인 최신 저장본을 밀어내면 안 된다.
     *
     * 유일 제약만으로는 소유자가 하나가 될 뿐 최신이 남는다는 보장이 없다.
     */
    @Test
    void 자기보다최신인저장본은밀어내지못한다() {
        port.save(
                "최신-ocid",
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                new TestPayload("최신"),
                FETCHED_AT.plusSeconds(3600)
        );

        entityManager.flush();

        // 이 저장은 유일 제약에 걸려 거부된다. 호출부가 삼키는 실패다.
        assertThatThrownBy(() -> {
            port.save(
                    "지연-ocid",
                    CHARACTER_NAME,
                    CharacterSection.PROFILE,
                    new TestPayload("오래된것"),
                    FETCHED_AT
            );
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * 구간이 다르면 같은 이름이라도 서로 다른 저장본이다.
     */
    @Test
    void 이름회수는같은구간에만적용한다() {
        port.save(
                "이전-ocid",
                CHARACTER_NAME,
                CharacterSection.EQUIPMENT,
                new TestPayload("장비"),
                FETCHED_AT
        );

        port.save(
                "새-ocid",
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                new TestPayload("프로필"),
                FETCHED_AT.plusSeconds(60)
        );

        entityManager.flush();

        assertThat(port.findByOcid(
                "이전-ocid",
                CharacterSection.EQUIPMENT,
                TestPayload.class
        )).isPresent();
    }

    @Test
    void 같은이름과구간을두번만들지못한다() {
        port.save(
                OCID,
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                new TestPayload("value"),
                FETCHED_AT
        );

        entityManager.flush();

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO p_character_section_snapshot
                    (ocid, character_name, section, payload, fetched_at)
                VALUES ('다른-ocid', ?, 'PROFILE', '{}'::jsonb, now())
                """,
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
    void 조회결과가없으면저장하지못한다() {
        assertThatThrownBy(() -> port.save(
                OCID,
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                null,
                FETCHED_AT
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
