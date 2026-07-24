package com.maplemetric.internal.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.internal.infrastructure.properties.OverallRankingCollectionProperties;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotOutcome;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotUseCase;
import com.maplemetric.ranking.api.OverallRankingCollectionAlreadyRunningException;
import com.maplemetric.ranking.api.OverallRankingCollectionStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OverallRankingCollectionController.class)
class OverallRankingCollectionControllerTest {

    private static final LocalDate RANKING_DATE =
            LocalDate.of(2026, 7, 24);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CollectOverallRankingSnapshotUseCase
            collectOverallRankingSnapshotUseCase;

    @MockitoBean
    private OverallRankingCollectionProperties properties;

    @Test
    void 수집성공시200과COLLECTED를반환한다() throws Exception {
        given(collectOverallRankingSnapshotUseCase.collect(any()))
                .willReturn(new CollectOverallRankingSnapshotOutcome(
                        OverallRankingCollectionStatus.COLLECTED,
                        RANKING_DATE,
                        10,
                        2000,
                        true
                ));

        mockMvc.perform(
                        post("/internal/v1/collections/rankings/overall")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"rankingDate":"2026-07-24","maxPages":10}
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.code")
                                .value("OVERALL_RANKING_COLLECTION_SUCCESS")
                )
                .andExpect(
                        jsonPath("$.data.status").value("COLLECTED")
                )
                .andExpect(
                        jsonPath("$.data.pageCount").value(10)
                )
                .andExpect(
                        jsonPath("$.data.sampleSize").value(2000)
                )
                .andExpect(
                        jsonPath("$.data.truncated").value(true)
                );
    }

    @Test
    void 중복Skip시200과SKIPPED를반환하며수치는null이다() throws Exception {
        given(collectOverallRankingSnapshotUseCase.collect(any()))
                .willReturn(new CollectOverallRankingSnapshotOutcome(
                        OverallRankingCollectionStatus.SKIPPED,
                        RANKING_DATE,
                        null,
                        null,
                        null
                ));

        mockMvc.perform(
                        post("/internal/v1/collections/rankings/overall")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.code")
                                .value("OVERALL_RANKING_COLLECTION_SKIPPED")
                )
                .andExpect(
                        jsonPath("$.data.status").value("SKIPPED")
                )
                .andExpect(
                        jsonPath("$.data.pageCount").doesNotExist()
                )
                .andExpect(
                        jsonPath("$.data.sampleSize").doesNotExist()
                )
                .andExpect(
                        jsonPath("$.data.truncated").doesNotExist()
                );
    }

    @Test
    void 이미실행중이면409를반환한다() throws Exception {
        given(collectOverallRankingSnapshotUseCase.collect(any()))
                .willThrow(new OverallRankingCollectionAlreadyRunningException());

        mockMvc.perform(
                        post("/internal/v1/collections/rankings/overall")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.code").value("INTERNAL_002")
                );
    }

    @Test
    void maxPages가0이면400을반환한다() throws Exception {
        mockMvc.perform(
                        post("/internal/v1/collections/rankings/overall")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"maxPages\":0}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void maxPages가101이면400을반환한다() throws Exception {
        mockMvc.perform(
                        post("/internal/v1/collections/rankings/overall")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"maxPages\":101}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
