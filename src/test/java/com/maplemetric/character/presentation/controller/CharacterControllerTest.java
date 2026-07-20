package com.maplemetric.character.presentation.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maplemetric.character.application.result.GetCharacterBasicResult;
import com.maplemetric.character.application.result.GetCharacterEquipmentResult;
import com.maplemetric.character.application.result.GetCharacterRankingResult;
import com.maplemetric.character.application.result.GetCharacterStatResult;
import com.maplemetric.character.application.result.GetCharacterSummaryResult;
import com.maplemetric.character.application.result.GetCharacterSymbolResult;
import com.maplemetric.character.application.result.GetCharacterUnionResult;
import com.maplemetric.character.application.service.CharacterQueryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CharacterController.class)
class CharacterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CharacterQueryService characterQueryService;

    @Test
    void 캐릭터검색에성공하면유니온과심볼정보를반환한다() throws Exception {
        given(characterQueryService.getCharacterSummary("감점"))
                .willReturn(createSummaryResult());

        mockMvc.perform(
                        get("/api/v1/characters/search")
                                .param("characterName", "감점")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.code")
                                .value("CHARACTER_SEARCH_SUCCESS")
                )
                .andExpect(
                        jsonPath("$.data.ranking.overallRank")
                                .value(58333)
                )
                .andExpect(
                        jsonPath("$.data.ranking.worldRank")
                                .value(10244)
                )
                .andExpect(
                        jsonPath("$.data.ranking.classRank")
                                .value(1588)
                )
                .andExpect(
                        jsonPath("$.data.ranking.worldClassRank")
                                .value(321)
                )
                .andExpect(
                        jsonPath("$.data.ranking.dojangFloor")
                                .value(57)
                )
                .andExpect(
                        jsonPath("$.data.union.unionLevel")
                                .value(9000)
                )
                .andExpect(
                        jsonPath("$.data.union.unionArtifactLevel")
                                .value(50)
                )
                .andExpect(
                        jsonPath(
                                "$.data.symbols.arcaneSymbols[0].symbolName"
                        )
                                .value("아케인심볼 : 소멸의 여로")
                )
                .andExpect(
                        jsonPath(
                                "$.data.symbols.arcaneSymbols[0].symbolLevel"
                        )
                                .value(20)
                )
                .andExpect(
                        jsonPath(
                                "$.data.symbols.arcaneSymbols[0].symbolForce"
                        )
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath(
                                "$.data.symbols.authenticSymbols[0].symbolName"
                        )
                                .value("어센틱심볼 : 세르니움")
                )
                .andExpect(
                        jsonPath(
                                "$.data.symbols.authenticSymbols[0].symbolLevel"
                        )
                                .value(11)
                )
                .andExpect(
                        jsonPath(
                                "$.data.symbols.authenticSymbols[1].symbolName"
                        )
                                .value("그랜드 어센틱심볼 : 탈라하트")
                )
                .andExpect(
                        jsonPath(
                                "$.data.symbols.authenticSymbols[1].symbolLevel"
                        )
                                .value(5)
                )
                .andExpect(
                        jsonPath(
                                "$.data.symbols.authenticSymbols[1].symbolForce"
                        )
                                .doesNotExist()
                );

        verify(characterQueryService)
                .getCharacterSummary("감점");
    }

    @Test
    void 캐릭터명이공백이면400응답을반환한다() throws Exception {
        mockMvc.perform(
                        get("/api/v1/characters/search")
                                .param("characterName", " ")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GLOBAL_001"))
                .andExpect(
                        jsonPath("$.message")
                                .value("요청값이 올바르지 않습니다.")
                )
                .andExpect(jsonPath("$.data").value(nullValue()));

        verifyNoInteractions(characterQueryService);
    }

    @Test
    void 캐릭터명파라미터가없으면400응답을반환한다() throws Exception {
        mockMvc.perform(
                        get("/api/v1/characters/search")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GLOBAL_001"))
                .andExpect(
                        jsonPath("$.message")
                                .value("요청값이 올바르지 않습니다.")
                )
                .andExpect(jsonPath("$.data").value(nullValue()));

        verifyNoInteractions(characterQueryService);
    }

    private GetCharacterSummaryResult createSummaryResult() {
        return GetCharacterSummaryResult.of(
                createBasicResult(),
                createStatResult(),
                createRankingResult(),
                createUnionResult(),
                createSymbolResult(),
                createEquipmentResult()
        );
    }

    private GetCharacterBasicResult createBasicResult() {
        return new GetCharacterBasicResult(
                "감점",
                "루나",
                "남",
                "팬텀",
                "6",
                282,
                1229455582005L,
                "3.020",
                "연의",
                "https://example.com/character.png",
                "2017-12-22T00:00+09:00",
                "true",
                null
        );
    }

    private GetCharacterStatResult createStatResult() {
        return new GetCharacterStatResult(
                null,
                "팬텀",
                "116871666",
                0,
                List.of()
        );
    }

    private GetCharacterRankingResult createRankingResult() {
        return new GetCharacterRankingResult(
                58333,
                10244,
                1588,
                321,
                57
        );
    }

    private GetCharacterUnionResult createUnionResult() {
        return new GetCharacterUnionResult(
                9000,
                50
        );
    }

    private GetCharacterSymbolResult createSymbolResult() {
        return new GetCharacterSymbolResult(
                List.of(
                        new GetCharacterSymbolResult.SymbolResult(
                                "아케인심볼 : 소멸의 여로",
                                20
                        )
                ),
                List.of(
                        new GetCharacterSymbolResult.SymbolResult(
                                "어센틱심볼 : 세르니움",
                                11
                        ),
                        new GetCharacterSymbolResult.SymbolResult(
                                "그랜드 어센틱심볼 : 탈라하트",
                                5
                        )
                )
        );
    }

    private GetCharacterEquipmentResult createEquipmentResult() {
        return new GetCharacterEquipmentResult(
                null,
                "남",
                "팬텀",
                2,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}