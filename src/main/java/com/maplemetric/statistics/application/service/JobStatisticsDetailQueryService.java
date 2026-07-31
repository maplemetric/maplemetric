package com.maplemetric.statistics.application.service;

import com.maplemetric.ranking.api.CanonicalJob;
import com.maplemetric.ranking.api.JobCatalogQuery;
import com.maplemetric.ranking.api.OverallRankingComparisonQuery;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryException;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryFailure;
import com.maplemetric.ranking.api.OverallRankingStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsHistoryQuery;
import com.maplemetric.ranking.api.OverallRankingStatisticsHistoryQueryException;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.statistics.application.exception.JobStatisticsException;
import com.maplemetric.statistics.application.exception.JobStatisticsFailure;
import com.maplemetric.statistics.application.result.GetJobStatisticsDetailResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsHistoryResult;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JobStatisticsDetailQueryService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private static final int MAX_HISTORY_DAYS = 365;

    private final OverallRankingComparisonQuery overallRankingComparisonQuery;
    private final OverallRankingStatisticsHistoryQuery
            overallRankingStatisticsHistoryQuery;
    private final JobCatalogQuery jobCatalogQuery;

    public JobStatisticsDetailQueryService(
            OverallRankingComparisonQuery overallRankingComparisonQuery,
            OverallRankingStatisticsHistoryQuery
                    overallRankingStatisticsHistoryQuery,
            JobCatalogQuery jobCatalogQuery
    ) {
        this.overallRankingComparisonQuery = overallRankingComparisonQuery;
        this.overallRankingStatisticsHistoryQuery =
                overallRankingStatisticsHistoryQuery;
        this.jobCatalogQuery = jobCatalogQuery;
    }

    public GetJobStatisticsDetailResult getJobStatisticsDetail(
            String jobSlug
    ) {
        CanonicalJob canonicalJob = loadCanonicalJob(jobSlug);

        OverallRankingStatisticsComparisonSnapshot rankingComparison;

        try {
            rankingComparison =
                    overallRankingComparisonQuery
                            .getJobStatisticsComparison();
        } catch (OverallRankingComparisonQueryException exception) {
            if (exception.getFailure()
                    == OverallRankingComparisonQueryFailure.NOT_FOUND) {
                return GetJobStatisticsDetailResult.notCollected(
                        canonicalJob
                );
            }

            throw dataInvalid(exception);
        }

        Map<String, CanonicalJob> canonicalJobsByClassName =
                jobCatalogQuery.resolveAliases(
                        collectClassNames(rankingComparison)
                );

        return GetJobStatisticsDetailResult.available(
                canonicalJob,
                rankingComparison,
                canonicalJobsByClassName
        );
    }

    public GetJobStatisticsHistoryResult getJobStatisticsHistory(
            String jobSlug,
            String range,
            LocalDate from,
            LocalDate to
    ) {
        HistoryRequest historyRequest =
                HistoryRequest.from(
                        range,
                        from,
                        to,
                        LocalDate.now(KOREA_ZONE)
                );

        CanonicalJob canonicalJob = loadCanonicalJob(jobSlug);

        OverallRankingStatisticsComparisonSnapshot latestComparison;

        try {
            latestComparison =
                    overallRankingComparisonQuery
                            .getJobStatisticsComparison();
        } catch (OverallRankingComparisonQueryException exception) {
            if (exception.getFailure()
                    == OverallRankingComparisonQueryFailure.NOT_FOUND) {
                return GetJobStatisticsHistoryResult.notCollected(
                        canonicalJob,
                        historyRequest.presetCode(),
                        historyRequest.from(),
                        historyRequest.to()
                );
            }

            throw dataInvalid(exception);
        }

        HistoryPeriod historyPeriod = historyRequest.resolve(
                latestComparison.latest().asOf()
        );

        List<OverallRankingStatisticsSnapshot> snapshots;

        try {
            snapshots = overallRankingStatisticsHistoryQuery
                    .getJobStatisticsHistory(
                            historyPeriod.from(),
                            historyPeriod.to()
                    );
        } catch (OverallRankingStatisticsHistoryQueryException exception) {
            throw dataInvalid(exception);
        }

        Map<String, CanonicalJob> canonicalJobsByClassName =
                jobCatalogQuery.resolveAliases(
                        collectClassNames(snapshots)
                );

        return GetJobStatisticsHistoryResult.available(
                canonicalJob,
                historyRequest.presetCode(),
                historyPeriod.from(),
                historyPeriod.to(),
                snapshots,
                canonicalJobsByClassName
        );
    }

    private CanonicalJob loadCanonicalJob(String jobSlug) {
        return jobCatalogQuery.findBySlug(jobSlug)
                .orElseThrow(() -> new JobStatisticsException(
                        JobStatisticsFailure.JOB_NOT_FOUND
                ));
    }

    private Set<String> collectClassNames(
            OverallRankingStatisticsComparisonSnapshot comparison
    ) {
        Set<String> classNames = new LinkedHashSet<>();

        addClassNames(classNames, comparison.latest());
        addClassNames(classNames, comparison.previous());

        return classNames;
    }

    private Set<String> collectClassNames(
            List<OverallRankingStatisticsSnapshot> snapshots
    ) {
        Set<String> classNames = new LinkedHashSet<>();

        snapshots.forEach(snapshot -> addClassNames(
                classNames,
                snapshot
        ));

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

    private JobStatisticsException dataInvalid(Throwable cause) {
        return new JobStatisticsException(
                JobStatisticsFailure.DATA_INVALID,
                cause
        );
    }

    private record HistoryRequest(
            HistoryPreset preset,
            LocalDate from,
            LocalDate to
    ) {

        private static HistoryRequest from(
                String range,
                LocalDate from,
                LocalDate to,
                LocalDate today
        ) {
            if (range != null) {
                if (range.isBlank() || from != null || to != null) {
                    throw invalidRequest();
                }

                return new HistoryRequest(
                        HistoryPreset.from(range),
                        null,
                        null
                );
            }

            if (from == null && to == null) {
                return new HistoryRequest(
                        HistoryPreset.SEVEN_DAYS,
                        null,
                        null
                );
            }

            validateCustomRange(from, to, today);

            return new HistoryRequest(null, from, to);
        }

        private static void validateCustomRange(
                LocalDate from,
                LocalDate to,
                LocalDate today
        ) {
            if (from == null || to == null || from.isAfter(to)) {
                throw invalidRequest();
            }

            long inclusiveDays = ChronoUnit.DAYS.between(from, to) + 1;

            if (inclusiveDays > MAX_HISTORY_DAYS || to.isAfter(today)) {
                throw invalidRequest();
            }
        }

        private HistoryPeriod resolve(LocalDate latestAsOf) {
            if (preset == null) {
                return new HistoryPeriod(from, to);
            }

            return new HistoryPeriod(
                    latestAsOf.minusDays(preset.days() - 1L),
                    latestAsOf
            );
        }

        private String presetCode() {
            return preset == null ? null : preset.code();
        }

        private static JobStatisticsException invalidRequest() {
            return new JobStatisticsException(
                    JobStatisticsFailure.INVALID_HISTORY_REQUEST
            );
        }
    }

    private record HistoryPeriod(
            LocalDate from,
            LocalDate to
    ) {
    }

    private enum HistoryPreset {
        SEVEN_DAYS("7D", 7),
        THIRTY_DAYS("30D", 30),
        NINETY_DAYS("90D", 90),
        ONE_YEAR("1Y", 365);

        private final String code;
        private final int days;

        HistoryPreset(String code, int days) {
            this.code = code;
            this.days = days;
        }

        private static HistoryPreset from(String code) {
            for (HistoryPreset preset : values()) {
                if (preset.code.equals(code)) {
                    return preset;
                }
            }

            throw HistoryRequest.invalidRequest();
        }

        private String code() {
            return code;
        }

        private int days() {
            return days;
        }
    }
}
