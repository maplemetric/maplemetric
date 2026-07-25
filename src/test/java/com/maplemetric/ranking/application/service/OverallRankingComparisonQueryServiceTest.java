package com.maplemetric.ranking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;

import com.maplemetric.ranking.api.OverallRankingComparisonQueryException;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryFailure;
import com.maplemetric.ranking.api.OverallRankingStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsComparisonSnapshot;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.ClassNameAggregate;
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
class OverallRankingComparisonQueryServiceTest {

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
    void 최신Collection이없으면NOT_FOUND예외를던진다() {
        OverallRankingComparisonQueryService service = createService();

        given(loadOverallRankingStatisticsPort
                .loadLatestAllConditionCollection())
                .willReturn(Optional.empty());

        OverallRankingComparisonQueryException exception =
                catchThrowableOfType(
                        service::getJobStatisticsComparison,
                        OverallRankingComparisonQueryException.class
                );

        assertThat(exception.getFailure())
                .isEqualTo(OverallRankingComparisonQueryFailure.NOT_FOUND);
    }

    @Test
    void 이전Collection이없으면previous와daysBetween이없다() {
        OverallRankingComparisonQueryService service = createService();

        givenLatestCollection(2);
        givenNoPreviousCollection();

        given(loadOverallRankingStatisticsPort
                .aggregateByClassName(LATEST_COLLECTION_ID))
                .willReturn(List.of(
                        classNameAggregate("히어로", 2L, 200)
                ));

        OverallRankingStatisticsComparisonSnapshot comparison =
                service.getJobStatisticsComparison();

        assertThat(comparison.latest().asOf())
                .isEqualTo(LATEST_SNAPSHOT_DATE);
        assertThat(comparison.previous()).isNull();
        assertThat(comparison.daysBetween()).isNull();
    }

    @Test
    void 최신과이전이모두있으면기준일간격을계산한다() {
        OverallRankingComparisonQueryService service = createService();

        givenLatestCollection(2);
        givenPreviousCollection(2);

        given(loadOverallRankingStatisticsPort
                .aggregateByClassName(LATEST_COLLECTION_ID))
                .willReturn(List.of(
                        classNameAggregate("히어로", 2L, 200)
                ));

        given(loadOverallRankingStatisticsPort
                .aggregateByClassName(PREVIOUS_COLLECTION_ID))
                .willReturn(List.of(
                        classNameAggregate("히어로", 2L, 190)
                ));

        OverallRankingStatisticsComparisonSnapshot comparison =
                service.getJobStatisticsComparison();

        assertThat(comparison.latest().asOf())
                .isEqualTo(LATEST_SNAPSHOT_DATE);
        assertThat(comparison.previous().asOf())
                .isEqualTo(PREVIOUS_SNAPSHOT_DATE);
        assertThat(comparison.daysBetween()).isEqualTo(3);
    }

    @Test
    void 최신과이전집계를각각의collectionId로조회한다() {
        OverallRankingComparisonQueryService service = createService();

        givenLatestCollection(1);
        givenPreviousCollection(1);

        given(loadOverallRankingStatisticsPort
                .aggregateByClassName(LATEST_COLLECTION_ID))
                .willReturn(List.of(
                        classNameAggregate("히어로", 1L, 200)
                ));

        given(loadOverallRankingStatisticsPort
                .aggregateByClassName(PREVIOUS_COLLECTION_ID))
                .willReturn(List.of(
                        classNameAggregate("팬텀", 1L, 190)
                ));

        OverallRankingStatisticsComparisonSnapshot comparison =
                service.getJobStatisticsComparison();

        assertThat(comparison.latest().jobCounts())
                .extracting(jobCount -> jobCount.className())
                .containsExactly("히어로");
        assertThat(comparison.previous().jobCounts())
                .extracting(jobCount -> jobCount.className())
                .containsExactly("팬텀");
    }

