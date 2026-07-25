package com.maplemetric.internal.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.internal.infrastructure.properties.OverallRankingCollectionProperties;
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
                    .withUserConfiguration(
                            OverallRankingCollectionScheduler.class
                    );

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
                new OverallRankingCollectionScheduler(useCase, properties);

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
                        new OverallRankingCollectionProperties(10)
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
                        new OverallRankingCollectionProperties(10)
                );

        assertThatThrownBy(scheduler::collectOverallRanking)
                .isInstanceOf(OverallRankingCollectionException.class);
    }
}
