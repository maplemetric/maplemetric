package com.maplemetric.statistics.application.service;

import com.maplemetric.ranking.api.CanonicalJob;
import com.maplemetric.ranking.api.JobCatalogQuery;
import com.maplemetric.ranking.api.OverallRankingComparisonQuery;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryException;
import com.maplemetric.ranking.api.OverallRankingStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot;
import com.maplemetric.statistics.application.exception.JobStatisticsException;
import com.maplemetric.statistics.application.exception.JobStatisticsFailure;
import com.maplemetric.statistics.application.exception.WorldStatisticsException;
import com.maplemetric.statistics.application.exception.WorldStatisticsFailure;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult;
import com.maplemetric.world.api.CanonicalWorld;
import com.maplemetric.world.api.WorldCatalogQuery;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class StatisticsQueryService {

    private final OverallRankingComparisonQuery overallRankingComparisonQuery;
    private final JobCatalogQuery jobCatalogQuery;
    private final WorldCatalogQuery worldCatalogQuery;

    public StatisticsQueryService(
            OverallRankingComparisonQuery overallRankingComparisonQuery,
            JobCatalogQuery jobCatalogQuery,
            WorldCatalogQuery worldCatalogQuery
    ) {
        this.overallRankingComparisonQuery = overallRankingComparisonQuery;
        this.jobCatalogQuery = jobCatalogQuery;
        this.worldCatalogQuery = worldCatalogQuery;
    }

    public GetJobStatisticsResult getJobStatistics() {
        OverallRankingStatisticsComparisonSnapshot comparison =
                loadJobStatisticsComparison();

        Map<String, CanonicalJob> canonicalJobsByClassName =
                jobCatalogQuery.resolveAliases(
                        collectClassNames(comparison)
                );

        return GetJobStatisticsResult.from(
                comparison,
                jobCatalogQuery.findAll(),
                canonicalJobsByClassName
        );
    }

    private Set<String> collectClassNames(
            OverallRankingStatisticsComparisonSnapshot comparison
    ) {
        Set<String> classNames = new LinkedHashSet<>();

        addClassNames(classNames, comparison.latest());
        addClassNames(classNames, comparison.previous());

        return classNames;
    }

    private void addClassNames(
            Set<String> classNames,
            OverallRankingStatisticsSnapshot snapshot
    ) {
        if (snapshot == null) {
            return;
        }

        snapshot.jobCounts()
                .forEach(jobCount -> classNames.add(jobCount.className()));
    }

    private OverallRankingStatisticsComparisonSnapshot loadJobStatisticsComparison() {
        try {
            return overallRankingComparisonQuery.getJobStatisticsComparison();
        } catch (OverallRankingComparisonQueryException exception) {
            throw new JobStatisticsException(
                    toJobStatisticsFailure(exception),
                    exception
            );
        }
    }

    private JobStatisticsFailure toJobStatisticsFailure(
            OverallRankingComparisonQueryException exception
    ) {
        return switch (exception.getFailure()) {
            case NOT_FOUND -> JobStatisticsFailure.SNAPSHOT_NOT_FOUND;
            case DATA_INVALID -> JobStatisticsFailure.DATA_INVALID;
        };
    }

    public GetWorldStatisticsResult getWorldStatistics() {
        OverallRankingWorldStatisticsComparisonSnapshot comparison =
                loadWorldStatisticsComparison();

        Set<String> worldNames = new LinkedHashSet<>();

        addWorldNames(worldNames, comparison.latest());
        addWorldNames(worldNames, comparison.previous());

        Map<String, CanonicalWorld> canonicalWorldsByWorldName =
                worldCatalogQuery.resolveAliases(worldNames);

        return GetWorldStatisticsResult.from(
                comparison,
                worldCatalogQuery.findAll(),
                canonicalWorldsByWorldName
        );
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

    private OverallRankingWorldStatisticsComparisonSnapshot
            loadWorldStatisticsComparison() {
        try {
            return overallRankingComparisonQuery
                    .getWorldStatisticsComparison();
        } catch (OverallRankingComparisonQueryException exception) {
            throw new WorldStatisticsException(
                    toWorldStatisticsFailure(exception),
                    exception
            );
        }
    }

    private WorldStatisticsFailure toWorldStatisticsFailure(
            OverallRankingComparisonQueryException exception
    ) {
        return switch (exception.getFailure()) {
            case NOT_FOUND -> WorldStatisticsFailure.SNAPSHOT_NOT_FOUND;
            case DATA_INVALID -> WorldStatisticsFailure.DATA_INVALID;
        };
    }
}
