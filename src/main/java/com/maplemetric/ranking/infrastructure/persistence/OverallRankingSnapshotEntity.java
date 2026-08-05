package com.maplemetric.ranking.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * 종합 랭킹 한 행이다.
 *
 * 순위는 유일하지 않다. Nexon이 같은 순위를 두 행으로 돌려주는 기준일이 실제로
 * 있어서, 식별은 순위가 아니라 캐릭터(이름과 월드)로 한다.
 */
@Getter
@Entity
@Table(
        name = "p_overall_ranking_snapshot",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_p_overall_ranking_snapshot_collection_character",
                        columnNames = {
                                "overall_ranking_collection_id",
                                "character_name",
                                "world_name"
                        }
                )
        }
)
public class OverallRankingSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "overall_ranking_snapshot_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "overall_ranking_collection_id",
            nullable = false,
            updatable = false
    )
    private OverallRankingCollectionEntity collection;

    @Column(name = "ranking", nullable = false, updatable = false)
    private int ranking;

    @Column(name = "character_name", nullable = false, updatable = false, length = 30)
    private String characterName;

    @Column(name = "world_name", nullable = false, updatable = false, length = 30)
    private String worldName;

    @Column(name = "class_name", nullable = false, updatable = false, length = 50)
    private String className;

    @Column(name = "sub_class_name", updatable = false, length = 50)
    private String subClassName;

    @Column(name = "character_level", nullable = false, updatable = false)
    private int characterLevel;

    @Column(name = "character_exp", updatable = false)
    private Long characterExp;

    @Column(name = "character_popularity", updatable = false)
    private Integer characterPopularity;

    @Column(name = "character_guild_name", updatable = false, length = 50)
    private String characterGuildName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OverallRankingSnapshotEntity() {
    }

    private OverallRankingSnapshotEntity(
            OverallRankingCollectionEntity collection,
            int ranking,
            String characterName,
            String worldName,
            String className,
            String subClassName,
            int characterLevel,
            Long characterExp,
            Integer characterPopularity,
            String characterGuildName
    ) {
        this.collection = requireNonNull(
                collection,
                "종합 랭킹 수집 메타데이터는 비어 있을 수 없습니다."
        );

        if (ranking < 1) {
            throw new IllegalArgumentException(
                    "랭킹 순위는 1 이상이어야 합니다."
            );
        }

        this.ranking = ranking;
        this.characterName = requireText(
                characterName,
                "캐릭터명은 비어 있을 수 없습니다."
        );
        this.worldName = requireText(
                worldName,
                "월드명은 비어 있을 수 없습니다."
        );
        this.className = requireText(
                className,
                "직업명은 비어 있을 수 없습니다."
        );

        if (characterLevel < 1 || characterLevel > 999) {
            throw new IllegalArgumentException(
                    "캐릭터 레벨은 1 이상 999 이하여야 합니다."
            );
        }

        if (characterExp != null && characterExp < 0) {
            throw new IllegalArgumentException(
                    "캐릭터 경험치는 0 이상이어야 합니다."
            );
        }

        this.subClassName = subClassName;
        this.characterLevel = characterLevel;
        this.characterExp = characterExp;
        this.characterPopularity = characterPopularity;
        this.characterGuildName = characterGuildName;
    }

    public static OverallRankingSnapshotEntity create(
            OverallRankingCollectionEntity collection,
            int ranking,
            String characterName,
            String worldName,
            String className,
            String subClassName,
            int characterLevel,
            Long characterExp,
            Integer characterPopularity,
            String characterGuildName
    ) {
        return new OverallRankingSnapshotEntity(
                collection,
                ranking,
                characterName,
                worldName,
                className,
                subClassName,
                characterLevel,
                characterExp,
                characterPopularity,
                characterGuildName
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
}
