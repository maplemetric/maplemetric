package com.maplemetric.internal.presentation.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maplemetric.internal.application.service.OverallRankingRetentionDisabledException;
import com.maplemetric.internal.application.service.OverallRankingRetentionRunner;
import com.maplemetric.ranking.api.OverallRankingRetentionPlan;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OverallRankingRetentionController.class)
class OverallRankingRetentionControllerTest {

    private static final String RETENTION_PATH =
            "/internal/v1/retentions/rankings/overall";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OverallRankingRetentionRunner retentionRunner;

    @Test
    void 산정은대상기준일과행수를반환한다() throws Exception {
        given(retentionRunner.plan()).willReturn(
                new OverallRankingRetentionPlan(
                        List.of(
                                LocalDate.of(2026, 7, 1),
                                LocalDate.of(2026, 7, 2)
                        ),
                        2L,
                        400L,
                        LocalDate.of(2026, 7, 25)
                )
        );

        mockMvc.perform(get(RETENTION_PATH + "/plan"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value("OVERALL_RANKING_RETENTION_PLANNED")
                )
                .andExpect(jsonPath("$.data.snapshotDates.length()").value(2))
                .andExpect(jsonPath("$.data.collectionCount").value(2))
                .andExpect(jsonPath("$.data.snapshotCount").value(400))
                .andExpect(
                        jsonPath("$.data.retainedLatestDate")
                                .value("2026-07-25")
                );
    }

    @Test
    void 만료가꺼져있으면409로거부한다() throws Exception {
        willThrow(new OverallRankingRetentionDisabledException())
                .given(retentionRunner).expire();

        mockMvc.perform(post(RETENTION_PATH + "/expirations"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INTERNAL_008"));
    }

    @Test
    void 만료실행은삭제한대상을반환한다() throws Exception {
        given(retentionRunner.expire()).willReturn(
                new OverallRankingRetentionPlan(
                        List.of(LocalDate.of(2026, 7, 1)),
                        1L,
                        200L,
                        LocalDate.of(2026, 7, 25)
                )
        );

        mockMvc.perform(post(RETENTION_PATH + "/expirations"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value("OVERALL_RANKING_RETENTION_EXPIRED")
                )
                .andExpect(jsonPath("$.data.collectionCount").value(1))
                .andExpect(jsonPath("$.data.snapshotCount").value(200));
    }
}
