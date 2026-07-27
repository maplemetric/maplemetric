package com.maplemetric.ranking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsTrendQueryException;
import com.maplemetric.ranking.api.OverallRankingStatisticsTrendQueryFailure;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.ClassNameAggregateByCollection;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.LatestCollection;
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
class OverallRankingStatisticsTrendQueryServiceTest {

    private static final UUID LATEST_COLLECTION_ID = UUID.randomUUID();
    private static final UUID PREVIOUS_COLLECTION_ID = UUID.randomUUID();

    private static final LocalDate LATEST_SNAPSHOT_DATE =
            LocalDate.of(2026, 7, 24);

    private static final LocalDate PREVIOUS_SNAPSHOT_DATE =
            LocalDate.of(2026, 7, 21);

    private static final Instant COLLECTED_AT =
            Instant.parse("2026-07-24T01:00:00Z");

    @Mock
    private LoadOverallRankingStatisticsPort loadOverallRankingStatisticsPort;

    @Test
    void 전체조건Collection이없으면NOT_FOUND예외를던진다() {
        OverallRankingStatisticsTrendQueryService service = createService();

        given(loadOverallRankingStatisticsPort
                .loadLatestAllConditionCollection())
                .willReturn(Optional.empty());

        OverallRankingStatisticsTrendQueryException exception =
                catchThrowableOfType(
                        () -> service.getJobStatisticsTrend(7),
                        OverallRankingStatisticsTrendQueryException.class
                );

        assertThat(exception.getFailure())
                .isEqualTo(OverallRankingStatisticsTrendQueryFailure.NOT_FOUND);
    }

    @Test
    void 포인트를asOf오름차순으로정렬한다() {
        OverallRankingStatisticsTrendQueryService service = createService();

        givenLatestCollection();
        givenCollectionsWithin(
                collection(LATEST_COLLECTION_ID, LATEST_SNAPSHOT_DATE, 1),
                collection(PREVIOUS_COLLECTION_ID, PREVIOUS_SNAPSHOT_DATE, 1)
        );

        given(loadOverallRankingStatisticsPort.aggregateByClassName(
                List.of(LATEST_COLLECTION_ID, PREVIOUS_COLLECTION_ID)
        )).willReturn(List.of(
                classNameAggregate(LATEST_COLLECTION_ID, "히어로", 1L, 200),
                classNameAggregate(PREVIOUS_COLLECTION_ID, "히어로", 1L, 190)
        ));

        List<OverallRankingStatisticsSnapshot> trend =
                service.getJobStatisticsTrend(7);

        assertThat(trend)
                .extracting(snapshot -> snapshot.asOf())
                .containsExactly(PREVIOUS_SNAPSHOT_DATE, LATEST_SNAPSHOT_DATE);
    }

    @Test
    void 각포인트가자신의collectionId집계만포함한다() {
        OverallRankingStatisticsTrendQueryService service = createService();

        givenLatestCollection();
        givenCollectionsWithin(
                collection(LATEST_COLLECTION_ID, LATEST_SNAPSHOT_DATE, 1),
                collection(PREVIOUS_COLLECTION_ID, PREVIOUS_SNAPSHOT_DATE, 1)
        );

        given(loadOverallRankingStatisticsPort.aggregateByClassName(
                List.of(LATEST_COLLECTION_ID, PREVIOUS_COLLECTION_ID)
        )).willReturn(List.of(
                classNameAggregate(LATEST_COLLECTION_ID, "히어로", 1L, 200),
                classNameAggregate(PREVIOUS_COLLECTION_ID, "팬텀", 1L, 190)
        ));

        List<OverallRankingStatisticsSnapshot> trend =
                service.getJobStatisticsTrend(7);

        OverallRankingStatisticsSnapshot latestPoint = trend.stream()
                .filter(snapshot -> snapshot.asOf().equals(LATEST_SNAPSHOT_DATE))
                .findFirst()
                .orElseThrow();

        OverallRankingStatisticsSnapshot previousPoint = trend.stream()
                .filter(snapshot -> snapshot.asOf().equals(PREVIOUS_SNAPSHOT_DATE))
                .findFirst()
                .orElseThrow();

        assertThat(latestPoint.jobCounts())
                .extracting(jobCount -> jobCount.className())
                .containsExactly("히어로");
        assertThat(previousPoint.jobCounts())
                .extracting(jobCount -> jobCount.className())
                .containsExactly("팬텀");
    }

    @Test
    void 최신Collection1건만있으면포인트1건을반환한다() {
        OverallRankingStatisticsTrendQueryService service = createService();

        givenLatestCollection();
        givenCollectionsWithin(
                collection(LATEST_COLLECTION_ID, LATEST_SNAPSHOT_DATE, 1)
        );

        given(loadOverallRankingStatisticsPort.aggregateByClassName(
                List.of(LATEST_COLLECTION_ID)
        )).willReturn(List.of(
                classNameAggregate(LATEST_COLLECTION_ID, "히어로", 1L, 200)
        ));

        List<OverallRankingStatisticsSnapshot> trend =
                service.getJobStatisticsTrend(7);

        assertThat(trend).hasSize(1);
        assertThat(trend.get(0).asOf()).isEqualTo(LATEST_SNAPSHOT_DATE);
    }

