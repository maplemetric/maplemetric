package com.maplemetric.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.maplemetric.character.infrastructure.persistence.CharacterEquipmentSnapshotEntity;
import com.maplemetric.character.infrastructure.persistence.CharacterEquipmentSnapshotRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
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
class CharacterEquipmentSnapshotPersistenceTest {

    private static final String POSTGRES_IMAGE =
            "postgres:17-alpine";

    private static final Instant COLLECTED_AT =
            Instant.parse("2026-07-23T02:00:00Z");

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private CharacterEquipmentSnapshotRepository equipmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private UUID characterSnapshotId;

    @BeforeEach
    void setUpCharacterSnapshot() {
        UUID worldId = insertWorld();
        UUID jobId = insertJob();

        characterSnapshotId = insertCharacterSnapshot(
                worldId,
                jobId
        );
    }

    @Test
    void 프리셋장비를저장하고슬롯명순서로조회한다() {
        equipmentRepository.saveAllAndFlush(
                List.of(
                        createEquipment("무기", "제네시스 케인"),
                        createEquipment("모자", "하이네스 어새신보닛")
                )
        );

        entityManager.clear();

        List<CharacterEquipmentSnapshotEntity> equipment =
                equipmentRepository
                        .findAllByCharacterSnapshotIdAndEquipmentPresetNoOrderBySlotNameAsc(
                                characterSnapshotId,
                                (short) 1
                        );

        assertThat(equipment)
                .extracting(item -> item.getSlotName())
                .containsExactly("모자", "무기");

        CharacterEquipmentSnapshotEntity weapon = equipment.get(1);

        assertThat(weapon.getCharacterSnapshotId())
                .isEqualTo(characterSnapshotId);
        assertThat(weapon.getEquipmentPresetNo())
                .isEqualTo((short) 1);
        assertThat(weapon.getItemName())
                .isEqualTo("제네시스 케인");
        assertThat(weapon.getItemLevel())
                .isEqualTo(200);
        assertThat(weapon.getStarforce())
                .isEqualTo(22);
        assertThat(weapon.getCreatedAt()).isNotNull();
        assertThat(
                weapon.getEquipmentData()
                        .path("str")
                        .asInt()
        ).isEqualTo(150);
    }

    @Test
    void 동일Snapshot프리셋슬롯중복을허용하지않는다() {
        equipmentRepository.saveAndFlush(
                createEquipment("무기", "제네시스 케인")
        );

        CharacterEquipmentSnapshotEntity duplicateEquipment =
                createEquipment("무기", "아케인셰이드 케인");

        assertThatThrownBy(
                () -> equipmentRepository.saveAndFlush(
                        duplicateEquipment
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 존재하지않는캐릭터Snapshot을참조할수없다() {
        CharacterEquipmentSnapshotEntity equipment =
                createEquipment(
                        UUID.randomUUID(),
                        "무기",
                        "제네시스 케인"
                );

        assertThatThrownBy(
                () -> equipmentRepository.saveAndFlush(equipment)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 0",
            "4, 0, 0",
            "1, -1, 0",
            "1, 0, -1"
    })
    void 잘못된프리셋과장비수치를허용하지않는다(
            int equipmentPresetNo,
            Integer itemLevel,
            Integer starforce
    ) {
        assertThatThrownBy(
                () -> insertEquipment(
                        equipmentPresetNo,
                        itemLevel,
                        starforce
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    private CharacterEquipmentSnapshotEntity createEquipment(
            String slotName,
            String itemName
    ) {
        return createEquipment(
                characterSnapshotId,
                slotName,
                itemName
        );
    }

    private CharacterEquipmentSnapshotEntity createEquipment(
            UUID targetCharacterSnapshotId,
            String slotName,
            String itemName
    ) {
        return CharacterEquipmentSnapshotEntity.builder()
                .characterSnapshotId(targetCharacterSnapshotId)
                .equipmentPresetNo(1)
                .slotName(slotName)
                .itemName(itemName)
                .itemIconUrl("https://example.com/item.png")
                .itemDescription("테스트 장비")
                .itemLevel(200)
                .starforce(22)
                .potentialGrade("레전드리")
                .additionalPotentialGrade("유니크")
                .equipmentData(createEquipmentData())
                .build();
    }

    private JsonNode createEquipmentData() {
        return JsonNodeFactory.instance
                .objectNode()
                .put("str", 150)
                .put("dex", 120)
                .put("attackPower", 45);
    }

    private UUID insertWorld() {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO p_world (
                    world_name,
                    world_slug,
                    external_world_code
                ) VALUES (?, ?, ?)
                RETURNING world_id
                """,
                UUID.class,
                "테스트루나",
                "test-luna",
                "luna"
        );
    }

    private UUID insertJob() {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO p_job (
                    job_name,
                    job_slug,
                    external_job_code,
                    job_group
                ) VALUES (?, ?, ?, ?)
                RETURNING job_id
                """,
                UUID.class,
                "테스트팬텀",
                "test-phantom",
                "phantom",
                "영웅"
        );
    }

    private UUID insertCharacterSnapshot(
            UUID worldId,
            UUID jobId
    ) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO p_character_snapshot (
                    ocid,
                    character_name,
                    world_id,
                    job_id,
                    character_level,
                    snapshot_date,
                    collected_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING character_snapshot_id
                """,
                UUID.class,
                "ocid-character-equipment-snapshot",
                "감점",
                worldId,
                jobId,
                290,
                LocalDate.of(2026, 7, 22),
                COLLECTED_AT.atOffset(ZoneOffset.UTC)
        );
    }

    private void insertEquipment(
            int equipmentPresetNo,
            Integer itemLevel,
            Integer starforce
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO p_character_equipment_snapshot (
                    character_snapshot_id,
                    equipment_preset_no,
                    slot_name,
                    item_name,
                    item_level,
                    starforce,
                    equipment_data
                ) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS JSONB))
                """,
                characterSnapshotId,
                (short) equipmentPresetNo,
                "무기",
                "테스트장비",
                itemLevel,
                starforce,
                """
                {
                  "attackPower": 1
                }
                """
        );
    }
}
