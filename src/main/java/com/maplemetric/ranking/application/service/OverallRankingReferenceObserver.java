package com.maplemetric.ranking.application.service;

import com.maplemetric.ranking.application.event.OverallRankingSnapshotStoredEvent;
import com.maplemetric.ranking.application.event.OverallRankingSnapshotStoredEvent.ObservedName;
import com.maplemetric.world.api.WorldAliasMatchingQuery;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class OverallRankingReferenceObserver {

    private static final String UNMATCHED_METRIC_NAME =
            "ranking.reference.mapping.unmatched";

    private final JobAliasMatchingService jobAliasMatchingService;
    private final WorldAliasMatchingQuery worldAliasMatchingQuery;
    private final Counter jobUnmatchedCounter;
    private final Counter worldUnmatchedCounter;

    public OverallRankingReferenceObserver(
            JobAliasMatchingService jobAliasMatchingService,
            WorldAliasMatchingQuery worldAliasMatchingQuery,
            MeterRegistry meterRegistry
    ) {
        this.jobAliasMatchingService = jobAliasMatchingService;
        this.worldAliasMatchingQuery = worldAliasMatchingQuery;
        this.jobUnmatchedCounter = createUnmatchedCounter(
                meterRegistry,
                ReferenceType.JOB
        );
        this.worldUnmatchedCounter = createUnmatchedCounter(
                meterRegistry,
                ReferenceType.WORLD
        );
    }

    @Transactional(
            readOnly = true,
            propagation = Propagation.REQUIRES_NEW
    )
    public void observe(OverallRankingSnapshotStoredEvent event) {
        observeUnmatched(
                event,
                ReferenceType.JOB,
                event.jobNames(),
                jobAliasMatchingService::matches
        );
        observeUnmatched(
                event,
                ReferenceType.WORLD,
                event.worldNames(),
                worldAliasMatchingQuery::matches
        );
    }

    private void observeUnmatched(
            OverallRankingSnapshotStoredEvent event,
            ReferenceType referenceType,
            List<ObservedName> observedNames,
            Predicate<String> matcher
    ) {
        aggregateByNormalizedName(observedNames)
                .values()
                .forEach(observedName -> {
                    if (matcher.test(observedName.normalizedName())) {
                        return;
                    }

                    log.warn(
                            "종합 랭킹 기준정보 매핑에 실패했습니다. "
                                    + "기준일={}, 유형={}, 대표 원본 이름={}, "
                                    + "정규화 이름={}, 행 수={}",
                            event.snapshotDate(),
                            referenceType,
                            observedName.representativeName(),
                            observedName.normalizedName(),
                            observedName.rowCount()
                    );
                    unmatchedCounter(referenceType)
                            .increment(observedName.rowCount());
                });
    }

    private Counter createUnmatchedCounter(
            MeterRegistry meterRegistry,
            ReferenceType referenceType
    ) {
        return Counter.builder(UNMATCHED_METRIC_NAME)
                .description(
                        "Overall Ranking Alias에 매칭되지 않은 Snapshot 행 수"
                )
                .baseUnit("rows")
                .tag("type", referenceType.metricTag())
                .register(meterRegistry);
    }

    private Counter unmatchedCounter(ReferenceType referenceType) {
        return switch (referenceType) {
            case JOB -> jobUnmatchedCounter;
            case WORLD -> worldUnmatchedCounter;
        };
    }

    private Map<String, AggregatedObservedName> aggregateByNormalizedName(
            List<ObservedName> observedNames
    ) {
        Map<String, AggregatedObservedName> aggregated =
                new LinkedHashMap<>();

        for (ObservedName observedName : observedNames) {
            String normalizedName = normalize(observedName.name());

            aggregated.compute(
                    normalizedName,
                    (key, current) -> {
                        if (current == null) {
                            return new AggregatedObservedName(
                                    observedName.name(),
                                    normalizedName,
                                    observedName.rowCount()
                            );
                        }

                        return current.add(observedName.rowCount());
                    }
            );
        }

        return aggregated;
    }

    private String normalize(String name) {
        if (name == null) {
            return "";
        }

        return name.trim().toLowerCase(Locale.ROOT);
    }

    private enum ReferenceType {
        JOB("job"),
        WORLD("world");

        private final String metricTag;

        ReferenceType(String metricTag) {
            this.metricTag = metricTag;
        }

        private String metricTag() {
            return metricTag;
        }
    }

    private record AggregatedObservedName(
            String representativeName,
            String normalizedName,
            long rowCount
    ) {

        private AggregatedObservedName add(long additionalRowCount) {
            return new AggregatedObservedName(
                    representativeName,
                    normalizedName,
                    rowCount + additionalRowCount
            );
        }
    }
}
