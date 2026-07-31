package com.maplemetric.statistics.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.ranking.api.CanonicalJob;
import com.maplemetric.ranking.api.JobCatalogQuery;
import com.maplemetric.ranking.api.OverallRankingComparisonQuery;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryException;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryFailure;
import com.maplemetric.ranking.api.OverallRankingStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsHistoryQuery;
import com.maplemetric.ranking.api.OverallRankingStatisticsHistoryQueryException;
import com.maplemetric.ranking.api.OverallRankingStatisticsHistoryQueryFailure;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot.JobCount;
import com.maplemetric.statistics.application.exception.JobStatisticsException;
import com.maplemetric.statistics.application.exception.JobStatisticsFailure;
import com.maplemetric.statistics.application.result.GetJobStatisticsDetailResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsHistoryResult;
import com.maplemetric.statistics.application.result.StatisticsDataAvailability;
import com.maplemetric.statistics.application.result.StatisticsTrend;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class JobStatisticsDetailQueryServiceTest {

    private static final LocalDate LATEST_AS_OF =
            LocalDate.of(2026, 7, 30);

    private static final LocalDate PREVIOUS_AS_OF =
            LocalDate.of(2026, 7, 29);

    private static final Instant COLLECTED_AT =
            Instant.parse("2026-07-30T01:00:00Z");

    private static final CanonicalJob HERO = new CanonicalJob(
            "hero",
            "히어로",
            "모험가",
            "전사",
            true,
            1
    );

    @Mock
    private OverallRankingComparisonQuery overallRankingComparisonQuery;

    @Mock
    private OverallRankingStatisticsHistoryQuery
            overallRankingStatisticsHistoryQuery;

    @Mock
    private JobCatalogQuery jobCatalogQuery;

    private JobStatisticsDetailQueryService service;

    @BeforeEach
    void setUp() {
        service = new JobStatisticsDetailQueryService(
                overallRankingComparisonQuery,
                overallRankingStatisticsHistoryQuery,
                jobCatalogQuery
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void 직업상세는CanonicalAlias를한번해석하고목록계산정책을재사용한다() {
        OverallRankingStatisticsComparisonSnapshot comparison =
                new OverallRankingStatisticsComparisonSnapshot(
                        snapshot(
                                LATEST_AS_OF,
                                100,
                                jobCount("히어로", 10L, "200"),
                                jobCount("Hero", 10L, "220"),
                                jobCount("팬텀", 80L, "210")
                        ),
                        snapshot(
                                PREVIOUS_AS_OF,
                                100,
                                jobCount("히어로", 10L, "190"),
                                jobCount("팬텀", 90L, "200")
                        ),
                        1
                );

        given(jobCatalogQuery.findBySlug("hero"))
                .willReturn(Optional.of(HERO));
        given(overallRankingComparisonQuery
                .getJobStatisticsComparison())
                .willReturn(comparison);
        given(jobCatalogQuery.resolveAliases(any()))
                .willReturn(Map.of(
                        "히어로", HERO,
                        "Hero", HERO
                ));

        GetJobStatisticsDetailResult result =
                service.getJobStatisticsDetail("hero");

        assertThat(result.dataAvailability())
                .isEqualTo(StatisticsDataAvailability.AVAILABLE);
        assertThat(result.job().jobSlug()).isEqualTo("hero");
        assertThat(result.job().jobGroup()).isEqualTo("모험가");
        assertThat(result.latest().count()).isEqualTo(20L);
        assertThat(result.latest().percentage())
                .isEqualTo(new BigDecimal("20.00"));
        assertThat(result.latest().averageLevel())
                .isEqualTo(new BigDecimal("210.0"));
        assertThat(result.comparison().previousAsOf())
                .isEqualTo(PREVIOUS_AS_OF);
        assertThat(result.comparison().previousCount()).isEqualTo(10L);
        assertThat(result.comparison().countChange()).isEqualTo(10L);
        assertThat(result.comparison().countChangeRate())
                .isEqualTo(new BigDecimal("100.00"));
        assertThat(result.comparison().percentageChangeRate())
                .isEqualTo(new BigDecimal("100.00"));
        assertThat(result.comparison().percentagePointChange())
                .isEqualTo(new BigDecimal("10.00"));
        assertThat(result.comparison().trend())
                .isEqualTo(StatisticsTrend.UP);
        assertThat(result.sourceMeta().sampleSize()).isEqualTo(100);

        ArgumentCaptor<Collection<String>> classNamesCaptor =
                ArgumentCaptor.forClass(Collection.class);

        verify(jobCatalogQuery).findBySlug("hero");
        verify(overallRankingComparisonQuery)
                .getJobStatisticsComparison();
        verify(jobCatalogQuery)
                .resolveAliases(classNamesCaptor.capture());
        assertThat(classNamesCaptor.getValue())
                .containsExactlyInAnyOrder("히어로", "Hero", "팬텀");
        verifyNoInteractions(overallRankingStatisticsHistoryQuery);
        verifyNoMoreInteractions(
                jobCatalogQuery,
                overallRankingComparisonQuery
        );
    }

    @Test
    void 정상직업에Snapshot이없으면상세는NOT_COLLECTED다() {
        given(jobCatalogQuery.findBySlug("hero"))
                .willReturn(Optional.of(HERO));
        given(overallRankingComparisonQuery
                .getJobStatisticsComparison())
                .willThrow(new OverallRankingComparisonQueryException(
                        OverallRankingComparisonQueryFailure.NOT_FOUND
                ));

        GetJobStatisticsDetailResult result =
                service.getJobStatisticsDetail("hero");

        assertThat(result.dataAvailability())
                .isEqualTo(StatisticsDataAvailability.NOT_COLLECTED);
        assertThat(result.latest()).isNull();
        assertThat(result.comparison()).isNull();
        assertThat(result.sourceMeta()).isNull();
        verify(jobCatalogQuery).findBySlug("hero");
        verify(overallRankingComparisonQuery)
                .getJobStatisticsComparison();
        verifyNoMoreInteractions(
                jobCatalogQuery,
                overallRankingComparisonQuery
        );
        verifyNoInteractions(overallRankingStatisticsHistoryQuery);
    }

    @Test
    void 알수없는직업Slug는404실패로변환한다() {
        given(jobCatalogQuery.findBySlug("unknown"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getJobStatisticsDetail("unknown"))
                .isInstanceOf(JobStatisticsException.class)
                .extracting(exception ->
                        ((JobStatisticsException) exception).getFailure())
                .isEqualTo(JobStatisticsFailure.JOB_NOT_FOUND);

        verify(jobCatalogQuery).findBySlug("unknown");
        verifyNoMoreInteractions(jobCatalogQuery);
        verifyNoInteractions(
                overallRankingComparisonQuery,
                overallRankingStatisticsHistoryQuery
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void 기본7DHistory는최신성공기준일에Anchor하고누락일을보간하지않는다() {
        givenAvailableJobAndLatestSnapshot();

        OverallRankingStatisticsSnapshot first = snapshot(
                LocalDate.of(2026, 7, 24),
                100,
                jobCount("히어로", 10L, "200"),
                jobCount("팬텀", 90L, "210")
        );
        OverallRankingStatisticsSnapshot last = snapshot(
                LATEST_AS_OF,
                200,
                jobCount("Hero", 40L, "220"),
                jobCount("팬텀", 160L, "215")
        );

        given(overallRankingStatisticsHistoryQuery
                .getJobStatisticsHistory(
                        LocalDate.of(2026, 7, 24),
                        LATEST_AS_OF
                ))
                .willReturn(List.of(first, last));
        given(jobCatalogQuery.resolveAliases(any()))
                .willReturn(Map.of(
                        "히어로", HERO,
                        "Hero", HERO
                ));

        GetJobStatisticsHistoryResult result =
                service.getJobStatisticsHistory(
                        "hero",
                        null,
                        null,
                        null
                );

        assertThat(result.dataAvailability())
                .isEqualTo(StatisticsDataAvailability.AVAILABLE);
        assertThat(result.range().preset()).isEqualTo("7D");
        assertThat(result.range().requestedFrom())
                .isEqualTo(LocalDate.of(2026, 7, 24));
        assertThat(result.range().requestedTo()).isEqualTo(LATEST_AS_OF);
        assertThat(result.range().pointCount()).isEqualTo(2);
        assertThat(result.range().missingDateCount()).isEqualTo(5);
        assertThat(result.points())
                .extracting(point -> point.asOf())
                .containsExactly(LocalDate.of(2026, 7, 24), LATEST_AS_OF);
        assertThat(result.points())
                .extracting(point -> point.percentage())
                .containsExactly(
                        new BigDecimal("10.00"),
                        new BigDecimal("20.00")
                );
        assertThat(result.rangeComparison().previousCount()).isEqualTo(10L);
        assertThat(result.rangeComparison().currentCount()).isEqualTo(40L);
        assertThat(result.rangeComparison().countChangeRate())
                .isEqualTo(new BigDecimal("300.00"));
        assertThat(result.rangeComparison().percentageChangeRate())
                .isEqualTo(new BigDecimal("100.00"));
        assertThat(result.rangeComparison().percentagePointChange())
                .isEqualTo(new BigDecimal("10.00"));

        ArgumentCaptor<Collection<String>> classNamesCaptor =
                ArgumentCaptor.forClass(Collection.class);

        verify(jobCatalogQuery)
                .resolveAliases(classNamesCaptor.capture());
        assertThat(classNamesCaptor.getValue())
                .containsExactlyInAnyOrder("히어로", "Hero", "팬텀");
        verifyNoMoreInteractions(
                jobCatalogQuery,
                overallRankingComparisonQuery,
                overallRankingStatisticsHistoryQuery
        );
    }

    static Stream<Arguments> presetRanges() {
        return Stream.of(
                Arguments.of("7D", LocalDate.of(2026, 7, 24)),
                Arguments.of("30D", LocalDate.of(2026, 7, 1)),
                Arguments.of("90D", LocalDate.of(2026, 5, 2)),
                Arguments.of("1Y", LocalDate.of(2025, 7, 31))
        );
    }

    @ParameterizedTest
    @MethodSource("presetRanges")
    void Preset은최신성공기준일을포함한달력일범위다(
            String preset,
            LocalDate expectedFrom
    ) {
        givenAvailableJobAndLatestSnapshot();
        given(overallRankingStatisticsHistoryQuery
                .getJobStatisticsHistory(expectedFrom, LATEST_AS_OF))
                .willReturn(List.of());
        given(jobCatalogQuery.resolveAliases(any()))
                .willReturn(Map.of());

        GetJobStatisticsHistoryResult result =
                service.getJobStatisticsHistory(
                        "hero",
                        preset,
                        null,
                        null
                );

        assertThat(result.range().preset()).isEqualTo(preset);
        assertThat(result.range().requestedFrom()).isEqualTo(expectedFrom);
        assertThat(result.range().requestedTo()).isEqualTo(LATEST_AS_OF);
        assertThat(result.points()).isEmpty();
        assertThat(result.rangeComparison()).isNull();
        verify(overallRankingStatisticsHistoryQuery)
                .getJobStatisticsHistory(expectedFrom, LATEST_AS_OF);
    }

    static Stream<Arguments> invalidHistoryRequests() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        return Stream.of(
                Arguments.of("ALL", null, null),
                Arguments.of("", null, null),
                Arguments.of(
                        "7D",
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 7)
                ),
                Arguments.of(null, LocalDate.of(2026, 7, 1), null),
                Arguments.of(null, null, LocalDate.of(2026, 7, 7)),
                Arguments.of(
                        null,
                        LocalDate.of(2026, 7, 8),
                        LocalDate.of(2026, 7, 7)
                ),
                Arguments.of(
                        null,
                        LocalDate.of(2025, 7, 30),
                        LocalDate.of(2026, 7, 30)
                ),
                Arguments.of(null, today, today.plusDays(1))
        );
    }

    @ParameterizedTest
    @MethodSource("invalidHistoryRequests")
    void 잘못된History요청은조회전에거부한다(
            String range,
            LocalDate from,
            LocalDate to
    ) {
        assertThatThrownBy(() -> service.getJobStatisticsHistory(
                "hero",
                range,
                from,
                to
        ))
                .isInstanceOf(JobStatisticsException.class)
                .extracting(exception ->
                        ((JobStatisticsException) exception).getFailure())
                .isEqualTo(
                        JobStatisticsFailure.INVALID_HISTORY_REQUEST
                );

        verifyNoInteractions(
                jobCatalogQuery,
                overallRankingComparisonQuery,
                overallRankingStatisticsHistoryQuery
        );
    }

    @Test
    void 정상직업에Snapshot이없으면기본PresetHistory는날짜없는NOT_COLLECTED다() {
        given(jobCatalogQuery.findBySlug("hero"))
                .willReturn(Optional.of(HERO));
        given(overallRankingComparisonQuery
                .getJobStatisticsComparison())
                .willThrow(new OverallRankingComparisonQueryException(
                        OverallRankingComparisonQueryFailure.NOT_FOUND
                ));

        GetJobStatisticsHistoryResult result =
                service.getJobStatisticsHistory(
                        "hero",
                        null,
                        null,
                        null
                );

        assertThat(result.dataAvailability())
                .isEqualTo(StatisticsDataAvailability.NOT_COLLECTED);
        assertThat(result.range().preset()).isEqualTo("7D");
        assertThat(result.range().requestedFrom()).isNull();
        assertThat(result.range().requestedTo()).isNull();
        assertThat(result.range().pointCount()).isZero();
        assertThat(result.range().missingDateCount()).isZero();
        assertThat(result.points()).isEmpty();
        assertThat(result.rangeComparison()).isNull();
        verifyNoInteractions(overallRankingStatisticsHistoryQuery);
    }

    @Test
    void 정상직업에Snapshot이없으면CustomHistory는요청기간을유지한다() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 7);

        given(jobCatalogQuery.findBySlug("hero"))
                .willReturn(Optional.of(HERO));
        given(overallRankingComparisonQuery
                .getJobStatisticsComparison())
                .willThrow(new OverallRankingComparisonQueryException(
                        OverallRankingComparisonQueryFailure.NOT_FOUND
                ));

        GetJobStatisticsHistoryResult result =
                service.getJobStatisticsHistory(
                        "hero",
                        null,
                        from,
                        to
                );

        assertThat(result.dataAvailability())
                .isEqualTo(StatisticsDataAvailability.NOT_COLLECTED);
        assertThat(result.range().preset()).isNull();
        assertThat(result.range().requestedFrom()).isEqualTo(from);
        assertThat(result.range().requestedTo()).isEqualTo(to);
        assertThat(result.range().missingDateCount()).isEqualTo(7);
        assertThat(result.points()).isEmpty();
        verifyNoInteractions(overallRankingStatisticsHistoryQuery);
    }

    @Test
    void 기간밖에만Snapshot이있으면History는AVAILABLE빈Points다() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 7);

        givenAvailableJobAndLatestSnapshot();
        given(overallRankingStatisticsHistoryQuery
                .getJobStatisticsHistory(from, to))
                .willReturn(List.of());
        given(jobCatalogQuery.resolveAliases(any()))
                .willReturn(Map.of());

        GetJobStatisticsHistoryResult result =
                service.getJobStatisticsHistory(
                        "hero",
                        null,
                        from,
                        to
                );

        assertThat(result.dataAvailability())
                .isEqualTo(StatisticsDataAvailability.AVAILABLE);
        assertThat(result.points()).isEmpty();
        assertThat(result.range().missingDateCount()).isEqualTo(7);
        assertThat(result.rangeComparison()).isNull();
        verify(jobCatalogQuery).resolveAliases(any());
    }

    @Test
    void Collection은있지만대상직업이없으면0Point이고비교는없다() {
        LocalDate asOf = LocalDate.of(2026, 7, 7);

        givenAvailableJobAndLatestSnapshot();
        given(overallRankingStatisticsHistoryQuery
                .getJobStatisticsHistory(asOf, asOf))
                .willReturn(List.of(snapshot(
                        asOf,
                        100,
                        jobCount("팬텀", 100L, "210")
                )));
        given(jobCatalogQuery.resolveAliases(any()))
                .willReturn(Map.of());

        GetJobStatisticsHistoryResult result =
                service.getJobStatisticsHistory(
                        "hero",
                        null,
                        asOf,
                        asOf
                );

        assertThat(result.dataAvailability())
                .isEqualTo(StatisticsDataAvailability.AVAILABLE);
        assertThat(result.points()).hasSize(1);
        assertThat(result.points().get(0).count()).isZero();
        assertThat(result.points().get(0).percentage())
                .isEqualTo(new BigDecimal("0.00"));
        assertThat(result.points().get(0).averageLevel()).isNull();
        assertThat(result.range().missingDateCount()).isZero();
        assertThat(result.rangeComparison()).isNull();
    }

    @Test
    void RankingHistoryDATA_INVALID는직업통계실패로변환한다() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 7);

        givenAvailableJobAndLatestSnapshot();
        given(overallRankingStatisticsHistoryQuery
                .getJobStatisticsHistory(from, to))
                .willThrow(
                        new OverallRankingStatisticsHistoryQueryException(
                                OverallRankingStatisticsHistoryQueryFailure
                                        .DATA_INVALID
                        )
                );

        assertThatThrownBy(() -> service.getJobStatisticsHistory(
                "hero",
                null,
                from,
                to
        ))
                .isInstanceOf(JobStatisticsException.class)
                .extracting(exception ->
                        ((JobStatisticsException) exception).getFailure())
                .isEqualTo(JobStatisticsFailure.DATA_INVALID);
    }

    @Test
    void 조회서비스는readOnlyTransaction경계다() {
        Transactional transactional =
                JobStatisticsDetailQueryService.class
                        .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }

    private void givenAvailableJobAndLatestSnapshot() {
        given(jobCatalogQuery.findBySlug("hero"))
                .willReturn(Optional.of(HERO));
        given(overallRankingComparisonQuery
                .getJobStatisticsComparison())
                .willReturn(new OverallRankingStatisticsComparisonSnapshot(
                        snapshot(
                                LATEST_AS_OF,
                                100,
                                jobCount("히어로", 10L, "200"),
                                jobCount("팬텀", 90L, "210")
                        ),
                        null,
                        null
                ));
    }

    private OverallRankingStatisticsSnapshot snapshot(
            LocalDate asOf,
            int sampleSize,
            JobCount... jobCounts
    ) {
        return new OverallRankingStatisticsSnapshot(
                asOf,
                "NEXON_OPEN_API",
                COLLECTED_AT,
                sampleSize,
                13,
                100,
                false,
                List.of(jobCounts)
        );
    }

    private JobCount jobCount(
            String className,
            long count,
            String averageLevel
    ) {
        return new JobCount(
                className,
                count,
                new BigDecimal(averageLevel)
        );
    }
}
