package com.maplemetric.ranking.presentation.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maplemetric.ranking.application.result.GetDojangRankingResult;
import com.maplemetric.ranking.application.result.GetOverallRankingResult;
import com.maplemetric.ranking.application.result.GetUnionRankingResult;
import com.maplemetric.ranking.application.service.RankingQueryService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RankingController.class)
class RankingControllerTest {

    private static final LocalDate RANKING_DATE =
            LocalDate.of(2026, 7, 19);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RankingQueryService rankingQueryService;

    @Test
    void 종합랭킹조회응답을반환한다() throws Exception {
        given(rankingQueryService.getOverallRanking(
                RANKING_DATE,
                null,
                0,
                "팬텀-전체 전직",
                2
        )).willReturn(createOverallResult());

        mockMvc.perform(
                        get("/api/v1/rankings/overall")
                                .param("date", "2026-07-19")
                                .param("worldType", "0")
                                .param(
                                        "className",
                                        "팬텀-전체 전직"
                                )
                                .param("page", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "OVERALL_RANKING_SEARCH_SUCCESS"
                                )
                )
                .andExpect(
                        jsonPath("$.data.ranking[0].ranking")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.data.ranking[0].characterName")
                                .value("감점")
                )
                .andExpect(
                        jsonPath("$.data.ranking[0].characterExp")
                                .value(1234567)
                )
                .andExpect(
                        jsonPath(
                                "$.data.ranking[0].characterGuildName"
                        ).value("메이플")
                )
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(
                        jsonPath("$.data.asOf")
                                .value("2026-07-19")
                )
                .andExpect(
                        jsonPath("$.data.source")
                                .value("NEXON_OPEN_API")
                )
                .andExpect(
                        jsonPath("$.data.total")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.data.hasNext")
                                .doesNotExist()
                );

        verify(rankingQueryService).getOverallRanking(
                RANKING_DATE,
                null,
                0,
                "팬텀-전체 전직",
                2
        );
    }

    @Test
    void 유니온랭킹조회응답을반환한다() throws Exception {
        given(rankingQueryService.getUnionRanking(
                null,
                "루나",
                1
        )).willReturn(createUnionResult());

        mockMvc.perform(
                        get("/api/v1/rankings/union")
                                .param("worldName", "루나")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "UNION_RANKING_SEARCH_SUCCESS"
                                )
                )
                .andExpect(
                        jsonPath("$.data.ranking[0].unionLevel")
                                .value(9000)
                )
                .andExpect(
                        jsonPath("$.data.ranking[0].unionPower")
                                .value(123456789)
                )
                .andExpect(jsonPath("$.data.page").value(1));

        verify(rankingQueryService).getUnionRanking(
                null,
                "루나",
                1
        );
    }

    @Test
    void 무릉도장랭킹조회응답을반환한다() throws Exception {
        given(rankingQueryService.getDojangRanking(
                RANKING_DATE,
                "루나",
                1,
                "팬텀-전체 전직",
                3
        )).willReturn(createDojangResult());

        mockMvc.perform(
                        get("/api/v1/rankings/dojang")
                                .param("date", "2026-07-19")
                                .param("worldName", "루나")
                                .param("difficulty", "1")
                                .param(
                                        "className",
                                        "팬텀-전체 전직"
                                )
                                .param("page", "3")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "DOJANG_RANKING_SEARCH_SUCCESS"
                                )
                )
                .andExpect(
                        jsonPath("$.data.ranking[0].dojangFloor")
                                .value(80)
                )
                .andExpect(
                        jsonPath(
                                "$.data.ranking[0].dojangTimeRecord"
                        ).value(600)
                );

        verify(rankingQueryService).getDojangRanking(
                RANKING_DATE,
                "루나",
                1,
                "팬텀-전체 전직",
                3
        );
    }

    @Test
    void 페이지가일보다작으면입력값오류를반환한다()
            throws Exception {
        mockMvc.perform(
                        get("/api/v1/rankings/union")
                                .param("page", "0")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.code")
                                .value("GLOBAL_001")
                );

        verifyNoInteractions(rankingQueryService);
    }

    @Test
    void 월드타입이범위를벗어나면입력값오류를반환한다()
            throws Exception {
        mockMvc.perform(
                        get("/api/v1/rankings/overall")
                                .param("worldType", "2")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("GLOBAL_001")
                );

        verifyNoInteractions(rankingQueryService);
    }

    @Test
    void 무릉난이도가없으면입력값오류를반환한다()
            throws Exception {
        mockMvc.perform(
                        get("/api/v1/rankings/dojang")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("GLOBAL_001")
                );

        verifyNoInteractions(rankingQueryService);
    }

    @Test
    void 무릉난이도가범위를벗어나면입력값오류를반환한다()
            throws Exception {
        mockMvc.perform(
                        get("/api/v1/rankings/dojang")
                                .param("difficulty", "2")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("GLOBAL_001")
                );

        verifyNoInteractions(rankingQueryService);
    }

    @Test
    void 날짜형식이올바르지않으면입력값오류를반환한다()
            throws Exception {
        mockMvc.perform(
                        get("/api/v1/rankings/overall")
                                .param("date", "2026-99-99")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("GLOBAL_001")
                );

        verifyNoInteractions(rankingQueryService);
    }

    private GetOverallRankingResult createOverallResult() {
        return new GetOverallRankingResult(
                List.of(
                        new GetOverallRankingResult.Ranking(
                                1,
                                "감점",
                                "루나",
                                "팬텀",
                                "",
                                290,
                                1_234_567L,
                                321,
                                "메이플"
                        )
                ),
                2,
                RANKING_DATE,
                "NEXON_OPEN_API"
        );
    }

    private GetUnionRankingResult createUnionResult() {
        return new GetUnionRankingResult(
                List.of(
                        new GetUnionRankingResult.Ranking(
                                10,
                                "감점",
                                "루나",
                                "팬텀",
                                "",
                                9000,
                                123_456_789L
                        )
                ),
                1,
                RANKING_DATE,
                "NEXON_OPEN_API"
        );
    }

    private GetDojangRankingResult createDojangResult() {
        return new GetDojangRankingResult(
                List.of(
                        new GetDojangRankingResult.Ranking(
                                20,
                                "감점",
                                "루나",
                                "팬텀",
                                "",
                                290,
                                80,
                                600
                        )
                ),
                3,
                RANKING_DATE,
                "NEXON_OPEN_API"
        );
    }
}
