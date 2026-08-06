package com.maplemetric.statistics.application.service;

import com.maplemetric.ranking.api.OverallRankingComparisonQuery;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryException;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryFailure;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsHistoryQuery;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsHistoryQueryException;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot;
import com.maplemetric.statistics.application.exception.WorldStatisticsException;
import com.maplemetric.statistics.application.exception.WorldStatisticsFailure;
import com.maplemetric.statistics.application.result.GetWorldStatisticsDetailResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsHistoryResult;
import com.maplemetric.world.api.CanonicalWorld;
import com.maplemetric.world.api.WorldCatalogQuery;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorldStatisticsDetailQueryService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final OverallRankingComparisonQuery overallRankingComparisonQuery;
    private final OverallRankingWorldStatisticsHistoryQuery
            overallRankingWorldStatisticsHistoryQuery;
    private final WorldCatalogQuery worldCatalogQuery;

    public WorldStatisticsDetailQueryService(
            OverallRankingComparisonQuery overallRankingComparisonQuery,
            OverallRankingWorldStatisticsHistoryQuery
                    overallRankingWorldStatisticsHistoryQuery,
            WorldCatalogQuery worldCatalogQuery
    ) {
        this.overallRankingComparisonQuery = overallRankingComparisonQuery;
        this.overallRankingWorldStatisticsHistoryQuery =
                overallRankingWorldStatisticsHistoryQuery;
        this.worldCatalogQuery = worldCatalogQuery;
    }

    public GetWorldStatisticsDetailResult getWorldStatisticsDetail(
            String worldSlug
    ) {
        CanonicalWorld canonicalWorld = loadCanonicalWorld(worldSlug);

        OverallRankingWorldStatisticsComparisonSnapshot rankingComparison;

        try {
            rankingComparison =
                    overallRankingComparisonQuery
                            .getWorldStatisticsComparison();
        } catch (OverallRankingComparisonQueryException exception) {
            if (exception.getFailure()
                    == OverallRankingComparisonQueryFailure.NOT_FOUND) {
                return GetWorldStatisticsDetailResult.notCollected(
                        canonicalWorld
                );
            }

            throw dataInvalid(exception);
        }

        Map<String, CanonicalWorld> canonicalWorldsByWorldName =
                worldCatalogQuery.resolveAliases(
                        collectWorldNames(rankingComparison)
                );

        return GetWorldStatisticsDetailResult.available(
                canonicalWorld,
                rankingComparison,
                canonicalWorldsByWorldName
        );
    }

    public GetWorldStatisticsHistoryResult getWorldStatisticsHistory(
            String worldSlug,
            String range,
            LocalDate from,
            LocalDate to
    ) {
        StatisticsHistoryRequest historyRequest =
                StatisticsHistoryRequest.from(
                        range,
                        from,
                        to,
                        LocalDate.now(KOREA_ZONE),
                        () -> new WorldStatisticsException(
                                WorldStatisticsFailure.INVALID_HISTORY_REQUEST
                        )
                );

        CanonicalWorld canonicalWorld = loadCanonicalWorld(worldSlug);

        OverallRankingWorldStatisticsComparisonSnapshot latestComparison;

        try {
            latestComparison =
                    overallRankingComparisonQuery
                            .getWorldStatisticsComparison();
        } catch (OverallRankingComparisonQueryException exception) {
            if (exception.getFailure()
                    == OverallRankingComparisonQueryFailure.NOT_FOUND) {
                return GetWorldStatisticsHistoryResult.notCollected(
                        canonicalWorld,
                        historyRequest.presetCode(),
                        historyRequest.from(),
                        historyRequest.to()
                );
            }

            throw dataInvalid(exception);
        }

        List<OverallRankingWorldStatisticsSnapshot> snapshots;
        StatisticsHistoryRequest.HistoryPeriod historyPeriod;

        try {
            if (historyRequest.isAll()) {
                snapshots = overallRankingWorldStatisticsHistoryQuery
                        .getWorldStatisticsAllHistory();

                historyPeriod = availablePeriod(snapshots);
            } else {
                historyPeriod = historyRequest.resolve(
                        latestComparison.latest().asOf()
                );

                snapshots = overallRankingWorldStatisticsHistoryQuery
                        .getWorldStatisticsHistory(
                                historyPeriod.from(),
                                historyPeriod.to()
                        );
            }
        } catch (OverallRankingWorldStatisticsHistoryQueryException exception) {
            throw dataInvalid(exception);
        }

        Map<String, CanonicalWorld> canonicalWorldsByWorldName =
                worldCatalogQuery.resolveAliases(
                        collectWorldNames(snapshots)
                );

        return GetWorldStatisticsHistoryResult.available(
                canonicalWorld,
                historyRequest.presetCode(),
                historyPeriod.from(),
                historyPeriod.to(),
                snapshots,
                canonicalWorldsByWorldName
        );
    }

    /**
     * 전체 기간의 요청 범위는 실제 보존된 범위와 같다.
     *
     * 요청한 기간이 따로 없으므로 첫·마지막 성공 기준일을 그대로 쓴다. 그래야
     * 응답의 누락 일수가 "보존 범위 안의 구멍"을 뜻하게 된다.
     */
    private StatisticsHistoryRequest.HistoryPeriod availablePeriod(
            List<OverallRankingWorldStatisticsSnapshot> snapshots
    ) {
        if (snapshots.isEmpty()) {
            return new StatisticsHistoryRequest.HistoryPeriod(null, null);
        }

        return new StatisticsHistoryRequest.HistoryPeriod(
                snapshots.get(0).asOf(),
                snapshots.get(snapshots.size() - 1).asOf()
        );
    }

    private CanonicalWorld loadCanonicalWorld(String worldSlug) {
        return worldCatalogQuery.findBySlug(worldSlug)
                .orElseThrow(() -> new WorldStatisticsException(
                        WorldStatisticsFailure.WORLD_NOT_FOUND
                ));
    }

    private Set<String> collectWorldNames(
            OverallRankingWorldStatisticsComparisonSnapshot comparison
    ) {
        Set<String> worldNames = new LinkedHashSet<>();

        addWorldNames(worldNames, comparison.latest());
        addWorldNames(worldNames, comparison.previous());

        return worldNames;
    }

    /**
     * History 전체의 원본 월드 이름을 한 번에 모은다.
     *
     * Point마다 Alias를 해석하면 날짜 수만큼 조회가 늘어나므로 DISTINCT 이름을 모아
     * 한 번만 해석한다.
     */
    private Set<String> collectWorldNames(
            List<OverallRankingWorldStatisticsSnapshot> snapshots
    ) {
        Set<String> worldNames = new LinkedHashSet<>();

        snapshots.forEach(snapshot -> addWorldNames(worldNames, snapshot));

        return worldNames;
    }

    private void addWorldNames(
            Set<String> worldNames,
            OverallRankingWorldStatisticsSnapshot snapshot
    ) {
        if (snapshot == null) {
            return;
        }

        snapshot.worldCounts()
                .forEach(worldCount -> worldNames.add(worldCount.worldName()));
    }

    private WorldStatisticsException dataInvalid(Throwable cause) {
        return new WorldStatisticsException(
                WorldStatisticsFailure.DATA_INVALID,
                cause
        );
    }
}
