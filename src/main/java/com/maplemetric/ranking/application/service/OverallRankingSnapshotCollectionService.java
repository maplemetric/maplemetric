package com.maplemetric.ranking.application.service;

import com.maplemetric.common.nexon.NexonApiFailure;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotOutcome;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotRequest;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotUseCase;
import com.maplemetric.ranking.api.OverallRankingCollectionAlreadyRunningException;
import com.maplemetric.ranking.api.OverallRankingCollectionException;
import com.maplemetric.ranking.api.OverallRankingCollectionFailure;
import com.maplemetric.ranking.api.OverallRankingCollectionStatus;
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
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OverallRankingSnapshotCollectionService
        implements CollectOverallRankingSnapshotUseCase {

    private static final String SOURCE = "NEXON_OPEN_API";

    private final LoadRankingListPort loadRankingListPort;
    private final SaveOverallRankingSnapshotPort saveOverallRankingSnapshotPort;
    private final OverallRankingSnapshotStoreService overallRankingSnapshotStoreService;
    private final Clock clock;
    private final AtomicBoolean collectionInProgress = new AtomicBoolean(false);

    @Autowired
    public OverallRankingSnapshotCollectionService(
            LoadRankingListPort loadRankingListPort,
            SaveOverallRankingSnapshotPort saveOverallRankingSnapshotPort,
            OverallRankingSnapshotStoreService overallRankingSnapshotStoreService
    ) {
        this(
                loadRankingListPort,
                saveOverallRankingSnapshotPort,
                overallRankingSnapshotStoreService,
                Clock.system(RankingDateResolver.KOREA_ZONE_ID)
        );
    }

    OverallRankingSnapshotCollectionService(
            LoadRankingListPort loadRankingListPort,
            SaveOverallRankingSnapshotPort saveOverallRankingSnapshotPort,
            OverallRankingSnapshotStoreService overallRankingSnapshotStoreService,
            Clock clock
    ) {
        this.loadRankingListPort = loadRankingListPort;
        this.saveOverallRankingSnapshotPort = saveOverallRankingSnapshotPort;
        this.overallRankingSnapshotStoreService =
                overallRankingSnapshotStoreService;
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

        overallRankingSnapshotStoreService.store(
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

    @Override
    public CollectOverallRankingSnapshotOutcome collect(
            CollectOverallRankingSnapshotRequest request
    ) {
        if (!collectionInProgress.compareAndSet(false, true)) {
            log.warn("종합 랭킹 Snapshot 수집이 이미 실행 중입니다.");
            throw new OverallRankingCollectionAlreadyRunningException();
        }

        try {
            log.info(
                    "종합 랭킹 Snapshot 수집을 시작합니다. 기준일={}, 최대 페이지={}",
                    request.rankingDate(),
                    request.maxPages()
            );

            CollectOverallRankingSnapshotResult result =
                    collectOverallRanking(
                            new CollectOverallRankingSnapshotCommand(
                                    request.rankingDate(),
                                    null,
                                    null,
                                    null,
                                    request.maxPages()
                            )
                    );

            if (!result.collected()) {
                log.info(
                        "동일한 종합 랭킹 Snapshot이 존재하여 수집을 건너뜁니다. 기준일={}",
                        result.asOf()
                );

                return new CollectOverallRankingSnapshotOutcome(
                        OverallRankingCollectionStatus.SKIPPED,
                        result.asOf(),
                        null,
                        null,
                        null
                );
            }

            log.info(
                    "종합 랭킹 Snapshot 수집을 완료했습니다. "
                            + "기준일={}, 페이지 수={}, 표본 수={}, 잘림={}",
                    result.asOf(),
                    result.pageCount(),
                    result.sampleSize(),
                    result.truncated()
            );

            return new CollectOverallRankingSnapshotOutcome(
                    OverallRankingCollectionStatus.COLLECTED,
                    result.asOf(),
                    result.pageCount(),
                    result.sampleSize(),
                    result.truncated()
            );
        } catch (RankingException exception) {
            log.error(
                    "종합 랭킹 Snapshot 수집에 실패했습니다. 기준일={}",
                    request.rankingDate(),
                    exception
            );

            throw new OverallRankingCollectionException(
                    toCollectionFailure(exception.getFailure())
            );
        } catch (RuntimeException exception) {
            log.error(
                    "종합 랭킹 Snapshot 수집에 실패했습니다. 기준일={}",
                    request.rankingDate(),
                    exception
            );

            throw exception;
        } finally {
            collectionInProgress.set(false);
        }
    }

    private OverallRankingCollectionFailure toCollectionFailure(
            NexonApiFailure failure
    ) {
        return switch (failure) {
            case NOT_FOUND, CLIENT_ERROR ->
                    OverallRankingCollectionFailure.EXTERNAL_API_CLIENT_ERROR;
            case RATE_LIMITED ->
                    OverallRankingCollectionFailure.EXTERNAL_API_RATE_LIMITED;
            case SERVER_ERROR ->
                    OverallRankingCollectionFailure.EXTERNAL_API_SERVER_ERROR;
            case TIMEOUT ->
                    OverallRankingCollectionFailure.EXTERNAL_API_TIMEOUT;
            case RESPONSE_INVALID ->
                    OverallRankingCollectionFailure
                            .EXTERNAL_API_RESPONSE_INVALID;
        };
    }
}
