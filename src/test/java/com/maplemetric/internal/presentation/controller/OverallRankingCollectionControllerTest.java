package com.maplemetric.internal.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maplemetric.internal.application.properties.OverallRankingCollectionProperties;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotOutcome;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotRequest;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotUseCase;
import com.maplemetric.ranking.api.OverallRankingCollectionAlreadyRunningException;
import com.maplemetric.ranking.api.OverallRankingCollectionException;
import com.maplemetric.ranking.api.OverallRankingCollectionFailure;
import com.maplemetric.ranking.api.OverallRankingCollectionStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OverallRankingCollectionController.class)
class OverallRankingCollectionControllerTest {

    private static final String COLLECTION_PATH =
            "/internal/v1/collections/rankings/overall";

    private static final LocalDate RANKING_DATE =
            LocalDate.of(2026, 7, 24);

    private static final int CONFIGURED_MAX_PAGES = 10;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollectOverallRankingSnapshotUseCase
            collectOverallRankingSnapshotUseCase;

    @MockitoBean
    private OverallRankingCollectionProperties properties;

    @Test
    void 수집성공시200과COLLECTED응답전체필드를반환한다() throws Exception {
        given(collectOverallRankingSnapshotUseCase.collect(any()))
                .willReturn(new CollectOverallRankingSnapshotOutcome(
                        OverallRankingCollectionStatus.COLLECTED,
                        RANKING_DATE,
                        10,
                        2000,
                        true
                ));

        mockMvc.perform(
                        post(COLLECTION_PATH)
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
                        jsonPath("$.data.asOf").value("2026-07-24")
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
    void 명시한rankingDate와maxPages를그대로UseCase에전달한다() throws Exception {
        given(collectOverallRankingSnapshotUseCase.collect(any()))
                .willReturn(collectedOutcome());

        mockMvc.perform(
                        post(COLLECTION_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"rankingDate":"2026-07-24","maxPages":3}
                                        """)
                )
                .andExpect(status().isOk());

        verify(collectOverallRankingSnapshotUseCase).collect(
                new CollectOverallRankingSnapshotRequest(RANKING_DATE, 3)
        );
    }

    @Test
    void Body가빈객체이면설정된maxPages기본값을전달한다() throws Exception {
        given(properties.maxPages()).willReturn(CONFIGURED_MAX_PAGES);
        given(collectOverallRankingSnapshotUseCase.collect(any()))
                .willReturn(collectedOutcome());

        mockMvc.perform(
                        post(COLLECTION_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isOk());

        verify(collectOverallRankingSnapshotUseCase).collect(
                new CollectOverallRankingSnapshotRequest(
                        null,
                        CONFIGURED_MAX_PAGES
                )
        );
    }

    @Test
    void Body가없으면설정된maxPages기본값을전달한다() throws Exception {
        given(properties.maxPages()).willReturn(CONFIGURED_MAX_PAGES);
        given(collectOverallRankingSnapshotUseCase.collect(any()))
                .willReturn(collectedOutcome());

        mockMvc.perform(
                        post(COLLECTION_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        verify(collectOverallRankingSnapshotUseCase).collect(
                new CollectOverallRankingSnapshotRequest(
                        null,
                        CONFIGURED_MAX_PAGES
                )
        );
    }

    @Test
    void 중복Skip시200과SKIPPED를반환하며수치는명시적null이다() throws Exception {
        given(properties.maxPages()).willReturn(CONFIGURED_MAX_PAGES);
        given(collectOverallRankingSnapshotUseCase.collect(any()))
                .willReturn(new CollectOverallRankingSnapshotOutcome(
                        OverallRankingCollectionStatus.SKIPPED,
                        RANKING_DATE,
                        null,
                        null,
                        null
                ));

        mockMvc.perform(
                        post(COLLECTION_PATH)
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
                        jsonPath("$.data.asOf").value("2026-07-24")
                )
                .andExpect(jsonPath("$.data.pageCount").hasJsonPath())
                .andExpect(jsonPath("$.data.sampleSize").hasJsonPath())
                .andExpect(jsonPath("$.data.truncated").hasJsonPath())
                .andExpect(
                        jsonPath("$.data.pageCount").value(nullValue())
                )
                .andExpect(
                        jsonPath("$.data.sampleSize").value(nullValue())
                )
                .andExpect(
                        jsonPath("$.data.truncated").value(nullValue())
                );
    }

    /**
     * 본문 JSON이 깨진 요청이다.
     *
     * 서버 오류로 돌려주면 보낸 쪽이 자기 요청을 의심하지 않는다.
     */
    @Test
    void 본문JSON이깨지면400과계약형식으로응답한다() throws Exception {
        mockMvc.perform(
                        post(COLLECTION_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"rankingDate\":")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GLOBAL_001"));
    }

    /**
     * 예상하지 못한 예외도 계약 형식을 지켜야 한다.
     *
     * 처리하지 않으면 Spring 기본 바디가 나가 {@code success} 필드조차 없다.
     * 그리고 예외 메시지에는 제약 이름·SQL이 섞여 들어오므로 응답에 싣지 않는다.
     */
    @Test
    void 예상못한예외는500과계약형식으로응답하고내부정보를숨긴다() throws Exception {
        given(properties.maxPages()).willReturn(CONFIGURED_MAX_PAGES);
        given(collectOverallRankingSnapshotUseCase.collect(any()))
                .willThrow(new IllegalStateException(
                        "uk_p_overall_ranking_snapshot_collection_rank 제약 위반"
                ));

        String body = mockMvc.perform(
                        post(COLLECTION_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GLOBAL_002"))
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(body)
                .doesNotContain("uk_p_overall_ranking_snapshot")
                .doesNotContain("IllegalStateException");
    }

    @Test
    void 이미실행중이면409를반환한다() throws Exception {
        given(properties.maxPages()).willReturn(CONFIGURED_MAX_PAGES);
        given(collectOverallRankingSnapshotUseCase.collect(any()))
                .willThrow(new OverallRankingCollectionAlreadyRunningException());

        mockMvc.perform(
                        post(COLLECTION_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.code").value("INTERNAL_002")
                );
    }

    @ParameterizedTest
    @CsvSource({
            "EXTERNAL_API_CLIENT_ERROR, 502, INTERNAL_003",
            "EXTERNAL_API_SERVER_ERROR, 502, INTERNAL_004",
            "EXTERNAL_API_TIMEOUT, 504, INTERNAL_005",
            "EXTERNAL_API_RESPONSE_INVALID, 502, INTERNAL_006"
    })
    void Nexon수집실패는표준ApiResponse오류계약으로반환한다(
            OverallRankingCollectionFailure failure,
            int expectedStatus,
            String expectedCode
    ) throws Exception {
        given(properties.maxPages()).willReturn(CONFIGURED_MAX_PAGES);
        given(collectOverallRankingSnapshotUseCase.collect(any()))
                .willThrow(new OverallRankingCollectionException(failure));

        mockMvc.perform(
                        post(COLLECTION_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @ParameterizedTest
    @CsvSource({"0", "101"})
    void maxPages가범위를벗어나면400을반환하고UseCase를호출하지않는다(
            int maxPages
    ) throws Exception {
        mockMvc.perform(
                        post(COLLECTION_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"maxPages\":" + maxPages + "}"
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(collectOverallRankingSnapshotUseCase, never())
                .collect(any());
    }

    private CollectOverallRankingSnapshotOutcome collectedOutcome() {
        return new CollectOverallRankingSnapshotOutcome(
                OverallRankingCollectionStatus.COLLECTED,
                RANKING_DATE,
                1,
                1,
                false
        );
    }
}