    @Test
    void 최신직업집계합계가sampleSize와다르면DATA_INVALID예외를던진다() {
        OverallRankingComparisonQueryService service = createService();

        givenLatestCollection(5);
        givenPreviousCollection(1);

        given(loadOverallRankingStatisticsPort
                .aggregateByClassName(LATEST_COLLECTION_ID))
                .willReturn(List.of(
                        classNameAggregate("히어로", 2L, 200)
                ));

        OverallRankingComparisonQueryException exception =
                catchThrowableOfType(
                        service::getJobStatisticsComparison,
                        OverallRankingComparisonQueryException.class
                );

        assertThat(exception.getFailure())
                .isEqualTo(OverallRankingComparisonQueryFailure.DATA_INVALID);
    }

    @Test
    void 이전직업집계합계가sampleSize와다르면DATA_INVALID예외를던진다() {
        OverallRankingComparisonQueryService service = createService();

        givenLatestCollection(1);
        givenPreviousCollection(5);

        given(loadOverallRankingStatisticsPort
                .aggregateByClassName(LATEST_COLLECTION_ID))
                .willReturn(List.of(
                        classNameAggregate("히어로", 1L, 200)
                ));

        given(loadOverallRankingStatisticsPort
                .aggregateByClassName(PREVIOUS_COLLECTION_ID))
                .willReturn(List.of(
                        classNameAggregate("히어로", 2L, 190)
                ));

        OverallRankingComparisonQueryException exception =
                catchThrowableOfType(
                        service::getJobStatisticsComparison,
                        OverallRankingComparisonQueryException.class
                );

        assertThat(exception.getFailure())
                .isEqualTo(OverallRankingComparisonQueryFailure.DATA_INVALID);
    }

    @Test
    void 최신월드집계합계가sampleSize와다르면DATA_INVALID예외를던진다() {
        OverallRankingComparisonQueryService service = createService();

        givenLatestCollection(5);
        givenPreviousCollection(1);

        given(loadOverallRankingStatisticsPort
                .aggregateByWorldName(LATEST_COLLECTION_ID))
                .willReturn(List.of(
                        worldNameAggregate("루나", 2L, 200)
                ));

        OverallRankingComparisonQueryException exception =
                catchThrowableOfType(
                        service::getWorldStatisticsComparison,
                        OverallRankingComparisonQueryException.class
                );

        assertThat(exception.getFailure())
                .isEqualTo(OverallRankingComparisonQueryFailure.DATA_INVALID);
    }

    @Test
    void 이전월드집계합계가sampleSize와다르면DATA_INVALID예외를던진다() {
        OverallRankingComparisonQueryService service = createService();

        givenLatestCollection(1);
        givenPreviousCollection(5);

        given(loadOverallRankingStatisticsPort
                .aggregateByWorldName(LATEST_COLLECTION_ID))
                .willReturn(List.of(
                        worldNameAggregate("루나", 1L, 200)
                ));

        given(loadOverallRankingStatisticsPort
                .aggregateByWorldName(PREVIOUS_COLLECTION_ID))
                .willReturn(List.of(
                        worldNameAggregate("루나", 2L, 190)
                ));

        OverallRankingComparisonQueryException exception =
                catchThrowableOfType(
                        service::getWorldStatisticsComparison,
                        OverallRankingComparisonQueryException.class
                );

        assertThat(exception.getFailure())
                .isEqualTo(OverallRankingComparisonQueryFailure.DATA_INVALID);
    }

    @Test
    void 월드비교도기준일간격과집계를반환한다() {
        OverallRankingComparisonQueryService service = createService();

        givenLatestCollection(2);
        givenPreviousCollection(2);

        given(loadOverallRankingStatisticsPort
                .aggregateByWorldName(LATEST_COLLECTION_ID))
                .willReturn(List.of(
                        worldNameAggregate("루나", 2L, 200)
                ));

        given(loadOverallRankingStatisticsPort
                .aggregateByWorldName(PREVIOUS_COLLECTION_ID))
                .willReturn(List.of(
                        worldNameAggregate("베라", 2L, 190)
                ));

        OverallRankingWorldStatisticsComparisonSnapshot comparison =
                service.getWorldStatisticsComparison();

        assertThat(comparison.daysBetween()).isEqualTo(3);
        assertThat(comparison.latest().worldCounts())
                .extracting(worldCount -> worldCount.worldName())
                .containsExactly("루나");
        assertThat(comparison.previous().worldCounts())
                .extracting(worldCount -> worldCount.worldName())
                .containsExactly("베라");
    }

