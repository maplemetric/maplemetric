package com.maplemetric.ranking.application.service;

import com.maplemetric.ranking.application.event.OverallRankingSnapshotStoredEvent;
import com.maplemetric.ranking.application.event.OverallRankingSnapshotStoredEvent.ObservedName;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.OverallRankingCollection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OverallRankingSnapshotStoreService {

    private final SaveOverallRankingSnapshotPort saveOverallRankingSnapshotPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    public OverallRankingSnapshotStoreService(
            SaveOverallRankingSnapshotPort saveOverallRankingSnapshotPort,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.saveOverallRankingSnapshotPort =
                saveOverallRankingSnapshotPort;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public void store(OverallRankingCollection collection) {
        saveOverallRankingSnapshotPort.saveOverallRankingSnapshot(collection);

        applicationEventPublisher.publishEvent(
                new OverallRankingSnapshotStoredEvent(
                        collection.snapshotDate(),
                        collection.rows().stream()
                                .map(row -> new ObservedName(
                                        row.className(),
                                        1L
                                ))
                                .toList(),
                        collection.rows().stream()
                                .map(row -> new ObservedName(
                                        row.worldName(),
                                        1L
                                ))
                                .toList()
                )
        );
    }
}
