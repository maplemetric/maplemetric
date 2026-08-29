package com.maplemetric.ranking.infrastructure.persistence;

import com.maplemetric.ranking.application.port.out.LoadCollectedSnapshotDatePort;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class OverallRankingCollectedDateAdapter
        implements LoadCollectedSnapshotDatePort {

    private final OverallRankingCollectionJpaRepository collectionRepository;

    OverallRankingCollectedDateAdapter(
            OverallRankingCollectionJpaRepository collectionRepository
    ) {
        this.collectionRepository = collectionRepository;
    }

    @Override
    public List<LocalDate> loadCollectedDates(LocalDate from, LocalDate to) {
        return collectionRepository.findCollectedSnapshotDates(
                from,
                to,
                OverallRankingCollectionEntity.ALL_WORLD_NAME,
                OverallRankingCollectionEntity.ALL_WORLD_TYPE,
                OverallRankingCollectionEntity.ALL_CLASS_NAME
        );
    }

    @Override
    public Optional<LocalDate> loadLatestCollectedDate() {
        return collectionRepository.findLatestCollectedSnapshotDate(
                OverallRankingCollectionEntity.ALL_WORLD_NAME,
                OverallRankingCollectionEntity.ALL_WORLD_TYPE,
                OverallRankingCollectionEntity.ALL_CLASS_NAME
        );
    }

    @Override
    public Optional<LocalDate> loadEarliestCollectedDate() {
        return collectionRepository.findEarliestCollectedSnapshotDate(
                OverallRankingCollectionEntity.ALL_WORLD_NAME,
                OverallRankingCollectionEntity.ALL_WORLD_TYPE,
                OverallRankingCollectionEntity.ALL_CLASS_NAME
        );
    }
}
