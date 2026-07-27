package com.maplemetric.ranking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.maplemetric.ranking.application.event.OverallRankingSnapshotStoredEvent;
import com.maplemetric.ranking.application.event.OverallRankingSnapshotStoredEvent.ObservedName;
import com.maplemetric.world.api.WorldAliasMatchingQuery;
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

    private static final LocalDate SNAPSHOT_DATE =
            LocalDate.of(2026, 7, 27);

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

        assertThat(output.getOut())
                .contains("기준일=2026-07-27")
                .contains("유형=JOB")
                .contains("대표 원본 이름=  HERO  ")
                .contains("정규화 이름=hero")
                .contains("행 수=5")
                .contains("유형=WORLD")
                .contains("대표 원본 이름=  LUNA  ")
                .contains("정규화 이름=luna")
                .contains("행 수=6")
                .containsOnlyOnce("정규화 이름=hero")
                .containsOnlyOnce("정규화 이름=luna")
                .doesNotContain("정규화 이름=팬텀");
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

        assertThat(output.getOut())
                .doesNotContain("종합 랭킹 기준정보 매핑에 실패했습니다.");
    }

    private OverallRankingReferenceObserver createObserver() {
        return new OverallRankingReferenceObserver(
                jobAliasMatchingService,
                worldAliasMatchingQuery
        );
    }
}