    @Test
    void sampleSize가0이고집계가비어있으면정상결과를반환한다() {
        OverallRankingComparisonQueryService service = createService();

        givenLatestCollection(0);
        givenPreviousCollection(0);

        given(loadOverallRankingStatisticsPort
                .aggregateByClassName(LATEST_COLLECTION_ID))
                .willReturn(List.of());

        given(loadOverallRankingStatisticsPort
                .aggregateByClassName(PREVIOUS_COLLECTION_ID))
                .willReturn(List.of());

        OverallRankingStatisticsComparisonSnapshot comparison =
                service.getJobStatisticsComparison();

        assertThat(comparison.latest().jobCounts()).isEmpty();
        assertThat(comparison.previous().jobCounts()).isEmpty();
        assertThat(comparison.daysBetween()).isEqualTo(3);
    }

    @Test
    void 이전기준일이최신기준일이후이면DATA_INVALID예외를던진다() {
        OverallRankingComparisonQueryService service = createService();

        givenLatestCollection(1);

        given(loadOverallRankingStatisticsPort
                .loadPreviousAllConditionCollection(LATEST_SNAPSHOT_DATE))
                .willReturn(Optional.of(new LatestCollection(
                        PREVIOUS_COLLECTION_ID,
                        LATEST_SNAPSHOT_DATE,
                        "NEXON_OPEN_API",
                        COLLECTED_AT,
                        1,
                        1,
                        100,
                        false
                )));

        OverallRankingComparisonQueryException exception =
                catchThrowableOfType(
                        service::getJobStatisticsComparison,
                        OverallRankingComparisonQueryException.class
                );

        assertThat(exception.getFailure())
                .isEqualTo(OverallRankingComparisonQueryFailure.DATA_INVALID);
    }

    @Test
    void 랭킹통계조회Port에만의존하고외부API를호출하지않는다() {
        assertThat(
                OverallRankingComparisonQueryService.class
                        .getDeclaredConstructors()
        ).hasSize(1);

        assertThat(
                OverallRankingComparisonQueryService.class
                        .getDeclaredConstructors()[0]
                        .getParameterTypes()
        ).containsExactly(LoadOverallRankingStatisticsPort.class);
    }

    private OverallRankingComparisonQueryService createService() {
        return new OverallRankingComparisonQueryService(
                loadOverallRankingStatisticsPort
        );
    }

    private void givenLatestCollection(int sampleSize) {
        given(loadOverallRankingStatisticsPort
                .loadLatestAllConditionCollection())
                .willReturn(Optional.of(createCollection(
                        LATEST_COLLECTION_ID,
                        LATEST_SNAPSHOT_DATE,
                        sampleSize
                )));
    }

    private void givenPreviousCollection(int sampleSize) {
        given(loadOverallRankingStatisticsPort
                .loadPreviousAllConditionCollection(LATEST_SNAPSHOT_DATE))
                .willReturn(Optional.of(createCollection(
                        PREVIOUS_COLLECTION_ID,
                        PREVIOUS_SNAPSHOT_DATE,
                        sampleSize
                )));
    }

    private void givenNoPreviousCollection() {
        given(loadOverallRankingStatisticsPort
                .loadPreviousAllConditionCollection(LATEST_SNAPSHOT_DATE))
                .willReturn(Optional.empty());
    }

    private LatestCollection createCollection(
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

    private ClassNameAggregate classNameAggregate(
            String className,
            long count,
            int averageLevel
    ) {
        return new ClassNameAggregate(
                className,
                count,
                BigDecimal.valueOf(averageLevel)
        );
    }

    private WorldNameAggregate worldNameAggregate(
            String worldName,
            long count,
            int averageLevel
    ) {
        return new WorldNameAggregate(
                worldName,
                count,
                BigDecimal.valueOf(averageLevel)
        );
    }
}
