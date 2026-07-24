package com.maplemetric.ranking.infrastructure.persistence;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OverallRankingCollectionJpaRepository
        extends JpaRepository<OverallRankingCollectionEntity, UUID> {

    boolean existsBySnapshotDateAndWorldNameAndWorldTypeAndClassName(
            LocalDate snapshotDate,
            String worldName,
            int worldType,
            String className
    );
}
