package com.maplemetric.ranking.infrastructure.persistence;

import com.maplemetric.ranking.application.port.out.ExpireOverallRankingSnapshotPort;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
     *
     * 두 삭제는 반드시 한 Transaction이어야 한다. 나뉘면 Snapshot만 지워지고 Collection이
     * 남아 조회에 빈 기준일이 생긴다. Transaction 없는 호출은 여기서 거부한다.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
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
