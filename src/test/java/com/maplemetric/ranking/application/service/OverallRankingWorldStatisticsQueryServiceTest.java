package com.maplemetric.ranking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;

import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQueryException;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQueryFailure;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.LatestCollection;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.WorldNameAggregate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OverallRankingWorldStatisticsQueryServiceTest {

    private static final UUID COLLECTION_ID = UUID.randomUUID();

    private static final LocalDate SNAPSHOT_DATE =
            LocalDate.of(2026, 7, 24);

    private static final Instant COLLECTED_AT =
            Instant.parse("2026-07-24T01:00:00Z");

    @Mock
    private LoadOverallRankingStatisticsPort loadOverallRankingStatisticsPort;

    @Test
    void 최신Collection과집계로Snapshot을생성한다() {
        OverallRankingWorldStatisticsQueryService service =
                new OverallRankingWorldStatisticsQueryService(
                        loadOverallRankingStatisticsPort
                );

        given(loadOverallRankingStatisticsPort
                .loadLatestAllConditionCollection())
                .willReturn(Optional.of(createCollection(2)));

        given(loadOverallRankingStatisticsPort
                .aggregateByWorldName(COLLECTION_ID))
                .willReturn(List.of(
                        new WorldNameAggregate(
                                "루나",
                                1L,
                                BigDecimal.valueOf(200)
                        ),
                        new WorldNameAggregate(
                                "베라",
                                1L,
                                BigDecimal.valueOf(210)
                        )
                ));

        OverallRankingWorldStatisticsSnapshot snapshot =
                service.getLatestWorldStatistics();

        assertThat(snapshot.asOf()).isEqualTo(SNAPSHOT_DATE);
        assertThat(snapshot.sampleSize()).isEqualTo(2);
        assertThat(snapshot.worldCounts())
                .extracting(worldCount -> worldCount.worldName())
                .containsExactlyInAnyOrder("루나", "베라");
    }

    @Test
    void 전체조건Collection이없으면NOT_FOUND예외를던진다() {
        OverallRankingWorldStatisticsQueryService service =
                new OverallRankingWorldStatisticsQueryService(
                        loadOverallRankingStatisticsPort
                );

        given(loadOverallRankingStatisticsPort
                .loadLatestAllConditionCollection())
                .willReturn(Optional.empty());

        OverallRankingWorldStatisticsQueryException exception =
                catchThrowableOfType(
                        service::getLatestWorldStatistics,
                        OverallRankingWorldStatisticsQueryException.class
                );

        assertThat(exception.getFailure())
                .isEqualTo(OverallRankingWorldStatisticsQueryFailure.NOT_FOUND);
    }

    @Test
    void 집계count합계와sampleSize가다르면DATA_INVALID예외를던진다() {
        OverallRankingWorldStatisticsQueryService service =
                new OverallRankingWorldStatisticsQueryService(
                        loadOverallRankingStatisticsPort
                );

        given(loadOverallRankingStatisticsPort
                .loadLatestAllConditionCollection())
                .willReturn(Optional.of(createCollection(5)));

        given(loadOverallRankingStatisticsPort
                .aggregateByWorldName(COLLECTION_ID))
                .willReturn(List.of(
                        new WorldNameAggregate(
                                "루나",
                                2L,
                                BigDecimal.valueOf(200)
                        )
                ));

        OverallRankingWorldStatisticsQueryException exception =
                catchThrowableOfType(
                        service::getLatestWorldStatistics,
                        OverallRankingWorldStatisticsQueryException.class
                );

        assertThat(exception.getFailure())
                .isEqualTo(OverallRankingWorldStatisticsQueryFailure.DATA_INVALID);
    }

    @Test
    void sampleSize가0이면빈worldCounts를반환한다() {
        OverallRankingWorldStatisticsQueryService service =
                new OverallRankingWorldStatisticsQueryService(
                        loadOverallRankingStatisticsPort
                );

        given(loadOverallRankingStatisticsPort
                .loadLatestAllConditionCollection())
                .willReturn(Optional.of(createCollection(0)));

        given(loadOverallRankingStatisticsPort
                .aggregateByWorldName(COLLECTION_ID))
                .willReturn(List.of());

        OverallRankingWorldStatisticsSnapshot snapshot =
                service.getLatestWorldStatistics();

        assertThat(snapshot.sampleSize()).isEqualTo(0);
        assertThat(snapshot.worldCounts()).isEmpty();
    }

    private LatestCollection createCollection(int sampleSize) {
        return new LatestCollection(
                COLLECTION_ID,
                SNAPSHOT_DATE,
                "NEXON_OPEN_API",
                COLLECTED_AT,
                sampleSize,
                1,
                100,
                false
        );
    }
}
