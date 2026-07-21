package com.maplemetric.character.presentation.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maplemetric.character.application.result.AdditionalOptionEvaluationResult;
import com.maplemetric.character.application.result.GetCharacterBasicResult;
import com.maplemetric.character.application.result.GetCharacterEquipmentResult;
import com.maplemetric.character.application.result.GetCharacterHexaResult;
import com.maplemetric.character.application.result.GetCharacterRankingResult;
import com.maplemetric.character.application.result.GetCharacterSkillsResult;
import com.maplemetric.character.application.result.GetCharacterStatResult;
import com.maplemetric.character.application.result.GetCharacterSummaryResult;
import com.maplemetric.character.application.result.GetCharacterSymbolResult;
import com.maplemetric.character.application.result.GetCharacterUnionResult;
import com.maplemetric.character.application.service.CharacterQueryService;
import java.math.BigDecimal;
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
                                "$.data.symbols.arcaneSymbols[0].symbolIcon"
                        )
                                .value("arcane-icon")
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
                )
                .andExpect(
                        jsonPath(
                                "$.data.symbols.authenticSymbols[0].symbolIcon"
                        )
                                .value("authentic-icon")
                )
                .andExpect(
                        jsonPath(
                                "$.data.skills.vMatrix.cores[0].coreName"
                        )
                                .value("조커")
                )
                .andExpect(
                        jsonPath(
                                "$.data.skills.linkSkills.currentSkills[0].skillName"
                        )
                                .value("데들리 인스팅트")
                )
                .andExpect(
                        jsonPath(
                                "$.data.skills.linkSkills.matchedPresetNos[0]"
                        )
                                .value(2)
                )
                .andExpect(
                        jsonPath(
                                "$.data.skills.linkSkills.presets.length()"
                        )
                                .value(3)
                )
                .andExpect(
                        jsonPath("$.data.hexa.cores[0].coreName")
                                .value("템페스트 오브 카드 VI")
                )
                .andExpect(
                        jsonPath("$.data.hexa.stats[0].statCoreNo")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.data.hexa.stats[0].slotNo")
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.data.equipment.itemEquipment[0]"
                                        + ".additionalOptionEvaluation.calculable"
                        ).value(true)
                )
                .andExpect(
                        jsonPath(
                                "$.data.equipment.itemEquipment[0]"
                                        + ".additionalOptionEvaluation.score"
                        ).value(158.0)
                )
                .andExpect(
                        jsonPath(
                                "$.data.equipment.itemEquipment[0]"
                                        + ".additionalOptionEvaluation.grade"
                        ).value(150)
                )
                .andExpect(
                        jsonPath(
                                "$.data.equipment.itemEquipment[0]"
                                        + ".additionalOptionEvaluation.criteria"
                                        + ".mainStats[0]"
                        ).value("LUK")
                )
                .andExpect(
                        jsonPath(
                                "$.data.equipment.itemEquipmentPreset1[0]"
                                        + ".additionalOptionEvaluation.score"
                        ).value(158.0)
                )
                .andExpect(
                        jsonPath(
                                "$.data.equipment.itemEquipmentPreset2[0]"
                                        + ".additionalOptionEvaluation.score"
                        ).value(158.0)
                )
                .andExpect(
                        jsonPath(
                                "$.data.equipment.itemEquipmentPreset3[0]"
                                        + ".additionalOptionEvaluation.score"
                        ).value(158.0)
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
                createSkillsResult(),
                createHexaResult(),
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
                                20,
                                "arcane-icon"
                        )
                ),
                List.of(
                        new GetCharacterSymbolResult.SymbolResult(
                                "어센틱심볼 : 세르니움",
                                11,
                                "authentic-icon"
                        ),
                        new GetCharacterSymbolResult.SymbolResult(
                                "그랜드 어센틱심볼 : 탈라하트",
                                5,
                                "grand-authentic-icon"
                        )
                )
        );
    }

    private GetCharacterSkillsResult createSkillsResult() {
        GetCharacterSkillsResult.LinkSkillResult linkSkill =
                new GetCharacterSkillsResult.LinkSkillResult(
                        "데들리 인스팅트",
                        2,
                        "link-icon"
                );

        return new GetCharacterSkillsResult(
                new GetCharacterSkillsResult.VMatrixResult(
                        List.of(
                                new GetCharacterSkillsResult.VCoreResult(
                                        "조커",
                                        "직업 코어",
                                        30,
                                        List.of(
                                                new GetCharacterSkillsResult.SkillResult(
                                                        "조커",
                                                        "joker-icon"
                                                )
                                        )
                                )
                        )
                ),
                new GetCharacterSkillsResult.LinkSkillsResult(
                        List.of(linkSkill),
                        List.of(2),
                        List.of(
                                new GetCharacterSkillsResult.LinkPresetResult(
                                        1,
                                        List.of()
                                ),
                                new GetCharacterSkillsResult.LinkPresetResult(
                                        2,
                                        List.of(linkSkill)
                                ),
                                new GetCharacterSkillsResult.LinkPresetResult(
                                        3,
                                        List.of()
                                )
                        )
                )
        );
    }

    private GetCharacterHexaResult createHexaResult() {
        return new GetCharacterHexaResult(
                List.of(
                        new GetCharacterHexaResult.HexaCoreResult(
                                "템페스트 오브 카드 VI",
                                "마스터리 코어",
                                18,
                                List.of(
                                        new GetCharacterHexaResult.LinkedSkillResult(
                                                "템페스트 오브 카드 VI",
                                                "tempest-icon"
                                        )
                                )
                        )
                ),
                List.of(
                        new GetCharacterHexaResult.HexaStatResult(
                                1,
                                1,
                                "크리티컬 데미지 증가",
                                4,
                                List.of(
                                        new GetCharacterHexaResult.SubStatResult(
                                                "공격력 증가",
                                                8
                                        )
                                )
                        )
                )
        );
    }

    private GetCharacterEquipmentResult createEquipmentResult() {
        GetCharacterEquipmentResult.ItemEquipmentResult item =
                createItemEquipmentResult();

        return new GetCharacterEquipmentResult(
                null,
                "남",
                "팬텀",
                2,
                List.of(item),
                List.of(item),
                List.of(item),
                List.of(item)
        );
    }

    private GetCharacterEquipmentResult.ItemEquipmentResult
    createItemEquipmentResult() {
        AdditionalOptionEvaluationResult evaluation =
                new AdditionalOptionEvaluationResult(
                        true,
                        new BigDecimal("158.0"),
                        150,
                        "v1",
                        "LUK",
                        new AdditionalOptionEvaluationResult.CriteriaResult(
                                List.of("LUK"),
                                List.of("DEX"),
                                true,
                                false,
                                new BigDecimal("1.0"),
                                new BigDecimal("0.1"),
                                new BigDecimal("4.0"),
                                new BigDecimal("4.0"),
                                new BigDecimal("10.0")
                        ),
                        null
                );

        return new GetCharacterEquipmentResult.ItemEquipmentResult(
                "장갑",
                "장갑",
                "테스트 장갑",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                evaluation,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
