package com.maplemetric.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.maplemetric.character.infrastructure.persistence.CharacterSnapshotEntity;
import com.maplemetric.character.infrastructure.persistence.CharacterSnapshotRepository;
import com.maplemetric.ranking.infrastructure.persistence.JobEntity;
import com.maplemetric.ranking.infrastructure.persistence.JobRepository;
import com.maplemetric.world.infrastructure.persistence.WorldEntity;
import com.maplemetric.world.infrastructure.persistence.WorldRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
class CharacterSnapshotPersistenceTest {

    private static final String POSTGRES_IMAGE =
            "postgres:17-alpine";

    private static final String OCID =
            "ocid-character-snapshot";

    private static final LocalDate SNAPSHOT_DATE =
            LocalDate.of(2026, 7, 22);

    private static final Instant COLLECTED_AT =
            Instant.parse("2026-07-23T02:00:00Z");

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private CharacterSnapshotRepository characterSnapshotRepository;

    @Autowired
    private WorldRepository worldRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private UUID worldId;
    private UUID jobId;

    @BeforeEach
    void setUpReferenceData() {
        WorldEntity world = worldRepository.saveAndFlush(
                WorldEntity.create(
                        "테스트루나",
                        "luna",
                        1,
                        WorldEntity.Status.ACTIVE
                )
        );

        JobEntity job = jobRepository.saveAndFlush(
                JobEntity.create(
                        "테스트팬텀",
                        "phantom",
                        "영웅",
                        "도적",
                        true,
                        1
                )
        );

        worldId = world.getId();
        jobId = job.getId();
    }

    @Test
    void 캐릭터Snapshot을저장하고OCID와기준일로조회한다() {
        CharacterSnapshotEntity snapshot =
                createSnapshot(OCID, SNAPSHOT_DATE);

        CharacterSnapshotEntity savedSnapshot =
                characterSnapshotRepository.saveAndFlush(snapshot);

        assertThat(savedSnapshot.getId()).isNotNull();
        assertThat(savedSnapshot.getCreatedAt()).isNotNull();

        entityManager.clear();

        CharacterSnapshotEntity foundSnapshot =
                characterSnapshotRepository
                        .findByOcidAndSnapshotDate(
                                OCID,
                                SNAPSHOT_DATE
                        )
                        .orElseThrow();

        assertThat(foundSnapshot.getId())
                .isEqualTo(savedSnapshot.getId());
        assertThat(foundSnapshot.getCharacterName())
                .isEqualTo("감점");
        assertThat(foundSnapshot.getWorldId())
                .isEqualTo(worldId);
        assertThat(foundSnapshot.getJobId())
                .isEqualTo(jobId);
        assertThat(foundSnapshot.getCharacterLevel())
                .isEqualTo(290);
        assertThat(foundSnapshot.getCharacterExp())
                .isEqualTo(123456789L);
        assertThat(foundSnapshot.getCharacterExpRate())
                .isEqualByComparingTo("42.1250");
        assertThat(foundSnapshot.getUnionLevel())
                .isEqualTo(9000);
        assertThat(foundSnapshot.getSnapshotDate())
                .isEqualTo(SNAPSHOT_DATE);
        assertThat(foundSnapshot.getCollectedAt())
                .isEqualTo(COLLECTED_AT);
    }

    @Test
    void JSONB상세정보를저장하고그대로조회한다() {
        CharacterSnapshotEntity snapshot =
                createSnapshot(OCID, SNAPSHOT_DATE);

        characterSnapshotRepository.saveAndFlush(snapshot);
        entityManager.clear();

        CharacterSnapshotEntity foundSnapshot =
                characterSnapshotRepository
                        .findByOcidAndSnapshotDate(
                                OCID,
                                SNAPSHOT_DATE
                        )
                        .orElseThrow();

        assertThat(
                foundSnapshot.getSymbolsData()
                        .path("symbolName")
                        .asText()
        ).isEqualTo("아케인심볼 : 소멸의 여로");
        assertThat(
                foundSnapshot.getHexaData()
                        .path("coreLevel")
                        .asInt()
        ).isEqualTo(30);
        assertThat(
                foundSnapshot.getProfileData()
                        .path("combatPower")
                        .asLong()
        ).isEqualTo(100000000L);
    }

