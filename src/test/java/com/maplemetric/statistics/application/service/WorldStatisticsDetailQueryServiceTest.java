package com.maplemetric.statistics.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.ranking.api.OverallRankingComparisonQuery;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryException;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryFailure;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsHistoryQuery;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsHistoryQueryException;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsHistoryQueryFailure;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot.WorldCount;
import com.maplemetric.statistics.application.exception.WorldStatisticsException;
import com.maplemetric.statistics.application.exception.WorldStatisticsFailure;
import com.maplemetric.statistics.application.result.GetWorldStatisticsDetailResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsHistoryResult;
import com.maplemetric.statistics.api.StatisticsDataAvailability;
import com.maplemetric.statistics.api.StatisticsTrend;
import com.maplemetric.world.api.CanonicalWorld;
import com.maplemetric.world.api.WorldCatalogQuery;
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
class WorldStatisticsDetailQueryServiceTest {

    private static final LocalDate LATEST_AS_OF =
            LocalDate.of(2026, 7, 30);

    private static final LocalDate PREVIOUS_AS_OF =
            LocalDate.of(2026, 7, 29);

    private static final Instant COLLECTED_AT =
            Instant.parse("2026-07-30T01:00:00Z");

    private static final CanonicalWorld LUNA = new CanonicalWorld(
            "luna",
            "루나",
            CanonicalWorld.Status.ACTIVE,
            1
    );

    @Mock
    private OverallRankingComparisonQuery overallRankingComparisonQuery;

    @Mock
    private OverallRankingWorldStatisticsHistoryQuery
            overallRankingWorldStatisticsHistoryQuery;

    @Mock
    private WorldCatalogQuery worldCatalogQuery;

    private WorldStatisticsDetailQueryService service;

