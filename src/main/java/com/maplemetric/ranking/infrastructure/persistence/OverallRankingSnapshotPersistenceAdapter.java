package com.maplemetric.ranking.infrastructure.persistence;

import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.OverallRankingCollection;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.RankingRow;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
class OverallRankingSnapshotPersistenceAdapter
        implements SaveOverallRankingSnapshotPort {

    private final OverallRankingCollectionJpaRepository overallRankingCollectionJpaRepository;

    OverallRankingSnapshotPersistenceAdapter(
            OverallRankingCollectionJpaRepository overallRankingCollectionJpaRepository
    ) {
        this.overallRankingCollectionJpaRepository =
                overallRankingCollectionJpaRepository;
    }

    @Override
    public boolean existsOverallRankingCollection(
            LocalDate snapshotDate,
            String worldName,
            Integer worldType,
            String className
    ) {
        return overallRankingCollectionJpaRepository
                .existsBySnapshotDateAndWorldNameAndWorldTypeAndClassName(
                        snapshotDate,
                        OverallRankingCollectionEntity
                                .normalizeWorldName(worldName),
                        OverallRankingCollectionEntity
                                .normalizeWorldType(worldType),
                        OverallRankingCollectionEntity
                                .normalizeClassName(className)
                );
    }

    @Override
    public void saveOverallRankingSnapshot(
            OverallRankingCollection collection
    ) {
        OverallRankingCollectionEntity collectionEntity =
                OverallRankingCollectionEntity.create(
                        collection.snapshotDate(),
                        collection.worldName(),
                        collection.worldType(),
                        collection.className(),
                        collection.source(),
                        collection.pageCount(),
                        collection.rows().size(),
                        collection.collectedAt()
                );

        for (RankingRow row : collection.rows()) {
            collectionEntity.addSnapshot(
                    OverallRankingSnapshotEntity.create(
                            collectionEntity,
                            row.ranking(),
                            row.characterName(),
                            row.worldName(),
                            row.className(),
                            row.subClassName(),
                            row.characterLevel(),
                            row.characterExp(),
                            row.characterPopularity(),
                            row.characterGuildName()
                    )
            );
        }

        overallRankingCollectionJpaRepository.saveAndFlush(collectionEntity);
    }
}
