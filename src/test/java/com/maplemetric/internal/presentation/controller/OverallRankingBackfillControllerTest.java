package com.maplemetric.internal.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillDate;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillErrorType;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillJob;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillStatus;
import com.maplemetric.internal.application.service.OverallRankingBackfillRunner;
import com.maplemetric.internal.application.service.OverallRankingBackfillStateService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OverallRankingBackfillController.class)
class OverallRankingBackfillControllerTest {

    private static final String BACKFILL_PATH =
            "/internal/v1/backfills/rankings/overall";

    private static final UUID JOB_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final LocalDate FROM = LocalDate.of(2026, 7, 1);

    private static final LocalDate TO = LocalDate.of(2026, 7, 3);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OverallRankingBackfillRunner backfillRunner;

    @MockitoBean
    private OverallRankingBackfillStateService backfillStateService;

    @Test
    void Job을생성하고기준일목록까지반환한다() throws Exception {
        given(backfillRunner.createJob(FROM, TO)).willReturn(job());
        givenStoredJob();

        mockMvc.perform(
                        post(BACKFILL_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"from":"2026-07-01","to":"2026-07-03"}
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.code")
                                .value("OVERALL_RANKING_BACKFILL_JOB_CREATED")
                )
                .andExpect(
                        jsonPath("$.data.backfillJobId")
                                .value(JOB_ID.toString())
                )
                .andExpect(jsonPath("$.data.requestedFrom").value("2026-07-01"))
                .andExpect(jsonPath("$.data.requestedTo").value("2026-07-03"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.dates.length()").value(2))
                .andExpect(
                        jsonPath("$.data.dates[1].lastErrorType")
                                .value("EXTERNAL_TIMEOUT")
                );
    }

    @ParameterizedTest
    @CsvSource({
            "'{\"from\":\"2026-07-03\",\"to\":\"2026-07-01\"}'",
            "'{\"from\":null,\"to\":\"2026-07-01\"}'",
            "'{\"from\":\"2026-07-01\"}'",
            "'{\"from\":\"2999-01-01\",\"to\":\"2999-01-02\"}'"
    })
    void 잘못된기간은Job을만들지않는다(String body) throws Exception {
        mockMvc.perform(
                        post(BACKFILL_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest());

        verify(backfillRunner, never()).createJob(any(), any());
    }

    @Test
    void 실행결과로처리한기준일수와갱신된상태를반환한다() throws Exception {
        givenStoredJob();
        given(backfillRunner.run(JOB_ID)).willReturn(2);

        mockMvc.perform(post(BACKFILL_PATH + "/" + JOB_ID + "/runs"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value("OVERALL_RANKING_BACKFILL_RUN_SUCCESS")
                )
                .andExpect(jsonPath("$.data.processedDateCount").value(2))
                .andExpect(
                        jsonPath("$.data.job.backfillJobId")
                                .value(JOB_ID.toString())
                );
    }

    /**
     * 없는 Job을 실행하면 처리한 기준일 0으로 조용히 성공해 잘못된 식별자를 눈치채지
     * 못한다.
     */
    @Test
    void 없는Job실행은404이고실행기를부르지않는다() throws Exception {
        given(backfillStateService.findJob(JOB_ID))
                .willReturn(Optional.empty());

        mockMvc.perform(post(BACKFILL_PATH + "/" + JOB_ID + "/runs"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INTERNAL_007"));

        verify(backfillRunner, never()).run(any());
    }

    @Test
    void 없는Job조회는404다() throws Exception {
        given(backfillStateService.findJob(JOB_ID))
                .willReturn(Optional.empty());

        mockMvc.perform(get(BACKFILL_PATH + "/" + JOB_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INTERNAL_007"));
    }

    @Test
    void 상태조회는집계와기준일을함께반환한다() throws Exception {
        givenStoredJob();

        mockMvc.perform(get(BACKFILL_PATH + "/" + JOB_ID))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value("OVERALL_RANKING_BACKFILL_JOB_FOUND")
                )
                .andExpect(jsonPath("$.data.succeededDateCount").value(1))
                .andExpect(jsonPath("$.data.failedDateCount").value(0))
                .andExpect(jsonPath("$.data.skippedDateCount").value(0))
                .andExpect(
                        jsonPath("$.data.dates[0].snapshotDate")
                                .value("2026-07-01")
                )
                .andExpect(jsonPath("$.data.dates[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.dates[0].attemptCount").value(1));
    }

    private void givenStoredJob() {
        given(backfillStateService.findJob(eq(JOB_ID)))
                .willReturn(Optional.of(job()));
        given(backfillStateService.findDates(eq(JOB_ID)))
                .willReturn(List.of(
                        date(FROM, BackfillStatus.SUCCEEDED, 1, null),
                        date(
                                FROM.plusDays(1),
                                BackfillStatus.PENDING,
                                1,
                                BackfillErrorType.EXTERNAL_TIMEOUT
                        )
                ));
    }

    private BackfillJob job() {
        return new BackfillJob(
                JOB_ID,
                FROM,
                TO,
                BackfillStatus.PENDING,
                1,
                0,
                0,
                Instant.parse("2026-08-01T00:00:00Z"),
                null,
                null
        );
    }

    private BackfillDate date(
            LocalDate snapshotDate,
            BackfillStatus status,
            int attemptCount,
            BackfillErrorType lastErrorType
    ) {
        return new BackfillDate(
                UUID.randomUUID(),
                JOB_ID,
                snapshotDate,
                status,
                attemptCount,
                lastErrorType,
                null,
                null
        );
    }
}