    @Test
    void 동일OCID와기준일의Snapshot중복을허용하지않는다() {
        characterSnapshotRepository.saveAndFlush(
                createSnapshot(OCID, SNAPSHOT_DATE)
        );

        CharacterSnapshotEntity duplicateSnapshot =
                createSnapshot(OCID, SNAPSHOT_DATE);

        assertThatThrownBy(
                () -> characterSnapshotRepository.saveAndFlush(
                        duplicateSnapshot
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "true, false",
            "false, true"
    })
    void 존재하지않는월드또는직업을참조할수없다(
            boolean invalidWorld,
            boolean invalidJob
    ) {
        UUID targetWorldId = invalidWorld
                ? UUID.randomUUID()
                : worldId;
        UUID targetJobId = invalidJob
                ? UUID.randomUUID()
                : jobId;

        CharacterSnapshotEntity snapshot =
                createSnapshot(
                        OCID,
                        SNAPSHOT_DATE,
                        targetWorldId,
                        targetJobId
                );

        assertThatThrownBy(
                () -> characterSnapshotRepository.saveAndFlush(snapshot)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 0, 0",
            "1000, 0, 0, 0",
            "1, -1, 0, 0",
            "1, 0, -0.0001, 0",
            "1, 0, 100.0001, 0",
            "1, 0, 0, -1"
    })
    void 잘못된캐릭터수치를허용하지않는다(
            int characterLevel,
            Long characterExp,
            BigDecimal characterExpRate,
            Integer unionLevel
    ) {
        assertThatThrownBy(
                () -> insertSnapshot(
                        characterLevel,
                        characterExp,
                        characterExpRate,
                        unionLevel
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    private CharacterSnapshotEntity createSnapshot(
            String ocid,
            LocalDate snapshotDate
    ) {
        return createSnapshot(
                ocid,
                snapshotDate,
                worldId,
                jobId
        );
    }

    private CharacterSnapshotEntity createSnapshot(
            String ocid,
            LocalDate snapshotDate,
            UUID targetWorldId,
            UUID targetJobId
    ) {
        return CharacterSnapshotEntity.builder()
                .ocid(ocid)
                .characterName("감점")
                .worldId(targetWorldId)
                .jobId(targetJobId)
                .characterLevel(290)
                .characterExp(123456789L)
                .characterExpRate(new BigDecimal("42.1250"))
                .guildName("테스트길드")
                .characterImageUrl(
                        "https://example.com/character.png"
                )
                .gender("남")
                .unionLevel(9000)
                .unionGrade("그랜드 마스터 유니온 3")
                .symbolsData(createJson(
                        "symbolName",
                        "아케인심볼 : 소멸의 여로"
                ))
                .hexaData(createJson("coreLevel", 30))
                .unionData(createJson("artifactLevel", 50))
                .linkSkillsData(createJson(
                        "skillName",
                        "데들리 인스팅트"
                ))
                .hyperStatsData(createJson("presetNo", 1))
                .profileData(createJson(
                        "combatPower",
                        100000000L
                ))
                .snapshotDate(snapshotDate)
                .collectedAt(COLLECTED_AT)
                .build();
    }

    private JsonNode createJson(
            String fieldName,
            String value
    ) {
        return JsonNodeFactory.instance
                .objectNode()
                .put(fieldName, value);
    }

    private JsonNode createJson(
            String fieldName,
            int value
    ) {
        return JsonNodeFactory.instance
                .objectNode()
                .put(fieldName, value);
    }

    private JsonNode createJson(
            String fieldName,
            long value
    ) {
        return JsonNodeFactory.instance
                .objectNode()
                .put(fieldName, value);
    }

    private void insertSnapshot(
            int characterLevel,
            Long characterExp,
            BigDecimal characterExpRate,
            Integer unionLevel
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO p_character_snapshot (
                    ocid,
                    character_name,
                    world_id,
                    job_id,
                    character_level,
                    character_exp,
                    character_exp_rate,
                    union_level,
                    snapshot_date,
                    collected_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                OCID,
                "감점",
                worldId,
                jobId,
                characterLevel,
                characterExp,
                characterExpRate,
                unionLevel,
                SNAPSHOT_DATE,
                COLLECTED_AT.atOffset(ZoneOffset.UTC)
        );
    }
}
