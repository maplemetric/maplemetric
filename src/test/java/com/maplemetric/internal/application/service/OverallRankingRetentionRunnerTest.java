package com.maplemetric.internal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.maplemetric.internal.infrastructure.properties.OverallRankingRetentionProperties;
import com.maplemetric.ranking.api.ExpireOverallRankingSnapshotUseCase;
import com.maplemetric.ranking.api.OverallRankingRetentionPlan;
import com.maplemetric.ranking.api.OverallRankingRetentionRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OverallRankingRetentionRunnerTest {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-01T00:00:00Z"),
            KOREA_ZONE_ID
    );

    private static final int RETENTION_DAYS = 30;

    private static final int MAX_DATES_PER_RUN = 7;

    @Mock
    private ExpireOverallRankingSnapshotUseCase expireUseCase;

    @Test
    void 보존일수만큼과거를만료기준일로삼는다() {
        given(expireUseCase.plan(any()))
                .willReturn(OverallRankingRetentionPlan.empty(null));

        createRunner(false).plan();

        ArgumentCaptor<OverallRankingRetentionRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        OverallRankingRetentionRequest.class
                );

        verify(expireUseCase).plan(requestCaptor.capture());

        assertThat(requestCaptor.getValue())
                .extracting(
                        request -> request.expireBefore(),
                        request -> request.maxDatesPerRun()
                )
                .containsExactly(
                        LocalDate.of(2026, 8, 1).minusDays(RETENTION_DAYS),
                        MAX_DATES_PER_RUN
                );
    }

    @Test
    void 만료가꺼져있으면삭제하지않는다() {
        assertThatThrownBy(() -> createRunner(false).expire())
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(expireUseCase);
    }

    @Test
    void 만료가켜져있으면삭제한다() {
        given(expireUseCase.expire(any()))
                .willReturn(OverallRankingRetentionPlan.empty(null));

        createRunner(true).expire();

        verify(expireUseCase).expire(any());
    }

    /**
     * 산정은 아무것도 바꾸지 않으므로 꺼져 있어도 막지 않는다.
     */
    @Test
    void 만료가꺼져있어도대상산정은가능하다() {
        given(expireUseCase.plan(any()))
                .willReturn(OverallRankingRetentionPlan.empty(null));

        assertThat(createRunner(false).plan()).isNotNull();
    }

    private OverallRankingRetentionRunner createRunner(boolean enabled) {
        return new OverallRankingRetentionRunner(
                expireUseCase,
                new OverallRankingRetentionProperties(
                        enabled,
                        RETENTION_DAYS,
                        MAX_DATES_PER_RUN
                ),
                FIXED_CLOCK
        );
    }
}
