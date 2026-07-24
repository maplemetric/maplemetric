package com.maplemetric.ranking.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "p_overall_ranking_collection",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_p_overall_ranking_collection_condition",
                        columnNames = {
                                "snapshot_date",
                                "world_name",
                                "world_type",
                                "class_name"
                        }
                )
        }
)
public class OverallRankingCollectionEntity {

    static final String ALL_WORLD_NAME = "ALL";
    static final int ALL_WORLD_TYPE = -1;
    static final String ALL_CLASS_NAME = "ALL";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "overall_ranking_collection_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(name = "snapshot_date", nullable = false, updatable = false)
    private LocalDate snapshotDate;

    @Column(name = "world_name", nullable = false, updatable = false, length = 30)
    private String worldName;

    @Column(name = "world_type", nullable = false, updatable = false)
    private int worldType;

    @Column(name = "class_name", nullable = false, updatable = false, length = 50)
    private String className;

    @Column(name = "source", nullable = false, updatable = false, length = 50)
    private String source;

    @Column(name = "page_count", nullable = false, updatable = false)
    private int pageCount;

    @Column(name = "requested_max_pages", nullable = false, updatable = false)
    private int requestedMaxPages;

    @Column(name = "truncated", nullable = false, updatable = false)
    private boolean truncated;

    @Column(name = "sample_size", nullable = false, updatable = false)
    private int sampleSize;

    @Column(name = "collected_at", nullable = false, updatable = false)
    private Instant collectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(
            mappedBy = "collection",
            cascade = CascadeType.PERSIST,
            fetch = FetchType.LAZY
    )
    private List<OverallRankingSnapshotEntity> snapshots =
            new ArrayList<>();

    protected OverallRankingCollectionEntity() {
    }

    private OverallRankingCollectionEntity(
            LocalDate snapshotDate,
            String worldName,
            Integer worldType,
            String className,
            String source,
            int pageCount,
            int requestedMaxPages,
            boolean truncated,
            int sampleSize,
            Instant collectedAt
    ) {
        this.snapshotDate = requireNonNull(
                snapshotDate,
                "Snapshot 기준일은 비어 있을 수 없습니다."
        );
        this.worldName = normalizeWorldName(worldName);
        this.worldType = normalizeWorldType(worldType);
        this.className = normalizeClassName(className);
        this.source = requireText(
                source,
                "수집 출처는 비어 있을 수 없습니다."
        );

        if (pageCount < 1) {
            throw new IllegalArgumentException(
                    "수집 페이지 수는 1 이상이어야 합니다."
            );
        }

        if (requestedMaxPages < 1) {
            throw new IllegalArgumentException(
                    "요청한 최대 수집 페이지 수는 1 이상이어야 합니다."
            );
        }

        if (sampleSize < 0) {
            throw new IllegalArgumentException(
                    "표본 크기는 0 이상이어야 합니다."
            );
        }

        this.pageCount = pageCount;
        this.requestedMaxPages = requestedMaxPages;
        this.truncated = truncated;
        this.sampleSize = sampleSize;
        this.collectedAt = requireNonNull(
                collectedAt,
                "수집 시각은 비어 있을 수 없습니다."
        );
    }

    public static OverallRankingCollectionEntity create(
            LocalDate snapshotDate,
            String worldName,
            Integer worldType,
            String className,
            String source,
            int pageCount,
            int requestedMaxPages,
            boolean truncated,
            int sampleSize,
            Instant collectedAt
    ) {
        return new OverallRankingCollectionEntity(
                snapshotDate,
                worldName,
                worldType,
                className,
                source,
                pageCount,
                requestedMaxPages,
                truncated,
                sampleSize,
                collectedAt
        );
    }

    public void addSnapshot(
            OverallRankingSnapshotEntity snapshot
    ) {
        snapshots.add(
                requireNonNull(
                        snapshot,
                        "종합 랭킹 스냅샷 행은 비어 있을 수 없습니다."
                )
        );
    }

    static String normalizeWorldName(String worldName) {
        return worldName == null
                ? ALL_WORLD_NAME
                : worldName;
    }

    static int normalizeWorldType(Integer worldType) {
        return worldType == null
                ? ALL_WORLD_TYPE
                : worldType;
    }

    static String normalizeClassName(String className) {
        return className == null
                ? ALL_CLASS_NAME
                : className;
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
