package com.maplemetric.ranking.application.service;

import com.maplemetric.common.nexon.NexonApiFailure;
import com.maplemetric.ranking.application.command.CollectOverallRankingSnapshotCommand;
import com.maplemetric.ranking.application.port.out.LoadRankingListPort;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.OverallRankingCollection;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.RankingRow;
import com.maplemetric.ranking.application.result.CollectOverallRankingSnapshotResult;
import com.maplemetric.ranking.application.result.GetOverallRankingResult;
import com.maplemetric.ranking.domain.exception.RankingException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OverallRankingSnapshotCollectionService {

    private static final String SOURCE = "NEXON_OPEN_API";

    private final LoadRankingListPort loadRankingListPort;
    private final SaveOverallRankingSnapshotPort saveOverallRankingSnapshotPort;
    private final Clock clock;

    @Autowired
    public OverallRankingSnapshotCollectionService(
            LoadRankingListPort loadRankingListPort,
            SaveOverallRankingSnapshotPort saveOverallRankingSnapshotPort
    ) {
        this(
                loadRankingListPort,
                saveOverallRankingSnapshotPort,
                Clock.system(RankingDateResolver.KOREA_ZONE_ID)
        );
    }

    OverallRankingSnapshotCollectionService(
            LoadRankingListPort loadRankingListPort,
            SaveOverallRankingSnapshotPort saveOverallRankingSnapshotPort,
            Clock clock
    ) {
        this.loadRankingListPort = loadRankingListPort;
        this.saveOverallRankingSnapshotPort = saveOverallRankingSnapshotPort;
        this.clock = clock;
    }

    public CollectOverallRankingSnapshotResult collectOverallRanking(
            CollectOverallRankingSnapshotCommand command
    ) {
        LocalDate snapshotDate =
                RankingDateResolver.resolve(
                        command.date(),
                        clock
                );

        boolean alreadyCollected =
                saveOverallRankingSnapshotPort
                        .existsOverallRankingCollection(
                                snapshotDate,
                                command.worldName(),
                                command.worldType(),
                                command.className()
                        );

        if (alreadyCollected) {
            return CollectOverallRankingSnapshotResult.skipped(
                    snapshotDate
            );
        }

        List<GetOverallRankingResult.Ranking> collectedRanking =
                new ArrayList<>();

        int pageCount = 0;
        boolean truncated = true;

        for (int page = 1; page <= command.maxPages(); page++) {
            GetOverallRankingResult result =
                    loadRankingListPort.loadOverallRanking(
                            snapshotDate,
                            command.worldName(),
                            command.worldType(),
                            command.className(),
                            page
                    );

            pageCount = page;

            if (!snapshotDate.equals(result.asOf())) {
                throw new RankingException(
                        NexonApiFailure.RESPONSE_INVALID
                );
            }

            if (result.ranking().isEmpty()) {
                truncated = false;
                break;
            }

            collectedRanking.addAll(result.ranking());
        }

        List<RankingRow> rows =
                collectedRanking.stream()
                        .map(ranking -> new RankingRow(
                                ranking.ranking(),
                                ranking.characterName(),
                                ranking.worldName(),
                                ranking.className(),
                                ranking.subClassName(),
                                ranking.characterLevel(),
                                ranking.characterExp(),
                                ranking.characterPopularity(),
                                ranking.characterGuildName()
                        ))
                        .toList();

        saveOverallRankingSnapshotPort.saveOverallRankingSnapshot(
                new OverallRankingCollection(
                        snapshotDate,
                        command.worldName(),
                        command.worldType(),
                        command.className(),
                        SOURCE,
                        pageCount,
                        command.maxPages(),
                        truncated,
                        Instant.now(clock),
                        rows
                )
        );

        return CollectOverallRankingSnapshotResult.collected(
                snapshotDate,
                pageCount,
                rows.size(),
                truncated
        );
    }
}
