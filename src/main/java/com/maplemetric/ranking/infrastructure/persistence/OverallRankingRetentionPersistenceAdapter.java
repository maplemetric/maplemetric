package com.maplemetric.ranking.infrastructure.persistence;

import com.maplemetric.ranking.application.port.out.ExpireOverallRankingSnapshotPort;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class OverallRankingRetentionPersistenceAdapter
        implements ExpireOverallRankingSnapshotPort {

    private final OverallRankingCollectionJpaRepository collectionRepository;
    private final OverallRankingSnapshotJpaRepository snapshotRepository;

    OverallRankingRetentionPersistenceAdapter(
            OverallRankingCollectionJpaRepository collectionRepository,
            OverallRankingSnapshotJpaRepository snapshotRepository
    ) {
        this.collectionRepository = collectionRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Override
    public Optional<LocalDate> findLatestSnapshotDate() {
        return collectionRepository.findLatestSnapshotDate();
    }

    @Override
    public List<LocalDate> findExpirableSnapshotDates(
            LocalDate expireBefore,
            LocalDate retainedDate,
            int limit
    ) {
        return collectionRepository.findExpirableSnapshotDates(
                expireBefore,
                retainedDate,
                PageRequest.ofSize(limit)
        );
    }

    @Override
    public long countCollections(List<LocalDate> snapshotDates) {
        if (snapshotDates.isEmpty()) {
            return 0L;
        }

        return collectionRepository.countBySnapshotDates(snapshotDates);
    }

    @Override
    public long countSnapshots(List<LocalDate> snapshotDates) {
        if (snapshotDates.isEmpty()) {
            return 0L;
        }

        return snapshotRepository.countBySnapshotDates(snapshotDates);
    }

    /**
     * 자식인 Snapshot을 먼저 지운다.
     *
     * 순서를 뒤집으면 Foreign Key 제약에 걸려 삭제 자체가 실패한다.
     */
    @Override
    public DeletedCounts deleteBySnapshotDates(
            List<LocalDate> snapshotDates
    ) {
        if (snapshotDates.isEmpty()) {
            return new DeletedCounts(0L, 0L);
        }

        long deletedSnapshots =
                snapshotRepository.deleteBySnapshotDates(snapshotDates);

        long deletedCollections =
                collectionRepository.deleteBySnapshotDates(snapshotDates);

        return new DeletedCounts(deletedCollections, deletedSnapshots);
    }
}
