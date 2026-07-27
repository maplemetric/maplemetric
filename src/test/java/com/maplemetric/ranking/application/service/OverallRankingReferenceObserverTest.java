package com.maplemetric.ranking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.ranking.application.event.OverallRankingSnapshotStoredEvent;
import com.maplemetric.ranking.application.event.OverallRankingSnapshotStoredEvent.ObservedName;
import com.maplemetric.world.api.WorldAliasMatchingQuery;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class OverallRankingReferenceObserverTest {

    private static final String UNMATCHED_METRIC_NAME =
            "ranking.reference.mapping.unmatched";
    private static final LocalDate SNAPSHOT_DATE =
            LocalDate.of(2026, 7, 27);

    private final SimpleMeterRegistry meterRegistry =
            new SimpleMeterRegistry();

    @Mock
    private JobAliasMatchingService jobAliasMatchingService;

    @Mock
    private WorldAliasMatchingQuery worldAliasMatchingQuery;

    @Test
    void 정규화이름별한번만조회하고행수를합산해미매칭WARN을남긴다(
            CapturedOutput output
    ) {
        OverallRankingReferenceObserver observer = createObserver();
        OverallRankingSnapshotStoredEvent event =
                new OverallRankingSnapshotStoredEvent(
                        SNAPSHOT_DATE,
                        List.of(
                                new ObservedName("  HERO  ", 2L),
                                new ObservedName("hero", 3L),
                                new ObservedName("팬텀", 1L)
                        ),
                        List.of(
                                new ObservedName("  LUNA  ", 4L),
                                new ObservedName("luna", 2L)
                        )
                );

        given(jobAliasMatchingService.matches("hero"))
                .willReturn(false);
        given(jobAliasMatchingService.matches("팬텀"))
                .willReturn(true);
        given(worldAliasMatchingQuery.matches("luna"))
                .willReturn(false);

        observer.observe(event);

        verify(jobAliasMatchingService, times(1)).matches("hero");
        verify(jobAliasMatchingService, times(1)).matches("팬텀");
        verify(worldAliasMatchingQuery, times(1)).matches("luna");
        verifyNoMoreInteractions(
                jobAliasMatchingService,
                worldAliasMatchingQuery
        );

        List<String> unmatchedWarnLines = unmatchedWarnLines(output);
        assertThat(unmatchedWarnLines).hasSize(2);

        assertThat(unmatchedWarnLines)
                .filteredOn(line -> line.contains("정규화 이름=hero"))
                .singleElement()
                .asString()
                .contains(
                        "기준일=2026-07-27",
                        "유형=JOB",
                        "대표 원본 이름=  HERO  ",
                        "행 수=5"
                );
        assertThat(unmatchedWarnLines)
                .filteredOn(line -> line.contains("정규화 이름=luna"))
                .singleElement()
                .asString()
                .contains(
                        "기준일=2026-07-27",
                        "유형=WORLD",
                        "대표 원본 이름=  LUNA  ",
                        "행 수=6"
                );
        assertThat(output.getOut())
                .doesNotContain("정규화 이름=팬텀");
        assertThat(unmatchedCount("job")).isEqualTo(5.0);
        assertThat(unmatchedCount("world")).isEqualTo(6.0);
        assertLowCardinalityMetric();
    }

    @Test
    void 모든이름이매칭되면미매칭WARN을남기지않는다(
            CapturedOutput output
    ) {
        OverallRankingReferenceObserver observer = createObserver();
        OverallRankingSnapshotStoredEvent event =
                new OverallRankingSnapshotStoredEvent(
                        SNAPSHOT_DATE,
                        List.of(new ObservedName("팬텀", 1L)),
                        List.of(new ObservedName("루나", 1L))
                );

        given(jobAliasMatchingService.matches("팬텀"))
                .willReturn(true);
        given(worldAliasMatchingQuery.matches("루나"))
                .willReturn(true);

        observer.observe(event);

        verify(jobAliasMatchingService, times(1)).matches("팬텀");
        verify(worldAliasMatchingQuery, times(1)).matches("루나");
        verifyNoMoreInteractions(
                jobAliasMatchingService,
                worldAliasMatchingQuery
        );

        assertThat(output.getOut())
                .doesNotContain("종합 랭킹 기준정보 매핑에 실패했습니다.");
        assertThat(unmatchedCount("job")).isZero();
        assertThat(unmatchedCount("world")).isZero();
    }

    @Test
    void null빈문자열공백이름을하나로묶어행수를합산하고WARN을한번만남긴다(
            CapturedOutput output
    ) {
        OverallRankingReferenceObserver observer = createObserver();
        OverallRankingSnapshotStoredEvent event =
                new OverallRankingSnapshotStoredEvent(
                        SNAPSHOT_DATE,
                        List.of(
                                new ObservedName(null, 2L),
                                new ObservedName("", 3L),
                                new ObservedName("   ", 4L)
                        ),
                        List.of()
                );

        observer.observe(event);

        verify(jobAliasMatchingService, times(1)).matches("");
        verifyNoMoreInteractions(
                jobAliasMatchingService,
                worldAliasMatchingQuery
        );

        assertThat(unmatchedWarnLines(output))
                .singleElement()
                .asString()
                .contains(
                        "기준일=2026-07-27",
                        "유형=JOB",
                        "대표 원본 이름=null",
                        "정규화 이름=, 행 수=9"
                );
        assertThat(unmatchedCount("job")).isEqualTo(9.0);
        assertThat(unmatchedCount("world")).isZero();
    }

    private List<String> unmatchedWarnLines(CapturedOutput output) {
        return output.getOut()
                .lines()
                .filter(line -> line.contains(" WARN "))
                .filter(line -> line.contains(
                        "종합 랭킹 기준정보 매핑에 실패했습니다."
                ))
                .toList();
    }

    private double unmatchedCount(String type) {
        return unmatchedCounter(type).count();
    }

    private Counter unmatchedCounter(String type) {
        return meterRegistry.get(UNMATCHED_METRIC_NAME)
                .tag("type", type)
                .counter();
    }

    private void assertLowCardinalityMetric() {
        assertThat(meterRegistry.getMeters())
                .hasSize(2)
                .allSatisfy(meter -> {
                    assertThat(meter.getId().getName())
                            .isEqualTo(UNMATCHED_METRIC_NAME);
                    assertThat(meter.getId().getBaseUnit())
                            .isEqualTo("rows");
                    assertThat(meter.getId().getTags())
                            .singleElement()
                            .satisfies(tag -> {
                                assertThat(tag.getKey())
                                        .isEqualTo("type");
                                assertThat(tag.getValue())
                                        .isIn("job", "world");
                            });
                });
    }

    private OverallRankingReferenceObserver createObserver() {
        return new OverallRankingReferenceObserver(
                jobAliasMatchingService,
                worldAliasMatchingQuery,
                meterRegistry
        );
    }
}
