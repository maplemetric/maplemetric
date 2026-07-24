package com.maplemetric.ranking.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OverallRankingSnapshotJpaRepository
        extends JpaRepository<OverallRankingSnapshotEntity, UUID> {

    List<OverallRankingSnapshotEntity> findByCollectionIdOrderByRankingAsc(
            UUID collectionId
    );
}