    @BeforeEach
    void setUp() {
        service = new WorldStatisticsDetailQueryService(
                overallRankingComparisonQuery,
                overallRankingWorldStatisticsHistoryQuery,
                worldCatalogQuery
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void 월드상세는CanonicalAlias를한번해석하고Count합산과가중평균을적용한다() {
        OverallRankingWorldStatisticsComparisonSnapshot comparison =
                new OverallRankingWorldStatisticsComparisonSnapshot(
                        snapshot(
                                LATEST_AS_OF,
                                100,
                                worldCount("루나", 10L, "200"),
                                worldCount("Luna", 10L, "220"),
                                worldCount("베라", 80L, "210")
                        ),
                        snapshot(
                                PREVIOUS_AS_OF,
                                100,
                                worldCount("루나", 10L, "190"),
                                worldCount("베라", 90L, "200")
                        ),
                        1
                );

        given(worldCatalogQuery.findBySlug("luna"))
                .willReturn(Optional.of(LUNA));
        given(overallRankingComparisonQuery
                .getWorldStatisticsComparison())
                .willReturn(comparison);
        given(worldCatalogQuery.resolveAliases(any()))
                .willReturn(Map.of(
                        "루나", LUNA,
                        "Luna", LUNA
                ));

        GetWorldStatisticsDetailResult result =
                service.getWorldStatisticsDetail("luna");

        assertThat(result.dataAvailability())
                .isEqualTo(StatisticsDataAvailability.AVAILABLE);
        assertThat(result.world().worldSlug()).isEqualTo("luna");
        assertThat(result.world().status())
                .isEqualTo(CanonicalWorld.Status.ACTIVE);
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

        ArgumentCaptor<Collection<String>> worldNamesCaptor =
                ArgumentCaptor.forClass(Collection.class);

        verify(worldCatalogQuery).findBySlug("luna");
        verify(overallRankingComparisonQuery)
                .getWorldStatisticsComparison();
        verify(worldCatalogQuery)
                .resolveAliases(worldNamesCaptor.capture());
        assertThat(worldNamesCaptor.getValue())
                .containsExactlyInAnyOrder("루나", "Luna", "베라");
        verifyNoInteractions(overallRankingWorldStatisticsHistoryQuery);
        verifyNoMoreInteractions(
                worldCatalogQuery,
                overallRankingComparisonQuery
        );
    }

    @Test
    void 정상월드에Snapshot이없으면상세는NOT_COLLECTED다() {
        given(worldCatalogQuery.findBySlug("luna"))
                .willReturn(Optional.of(LUNA));
        given(overallRankingComparisonQuery
                .getWorldStatisticsComparison())
                .willThrow(new OverallRankingComparisonQueryException(
                        OverallRankingComparisonQueryFailure.NOT_FOUND
                ));

        GetWorldStatisticsDetailResult result =
                service.getWorldStatisticsDetail("luna");

        assertThat(result.dataAvailability())
                .isEqualTo(StatisticsDataAvailability.NOT_COLLECTED);
        assertThat(result.world().worldSlug()).isEqualTo("luna");
        assertThat(result.latest()).isNull();
        assertThat(result.comparison()).isNull();
        assertThat(result.sourceMeta()).isNull();
    }

    @Test
    void 알수없는월드Slug는404실패로변환한다() {
        given(worldCatalogQuery.findBySlug("unknown"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getWorldStatisticsDetail("unknown"))
                .isInstanceOf(WorldStatisticsException.class)
                .extracting(exception ->
                        ((WorldStatisticsException) exception).getFailure())
                .isEqualTo(WorldStatisticsFailure.WORLD_NOT_FOUND);

        verifyNoInteractions(
                overallRankingComparisonQuery,
                overallRankingWorldStatisticsHistoryQuery
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void 기본7DHistory는최신성공기준일에Anchor하고Alias를한번만해석한다() {
        givenAvailableWorldAndLatestSnapshot();

        given(overallRankingWorldStatisticsHistoryQuery
                .getWorldStatisticsHistory(
                        LocalDate.of(2026, 7, 24),
                        LATEST_AS_OF
                ))
                .willReturn(List.of(
                        snapshot(
                                PREVIOUS_AS_OF,
                                100,
                                worldCount("루나", 10L, "190"),
                                worldCount("베라", 90L, "200")
                        ),
                        snapshot(
                                LATEST_AS_OF,
                                100,
                                worldCount("루나", 20L, "210"),
                                worldCount("베라", 80L, "205")
                        )
                ));
        given(worldCatalogQuery.resolveAliases(any()))
                .willReturn(Map.of("루나", LUNA));

        GetWorldStatisticsHistoryResult result =
                service.getWorldStatisticsHistory(
                        "luna",
                        null,
                        null,
                        null
                );

        assertThat(result.dataAvailability())
                .isEqualTo(StatisticsDataAvailability.AVAILABLE);
        assertThat(result.range().preset()).isEqualTo("7D");
        assertThat(result.range().pointCount()).isEqualTo(2);

        // 7일을 요청했고 Point는 2개다. 누락 5일을 0으로 채우지 않는다.
        assertThat(result.range().missingDateCount()).isEqualTo(5);

        assertThat(result.points())
                .extracting(point -> point.asOf(), point -> point.count())
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(
                                PREVIOUS_AS_OF, 10L
                        ),
                        org.assertj.core.api.Assertions.tuple(
                                LATEST_AS_OF, 20L
                        )
                );

        assertThat(result.rangeComparison().percentagePointChange())
                .isEqualTo(new BigDecimal("10.00"));
        assertThat(result.limitations()).isNotEmpty();

        ArgumentCaptor<Collection<String>> worldNamesCaptor =
                ArgumentCaptor.forClass(Collection.class);

        verify(worldCatalogQuery)
                .resolveAliases(worldNamesCaptor.capture());
        assertThat(worldNamesCaptor.getValue())
                .containsExactlyInAnyOrder("루나", "베라");
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
        givenAvailableWorldAndLatestSnapshot();
        given(overallRankingWorldStatisticsHistoryQuery
                .getWorldStatisticsHistory(expectedFrom, LATEST_AS_OF))
                .willReturn(List.of());
        given(worldCatalogQuery.resolveAliases(any()))
                .willReturn(Map.of());

        GetWorldStatisticsHistoryResult result =
                service.getWorldStatisticsHistory(
                        "luna",
                        preset,
                        null,
                        null
                );

        assertThat(result.range().preset()).isEqualTo(preset);
        assertThat(result.range().requestedFrom()).isEqualTo(expectedFrom);
        assertThat(result.range().requestedTo()).isEqualTo(LATEST_AS_OF);
        assertThat(result.points()).isEmpty();
        assertThat(result.rangeComparison()).isNull();
        verify(overallRankingWorldStatisticsHistoryQuery)
                .getWorldStatisticsHistory(expectedFrom, LATEST_AS_OF);
    }

    @Test
    void exactly365InclusiveDaysAreAllowed() {
        // Custom 범위만 today와 비교하므로 고정 날짜를 쓰면 시스템 날짜에 결과가 묶인다.
        LocalDate to = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate from = to.minusDays(364);

        givenAvailableWorldAndLatestSnapshot();
        given(overallRankingWorldStatisticsHistoryQuery
                .getWorldStatisticsHistory(from, to))
                .willReturn(List.of());
        given(worldCatalogQuery.resolveAliases(any()))
                .willReturn(Map.of());

        GetWorldStatisticsHistoryResult result =
                service.getWorldStatisticsHistory("luna", null, from, to);

        assertThat(result.range().requestedFrom()).isEqualTo(from);
        assertThat(result.range().requestedTo()).isEqualTo(to);
        assertThat(result.range().missingDateCount()).isEqualTo(365);
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
        assertThatThrownBy(() -> service.getWorldStatisticsHistory(
                "luna",
                range,
                from,
                to
        ))
                .isInstanceOf(WorldStatisticsException.class)
                .extracting(exception ->
                        ((WorldStatisticsException) exception).getFailure())
                .isEqualTo(
                        WorldStatisticsFailure.INVALID_HISTORY_REQUEST
                );

        verifyNoInteractions(
                worldCatalogQuery,
                overallRankingComparisonQuery,
                overallRankingWorldStatisticsHistoryQuery
        );
    }

    @Test
    void 정상월드에Snapshot이없으면기본PresetHistory는날짜없는NOT_COLLECTED다() {
        given(worldCatalogQuery.findBySlug("luna"))
                .willReturn(Optional.of(LUNA));
        given(overallRankingComparisonQuery
                .getWorldStatisticsComparison())
                .willThrow(new OverallRankingComparisonQueryException(
                        OverallRankingComparisonQueryFailure.NOT_FOUND
                ));

        GetWorldStatisticsHistoryResult result =
                service.getWorldStatisticsHistory(
                        "luna",
                        null,
                        null,
                        null
                );

        assertThat(result.dataAvailability())
                .isEqualTo(StatisticsDataAvailability.NOT_COLLECTED);
        assertThat(result.range().preset()).isEqualTo("7D");
        assertThat(result.range().requestedFrom()).isNull();
        assertThat(result.range().requestedTo()).isNull();
        assertThat(result.range().missingDateCount()).isZero();
        assertThat(result.points()).isEmpty();
        assertThat(result.rangeComparison()).isNull();

        verifyNoInteractions(overallRankingWorldStatisticsHistoryQuery);
    }

    @Test
    void 정상월드에Snapshot이없으면CustomHistory는요청기간을유지한다() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 7);

        given(worldCatalogQuery.findBySlug("luna"))
                .willReturn(Optional.of(LUNA));
        given(overallRankingComparisonQuery
                .getWorldStatisticsComparison())
                .willThrow(new OverallRankingComparisonQueryException(
                        OverallRankingComparisonQueryFailure.NOT_FOUND
                ));

        GetWorldStatisticsHistoryResult result =
                service.getWorldStatisticsHistory("luna", null, from, to);

        assertThat(result.range().preset()).isNull();
        assertThat(result.range().requestedFrom()).isEqualTo(from);
        assertThat(result.range().requestedTo()).isEqualTo(to);
        assertThat(result.range().missingDateCount()).isEqualTo(7);
    }

    @Test
    void Collection은있지만대상월드가없으면0Point이고비교는없다() {
        givenAvailableWorldAndLatestSnapshot();

        given(overallRankingWorldStatisticsHistoryQuery
                .getWorldStatisticsHistory(
                        LocalDate.of(2026, 7, 24),
                        LATEST_AS_OF
                ))
                .willReturn(List.of(
                        snapshot(
                                LATEST_AS_OF,
                                100,
                                worldCount("베라", 100L, "200")
                        )
                ));
        given(worldCatalogQuery.resolveAliases(any()))
                .willReturn(Map.of());

        GetWorldStatisticsHistoryResult result =
                service.getWorldStatisticsHistory(
                        "luna",
                        null,
                        null,
                        null
                );

        assertThat(result.points())
                .singleElement()
                .satisfies(point -> {
                    assertThat(point.count()).isZero();
                    assertThat(point.percentage())
                            .isEqualTo(new BigDecimal("0.00"));
                    assertThat(point.averageLevel()).isNull();
                });

        assertThat(result.rangeComparison()).isNull();
    }

    @Test
    void RankingHistoryDATA_INVALID는월드통계실패로변환한다() {
        givenAvailableWorldAndLatestSnapshot();

        given(overallRankingWorldStatisticsHistoryQuery
                .getWorldStatisticsHistory(
                        LocalDate.of(2026, 7, 24),
                        LATEST_AS_OF
                ))
                .willThrow(
                        new OverallRankingWorldStatisticsHistoryQueryException(
                                OverallRankingWorldStatisticsHistoryQueryFailure
                                        .DATA_INVALID
                        )
                );

        assertThatThrownBy(() -> service.getWorldStatisticsHistory(
                "luna",
                null,
                null,
                null
        ))
                .isInstanceOf(WorldStatisticsException.class)
                .extracting(exception ->
                        ((WorldStatisticsException) exception).getFailure())
                .isEqualTo(WorldStatisticsFailure.DATA_INVALID);
    }

    @Test
    void 조회서비스는readOnlyTransaction경계다() {
        Transactional transactional =
                WorldStatisticsDetailQueryService.class
                        .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }

    private void givenAvailableWorldAndLatestSnapshot() {
        given(worldCatalogQuery.findBySlug("luna"))
                .willReturn(Optional.of(LUNA));
        given(overallRankingComparisonQuery
                .getWorldStatisticsComparison())
                .willReturn(
                        new OverallRankingWorldStatisticsComparisonSnapshot(
                                snapshot(
                                        LATEST_AS_OF,
                                        100,
                                        worldCount("루나", 10L, "200"),
                                        worldCount("베라", 90L, "210")
                                ),
                                null,
                                null
                        )
                );
    }

    private OverallRankingWorldStatisticsSnapshot snapshot(
            LocalDate asOf,
            int sampleSize,
            WorldCount... worldCounts
    ) {
        return new OverallRankingWorldStatisticsSnapshot(
                asOf,
                "NEXON_OPEN_API",
                COLLECTED_AT,
                sampleSize,
                13,
                100,
                false,
                List.of(worldCounts)
        );
    }

    private WorldCount worldCount(
            String worldName,
            long count,
            String averageLevel
    ) {
        return new WorldCount(
                worldName,
                count,
                new BigDecimal(averageLevel)
        );
    }
}
