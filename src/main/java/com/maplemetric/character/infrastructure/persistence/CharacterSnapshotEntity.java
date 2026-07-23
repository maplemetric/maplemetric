package com.maplemetric.character.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "p_character_snapshot",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_p_character_snapshot_ocid_date",
                        columnNames = {
                                "ocid",
                                "snapshot_date"
                        }
                )
        }
)
public class CharacterSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "character_snapshot_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ocid", nullable = false, updatable = false, length = 100)
    private String ocid;

    @Column(
            name = "character_name",
            nullable = false,
            updatable = false,
            length = 30
    )
    private String characterName;

    @Column(name = "world_id", nullable = false, updatable = false)
    private UUID worldId;

    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID jobId;

    @Column(name = "character_level", nullable = false, updatable = false)
    private int characterLevel;

    @Column(name = "character_exp", updatable = false)
    private Long characterExp;

    @Column(name = "character_exp_rate", updatable = false, precision = 7, scale = 4)
    private BigDecimal characterExpRate;

    @Column(name = "guild_name", updatable = false, length = 50)
    private String guildName;

    @Column(name = "character_image_url", updatable = false, columnDefinition = "TEXT")
    private String characterImageUrl;

    @Column(name = "gender", updatable = false, length = 20)
    private String gender;

    @Column(name = "union_level", updatable = false)
    private Integer unionLevel;

    @Column(name = "union_grade", updatable = false, length = 50)
    private String unionGrade;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "symbols_data", updatable = false, columnDefinition = "jsonb")
    private JsonNode symbolsData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hexa_data", updatable = false, columnDefinition = "jsonb")
    private JsonNode hexaData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "union_data", updatable = false, columnDefinition = "jsonb")
    private JsonNode unionData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "link_skills_data", updatable = false, columnDefinition = "jsonb")
    private JsonNode linkSkillsData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hyper_stats_data", updatable = false, columnDefinition = "jsonb")
    private JsonNode hyperStatsData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profile_data", updatable = false, columnDefinition = "jsonb")
    private JsonNode profileData;

    @Column(name = "snapshot_date", nullable = false, updatable = false)
    private LocalDate snapshotDate;

    @Column(name = "collected_at", nullable = false, updatable = false)
    private Instant collectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CharacterSnapshotEntity() {
    }

    @Builder
    private CharacterSnapshotEntity(
            String ocid,
            String characterName,
            UUID worldId,
            UUID jobId,
            int characterLevel,
            Long characterExp,
            BigDecimal characterExpRate,
            String guildName,
            String characterImageUrl,
            String gender,
            Integer unionLevel,
            String unionGrade,
            JsonNode symbolsData,
            JsonNode hexaData,
            JsonNode unionData,
            JsonNode linkSkillsData,
            JsonNode hyperStatsData,
            JsonNode profileData,
            LocalDate snapshotDate,
            Instant collectedAt
    ) {
        this.ocid = requireText(
                ocid,
                "OCID는 비어 있을 수 없습니다."
        );
        this.characterName = requireText(
                characterName,
                "캐릭터명은 비어 있을 수 없습니다."
        );
        this.worldId = requireNonNull(
                worldId,
                "월드 식별자는 비어 있을 수 없습니다."
        );
        this.jobId = requireNonNull(
                jobId,
                "직업 식별자는 비어 있을 수 없습니다."
        );

        validateCharacterLevel(characterLevel);
        validateNonNegative(
                characterExp,
                "캐릭터 경험치는 0 이상이어야 합니다."
        );
        validateRate(characterExpRate);
        validateNonNegative(
                unionLevel,
                "유니온 레벨은 0 이상이어야 합니다."
        );

        this.characterLevel = characterLevel;
        this.characterExp = characterExp;
        this.characterExpRate = characterExpRate;
        this.guildName = guildName;
        this.characterImageUrl = characterImageUrl;
        this.gender = gender;
        this.unionLevel = unionLevel;
        this.unionGrade = unionGrade;
        this.symbolsData = symbolsData;
        this.hexaData = hexaData;
        this.unionData = unionData;
        this.linkSkillsData = linkSkillsData;
        this.hyperStatsData = hyperStatsData;
        this.profileData = profileData;
        this.snapshotDate = requireNonNull(
                snapshotDate,
                "Snapshot 기준일은 비어 있을 수 없습니다."
        );
        this.collectedAt = requireNonNull(
                collectedAt,
                "Snapshot 수집 시각은 비어 있을 수 없습니다."
        );
    }

    @PrePersist
    private void prePersist() {
        createdAt = Instant.now();
    }

    private static String requireText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static <T> T requireNonNull(
            T value,
            String message
    ) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static void validateCharacterLevel(int characterLevel) {
        if (characterLevel < 1 || characterLevel > 999) {
            throw new IllegalArgumentException(
                    "캐릭터 레벨은 1 이상 999 이하여야 합니다."
            );
        }
    }

    private static void validateNonNegative(
            Number value,
            String message
    ) {
        if (value != null && value.longValue() < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void validateRate(BigDecimal characterExpRate) {
        if (characterExpRate == null) {
            return;
        }

        if (characterExpRate.compareTo(BigDecimal.ZERO) < 0
                || characterExpRate.compareTo(
                        BigDecimal.valueOf(100)
                ) > 0) {
            throw new IllegalArgumentException(
                    "캐릭터 경험치 비율은 0 이상 100 이하여야 합니다."
            );
        }
    }
}