    @Test
    void 어느포인트든집계합계가sampleSize와다르면DATA_INVALID예외를던진다() {
        OverallRankingStatisticsTrendQueryService service = createService();

        givenLatestCollection();
        givenCollectionsWithin(
                collection(LATEST_COLLECTION_ID, LATEST_SNAPSHOT_DATE, 5)
        );

        given(loadOverallRankingStatisticsPort.aggregateByClassName(
                List.of(LATEST_COLLECTION_ID)
        )).willReturn(List.of(
                classNameAggregate(LATEST_COLLECTION_ID, "히어로", 2L, 200)
        ));

        OverallRankingStatisticsTrendQueryException exception =
                catchThrowableOfType(
                        () -> service.getJobStatisticsTrend(7),
                        OverallRankingStatisticsTrendQueryException.class
                );

        assertThat(exception.getFailure())
                .isEqualTo(OverallRankingStatisticsTrendQueryFailure.DATA_INVALID);
    }

    @Test
    void sampleSize가0이고집계가비어있으면정상결과다() {
        OverallRankingStatisticsTrendQueryService service = createService();

        givenLatestCollection();
        givenCollectionsWithin(
                collection(LATEST_COLLECTION_ID, LATEST_SNAPSHOT_DATE, 0)
        );

        given(loadOverallRankingStatisticsPort.aggregateByClassName(
                List.of(LATEST_COLLECTION_ID)
        )).willReturn(List.of());

        List<OverallRankingStatisticsSnapshot> trend =
                service.getJobStatisticsTrend(7);

        assertThat(trend).hasSize(1);
        assertThat(trend.get(0).jobCounts()).isEmpty();
    }

    @Test
    void days가1미만이면거부한다() {
        OverallRankingStatisticsTrendQueryService service = createService();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.getJobStatisticsTrend(0));
    }

    @Test
    void days가31을초과하면거부한다() {
        OverallRankingStatisticsTrendQueryService service = createService();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.getJobStatisticsTrend(32));
    }

    @Test
    void 집계조회를포인트수만큼반복호출하지않는다() {
        OverallRankingStatisticsTrendQueryService service = createService();

        givenLatestCollection();
        givenCollectionsWithin(
                collection(LATEST_COLLECTION_ID, LATEST_SNAPSHOT_DATE, 1),
                collection(PREVIOUS_COLLECTION_ID, PREVIOUS_SNAPSHOT_DATE, 1)
        );

        given(loadOverallRankingStatisticsPort.aggregateByClassName(
                List.of(LATEST_COLLECTION_ID, PREVIOUS_COLLECTION_ID)
        )).willReturn(List.of(
                classNameAggregate(LATEST_COLLECTION_ID, "히어로", 1L, 200),
                classNameAggregate(PREVIOUS_COLLECTION_ID, "히어로", 1L, 190)
        ));

        service.getJobStatisticsTrend(7);

        verify(loadOverallRankingStatisticsPort, times(1))
                .aggregateByClassName(List.of(
                        LATEST_COLLECTION_ID,
                        PREVIOUS_COLLECTION_ID
                ));
        verify(loadOverallRankingStatisticsPort, never())
                .aggregateByClassName(LATEST_COLLECTION_ID);
    }

    @Test
    void 랭킹통계조회Port에만의존하고외부API를호출하지않는다() {
        assertThat(
                OverallRankingStatisticsTrendQueryService.class
                        .getDeclaredConstructors()
        ).hasSize(1);

        assertThat(
                OverallRankingStatisticsTrendQueryService.class
                        .getDeclaredConstructors()[0]
                        .getParameterTypes()
        ).containsExactly(LoadOverallRankingStatisticsPort.class);
    }

    private OverallRankingStatisticsTrendQueryService createService() {
        return new OverallRankingStatisticsTrendQueryService(
                loadOverallRankingStatisticsPort
        );
    }

    private void givenLatestCollection() {
        given(loadOverallRankingStatisticsPort
                .loadLatestAllConditionCollection())
                .willReturn(Optional.of(
                        collection(LATEST_COLLECTION_ID, LATEST_SNAPSHOT_DATE, 1)
                ));
    }

    private void givenCollectionsWithin(LatestCollection... collections) {
        given(loadOverallRankingStatisticsPort
                .loadAllConditionCollectionsWithin(
                        LATEST_SNAPSHOT_DATE,
                        7
                ))
                .willReturn(List.of(collections));
    }

    private LatestCollection collection(
            UUID collectionId,
            LocalDate snapshotDate,
            int sampleSize
    ) {
        return new LatestCollection(
                collectionId,
                snapshotDate,
                "NEXON_OPEN_API",
                COLLECTED_AT,
                sampleSize,
                1,
                100,
                false
        );
    }

    private ClassNameAggregateByCollection classNameAggregate(
            UUID collectionId,
            String className,
            long count,
            int averageLevel
    ) {
        return new ClassNameAggregateByCollection(
                collectionId,
                className,
                count,
                BigDecimal.valueOf(averageLevel)
        );
    }
}
