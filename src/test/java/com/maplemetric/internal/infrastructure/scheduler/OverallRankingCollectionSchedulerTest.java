package com.maplemetric.internal.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.internal.application.properties.OverallRankingCollectionProperties;
import com.maplemetric.internal.application.service.OverallRankingGapRecoveryRunner;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotOutcome;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotRequest;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotUseCase;
import com.maplemetric.ranking.api.OverallRankingCollectionAlreadyRunningException;
import com.maplemetric.ranking.api.OverallRankingCollectionException;
import com.maplemetric.ranking.api.OverallRankingCollectionFailure;
import com.maplemetric.ranking.api.OverallRankingCollectionStatus;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class OverallRankingCollectionSchedulerTest {

    /** 메우기는 이 테스트의 대상이 아니다. 아무것도 하지 않게 둔다. */
    private final OverallRankingGapRecoveryRunner gapRecoveryRunner =
            mock(OverallRankingGapRecoveryRunner.class);

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withBean(
                            CollectOverallRankingSnapshotUseCase.class,
                            () -> mock(
                                    CollectOverallRankingSnapshotUseCase.class
                            )
                    )
                    .withBean(
                            OverallRankingCollectionProperties.class,
                            () -> new OverallRankingCollectionProperties(10)
                    )
                    .withBean(
                            OverallRankingGapRecoveryRunner.class,
                            () -> gapRecoveryRunner
                    )
                    .withBean(
                            MeterRegistry.class,
                            () -> meterRegistry
                    )
                    .withUserConfiguration(
                            OverallRankingCollectionScheduler.class
                    );

    /**
     * 정기 수집의 결과를 센다.
     *
     * 이 값이 없으면 수집이 며칠째 빠졌는지 사람이 저장소를 뒤져야 알 수 있다.
     */
    @Test
    void 수집에성공하면성공으로센다() {
        CollectOverallRankingSnapshotUseCase useCase =
                mock(CollectOverallRankingSnapshotUseCase.class);

        given(useCase.collect(any()))
                .willReturn(new CollectOverallRankingSnapshotOutcome(
                        OverallRankingCollectionStatus.COLLECTED,
                        null, 1, 1, false
                ));

        scheduler(useCase).collectOverallRanking();

        assertThat(counterValue("succeeded", "none")).isEqualTo(1.0);
    }

    /**
     * 실패는 사유로 갈라 센다.
     *
     * 실패 수만 세면 외부가 잠시 막힌 것과 응답이 계약과 어긋난 것이 같은 숫자로
     * 보인다. 그 둘은 대응이 다르다.
     */
    @Test
    void 실패는사유로갈라센다() {
        CollectOverallRankingSnapshotUseCase useCase =
                mock(CollectOverallRankingSnapshotUseCase.class);

        given(useCase.collect(any()))
                .willThrow(new OverallRankingCollectionException(
                        OverallRankingCollectionFailure
                                .EXTERNAL_API_RATE_LIMITED
                ));

        assertThatThrownBy(
                () -> scheduler(useCase).collectOverallRanking()
        )
                .isInstanceOf(OverallRankingCollectionException.class);

        assertThat(counterValue("failed", "EXTERNAL_API_RATE_LIMITED"))
                .isEqualTo(1.0);
    }

    /** 중복 실행 차단은 실패가 아니다. 정상 동작이므로 따로 센다. */
    @Test
    void 중복실행차단은건너뜀으로센다() {
        CollectOverallRankingSnapshotUseCase useCase =
                mock(CollectOverallRankingSnapshotUseCase.class);

        given(useCase.collect(any()))
                .willThrow(
                        new OverallRankingCollectionAlreadyRunningException()
                );

        scheduler(useCase).collectOverallRanking();

        assertThat(counterValue("skipped", "ALREADY_RUNNING"))
                .isEqualTo(1.0);
    }

    /**
     * 이미 받아 둔 기준일은 건너뜀으로 센다.
     *
     * 이때는 예외가 오르지 않고 건너뛴 결과가 돌아온다. 성공으로 세면 새로 받은 날과
     * 구별되지 않아, 며칠째 같은 자리에 머물러 있어도 매일 성공한 것처럼 보인다.
     */
    @Test
    void 이미받아둔기준일은건너뜀으로센다() {
        CollectOverallRankingSnapshotUseCase useCase =
                mock(CollectOverallRankingSnapshotUseCase.class);

        given(useCase.collect(any()))
                .willReturn(new CollectOverallRankingSnapshotOutcome(
                        OverallRankingCollectionStatus.SKIPPED,
                        null, 0, 0, false
                ));

        scheduler(useCase).collectOverallRanking();

        assertThat(counterValue("skipped", "ALREADY_COLLECTED"))
                .isEqualTo(1.0);
    }

    private OverallRankingCollectionScheduler scheduler(
            CollectOverallRankingSnapshotUseCase useCase
    ) {
        return new OverallRankingCollectionScheduler(
                useCase,
                new OverallRankingCollectionProperties(10),
                gapRecoveryRunner,
                meterRegistry
        );
    }

    private double counterValue(String result, String reason) {
        return meterRegistry.get("ranking.collection.scheduled")
                .tag("result", result)
                .tag("reason", reason)
                .counter()
                .count();
    }

    @Test
    void enabled가false이면Bean이등록되지않는다() {
        contextRunner
                .withPropertyValues(
                        "maplemetric.internal.ranking"
                                + ".overall-ranking-collection"
                                + ".scheduler.enabled=false"
                )
                .run(context -> assertThat(context)
                        .doesNotHaveBean(
                                OverallRankingCollectionScheduler.class
                        ));
    }

    @Test
    void enabled가true이면Bean이등록된다() {
        contextRunner
                .withPropertyValues(
                        "maplemetric.internal.ranking"
                                + ".overall-ranking-collection"
                                + ".scheduler.enabled=true"
                )
                .run(context -> assertThat(context)
                        .hasSingleBean(
                                OverallRankingCollectionScheduler.class
                        ));
    }

    @Test
    void 설정된maxPages와rankingDate없이UseCase를1회호출한다() {
        CollectOverallRankingSnapshotUseCase useCase =
                mock(CollectOverallRankingSnapshotUseCase.class);
        OverallRankingCollectionProperties properties =
                new OverallRankingCollectionProperties(10);

        BDDMockito.given(useCase.collect(
                        new CollectOverallRankingSnapshotRequest(null, 10)
                ))
                .willReturn(new CollectOverallRankingSnapshotOutcome(
                        OverallRankingCollectionStatus.COLLECTED,
                        null, 1, 1, false
                ));

        OverallRankingCollectionScheduler scheduler =
                new OverallRankingCollectionScheduler(
                        useCase,
                        properties,
                        gapRecoveryRunner,
                        meterRegistry
                );

        scheduler.collectOverallRanking();

        verify(useCase).collect(
                eq(new CollectOverallRankingSnapshotRequest(null, 10))
        );
        verifyNoMoreInteractions(useCase);
    }

    @Test
    void 이미실행중이면예외를전파하지않고Skip한다() {
        CollectOverallRankingSnapshotUseCase useCase =
                mock(CollectOverallRankingSnapshotUseCase.class);

        BDDMockito.given(useCase.collect(any()))
                .willThrow(
                        new OverallRankingCollectionAlreadyRunningException()
                );

        OverallRankingCollectionScheduler scheduler =
                new OverallRankingCollectionScheduler(
                        useCase,
                        new OverallRankingCollectionProperties(10),
                        gapRecoveryRunner,
                        meterRegistry
                );

        assertThatCode(scheduler::collectOverallRanking)
                .doesNotThrowAnyException();

        verify(useCase).collect(
                eq(new CollectOverallRankingSnapshotRequest(null, 10))
        );
    }

    @Test
    void 예상하지못한예외는그대로전파한다() {
        CollectOverallRankingSnapshotUseCase useCase =
                mock(CollectOverallRankingSnapshotUseCase.class);

        BDDMockito.given(useCase.collect(any()))
                .willThrow(
                        new OverallRankingCollectionException(
                                OverallRankingCollectionFailure
                                        .EXTERNAL_API_TIMEOUT
                        )
                );

        OverallRankingCollectionScheduler scheduler =
                new OverallRankingCollectionScheduler(
                        useCase,
                        new OverallRankingCollectionProperties(10),
                        gapRecoveryRunner,
                        meterRegistry
                );

        assertThatThrownBy(scheduler::collectOverallRanking)
                .isInstanceOf(OverallRankingCollectionException.class);
    }
}
