package com.maplemetric.character.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.character.application.calculator.AdditionalOptionCalculationPolicyV1;
import com.maplemetric.character.application.calculator.AdditionalOptionCalculator;
import com.maplemetric.character.application.result.GetCharacterEquipmentResult;
import com.maplemetric.character.application.result.GetCharacterRankingResult;
import com.maplemetric.character.application.result.GetCharacterSymbolResult;
import com.maplemetric.character.application.result.GetCharacterSummaryResult;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterAbilityResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterBasicResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterDojangResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterEquipmentResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHyperStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterLinkSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterPopularityResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterRankingResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSymbolResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterUnionResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterVMatrixResponse;
import com.maplemetric.character.infrastructure.client.dto.FinalStat;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CharacterQueryServiceTest {

    private static final String CHARACTER_NAME = "감점";
    private static final String OCID = "test-ocid";
    private static final LocalDate RANKING_DATE =
            LocalDate.of(2026, 7, 19);

    @Mock
    private CharacterClient characterClient;

    private AdditionalOptionCalculator additionalOptionCalculator;
    private CharacterQueryService characterQueryService;

    @BeforeEach
    void setUp() {
        Clock clock =
                Clock.fixed(
                        Instant.parse("2026-07-20T00:00:00Z"),
                        ZoneId.of("Asia/Seoul")
                );

        additionalOptionCalculator =
                new AdditionalOptionCalculator(
                        new AdditionalOptionCalculationPolicyV1()
                );

        characterQueryService =
                new CharacterQueryService(
                        characterClient,
                        additionalOptionCalculator,
                        clock
                );
    }

    @Test
    void 캐릭터종합정보를조회한다() {
        CharacterBasicResponse basicResponse =
                createBasicResponse();

        CharacterStatResponse statResponse =
                createStatResponse(
                        List.of(
                                new FinalStat(
                                        "전투력",
                                        "116871666"
                                )
                        )
                );

        CharacterEquipmentResponse equipmentResponse =
                createEquipmentResponse(
                        List.of()
                );

        given(characterClient.getOcid(CHARACTER_NAME))
                .willReturn(OCID);

        given(characterClient.getCharacterBasic(OCID))
                .willReturn(basicResponse);

        given(characterClient.getCharacterStat(OCID))
                .willReturn(statResponse);

        givenRankingResponses(RANKING_DATE);

        given(characterClient.getCharacterUnion(OCID))
                .willReturn(createUnionResponse());

        given(characterClient.getCharacterSymbol(OCID))
                .willReturn(createSymbolResponse());

        givenExtendedSummaryResponses();

        given(characterClient.getCharacterEquipment(OCID))
                .willReturn(equipmentResponse);

        GetCharacterSummaryResult result =
                characterQueryService.getCharacterSummary(
                        CHARACTER_NAME
                );

        assertThat(result.basic().characterName())
                .isEqualTo(CHARACTER_NAME);

        assertThat(result.basic().worldName())
                .isEqualTo("루나");

        assertThat(result.basic().characterClass())
                .isEqualTo("팬텀");

        assertThat(result.stat().combatPower())
                .isEqualTo("116871666");

        assertThat(result.ranking().overallRank())
                .isEqualTo(58333);

        assertThat(result.ranking().worldRank())
                .isEqualTo(10244);

        assertThat(result.ranking().classRank())
                .isEqualTo(1588);

        assertThat(result.ranking().worldClassRank())
                .isEqualTo(321);

        assertThat(result.ranking().dojangFloor())
                .isEqualTo(57);

        assertThat(result.union().unionLevel())
                .isEqualTo(9000);

        assertThat(result.union().unionArtifactLevel())
                .isEqualTo(50);

        assertThat(result.symbols().arcaneSymbols())
                .extracting(
                        symbol -> symbol.symbolName(),
                        symbol -> symbol.symbolLevel()
                )
                .containsExactly(
                        tuple("아케인심볼 : 소멸의 여로", 20)
                );

        assertThat(result.symbols().authenticSymbols())
                .extracting(
                        symbol -> symbol.symbolName(),
                        symbol -> symbol.symbolLevel(),
                        symbol -> symbol.symbolIcon()
                )
                .containsExactly(
                        tuple(
                                "어센틱심볼 : 세르니움",
                                11,
                                "https://example.com/cernium.png"
                        ),
                        tuple(
                                "그랜드 어센틱심볼 : 탈라하트",
                                5,
                                "https://example.com/tallahart.png"
                        )
                );

        assertThat(result.skills().vMatrix().cores())
                .extracting(
                        core -> core.coreName(),
                        core -> core.coreLevel()
                )
                .containsExactly(
                        tuple("조커", 30)
                );

        assertThat(result.skills().linkSkills().matchedPresetNos())
                .containsExactly(1);

        assertThat(result.hexa().cores())
                .extracting(
                        core -> core.coreName(),
                        core -> core.coreLevel()
                )
                .containsExactly(
                        tuple("템페스트 오브 카드 VI", 18)
                );

        assertThat(result.hexa().stats())
                .extracting(
                        stat -> stat.statCoreNo(),
                        stat -> stat.slotNo()
                )
                .containsExactly(
                        tuple(1, 1)
                );

        assertThat(result.equipment().presetNo())
                .isEqualTo(2);

        assertThat(result.popularity().popularity())
                .isEqualTo(1234L);

        assertThat(result.hyperStat().appliedPresetNo())
                .isEqualTo(2);

        assertThat(result.hyperStat().presets())
                .hasSize(3);

        assertThat(result.ability().currentOptions())
                .hasSize(1);

        assertThat(result.ability().presets())
                .hasSize(3);

        assertThat(result.dojang().bestFloor())
                .isEqualTo(57);

        assertThat(result.dojang().bestTime())
                .isEqualTo(600);

        assertThat(result.dataUpdatedAt())
                .isEqualTo("2026-07-20T00:00:00Z");

        verify(characterClient).getOcid(CHARACTER_NAME);
        verify(characterClient).getCharacterBasic(OCID);
        verify(characterClient).getCharacterStat(OCID);
        verifyRankingCalls(RANKING_DATE);
        verify(characterClient).getCharacterUnion(OCID);
        verify(characterClient).getCharacterSymbol(OCID);
        verifyExtendedSummaryCalls();
        verify(characterClient).getCharacterEquipment(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    @Test
    void 전투력스탯을추출한다() {
        CharacterStatResponse statResponse =
                createStatResponse(
                        List.of(
                                new FinalStat(
                                        "최대 스탯공격력",
                                        "88260302"
                                ),
                                new FinalStat(
                                        "전투력",
                                        "116871666"
                                ),
                                new FinalStat(
                                        "LUK",
                                        "50724"
                                )
                        )
                );

        given(characterClient.getOcid(CHARACTER_NAME))
                .willReturn(OCID);

        given(characterClient.getCharacterBasic(OCID))
                .willReturn(createBasicResponse());

        given(characterClient.getCharacterStat(OCID))
                .willReturn(statResponse);

        givenRankingResponses(RANKING_DATE);

        given(characterClient.getCharacterUnion(OCID))
                .willReturn(createUnionResponse());

        given(characterClient.getCharacterSymbol(OCID))
                .willReturn(createSymbolResponse());

        givenExtendedSummaryResponses();

        given(characterClient.getCharacterEquipment(OCID))
                .willReturn(
                        createEquipmentResponse(List.of())
                );

        GetCharacterSummaryResult result =
                characterQueryService.getCharacterSummary(
                        CHARACTER_NAME
                );

        assertThat(result.stat().combatPower())
                .isEqualTo("116871666");

        verify(characterClient).getOcid(CHARACTER_NAME);
        verify(characterClient).getCharacterBasic(OCID);
        verify(characterClient).getCharacterStat(OCID);
        verifyRankingCalls(RANKING_DATE);
        verify(characterClient).getCharacterUnion(OCID);
        verify(characterClient).getCharacterSymbol(OCID);
        verifyExtendedSummaryCalls();
        verify(characterClient).getCharacterEquipment(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    @Test
    void 최종스탯이없으면빈목록을반환한다() {
        CharacterStatResponse statResponse =
                createStatResponse(null);

        given(characterClient.getOcid(CHARACTER_NAME))
                .willReturn(OCID);

        given(characterClient.getCharacterBasic(OCID))
                .willReturn(createBasicResponse());

        given(characterClient.getCharacterStat(OCID))
                .willReturn(statResponse);

        givenRankingResponses(RANKING_DATE);

        given(characterClient.getCharacterUnion(OCID))
                .willReturn(createUnionResponse());

        given(characterClient.getCharacterSymbol(OCID))
                .willReturn(createSymbolResponse());

        givenExtendedSummaryResponses();

        given(characterClient.getCharacterEquipment(OCID))
                .willReturn(
                        createEquipmentResponse(List.of())
                );

        GetCharacterSummaryResult result =
                characterQueryService.getCharacterSummary(
                        CHARACTER_NAME
                );

        assertThat(result.stat().combatPower())
                .isNull();

        assertThat(result.stat().finalStat())
                .isEmpty();

        verify(characterClient).getOcid(CHARACTER_NAME);
        verify(characterClient).getCharacterBasic(OCID);
        verify(characterClient).getCharacterStat(OCID);
        verifyRankingCalls(RANKING_DATE);
        verify(characterClient).getCharacterUnion(OCID);
        verify(characterClient).getCharacterSymbol(OCID);
        verifyExtendedSummaryCalls();
        verify(characterClient).getCharacterEquipment(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    @Test
    void 전투력스탯이없으면전투력을반환하지않는다() {
        CharacterStatResponse statResponse =
                createStatResponse(
                        List.of(
                                new FinalStat(
                                        "LUK",
                                        "50724"
                                )
                        )
                );

        given(characterClient.getOcid(CHARACTER_NAME))
                .willReturn(OCID);

        given(characterClient.getCharacterBasic(OCID))
                .willReturn(createBasicResponse());

        given(characterClient.getCharacterStat(OCID))
                .willReturn(statResponse);

        givenRankingResponses(RANKING_DATE);

        given(characterClient.getCharacterUnion(OCID))
                .willReturn(createUnionResponse());

        given(characterClient.getCharacterSymbol(OCID))
                .willReturn(createSymbolResponse());

        givenExtendedSummaryResponses();

        given(characterClient.getCharacterEquipment(OCID))
                .willReturn(
                        createEquipmentResponse(List.of())
                );

        GetCharacterSummaryResult result =
                characterQueryService.getCharacterSummary(
                        CHARACTER_NAME
                );

        assertThat(result.stat().combatPower())
                .isNull();

        assertThat(result.stat().finalStat())
                .hasSize(1);

        verify(characterClient).getOcid(CHARACTER_NAME);
        verify(characterClient).getCharacterBasic(OCID);
        verify(characterClient).getCharacterStat(OCID);
        verifyRankingCalls(RANKING_DATE);
        verify(characterClient).getCharacterUnion(OCID);
        verify(characterClient).getCharacterSymbol(OCID);
        verifyExtendedSummaryCalls();
        verify(characterClient).getCharacterEquipment(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    @Test
    void 장비목록이비어있어도종합정보를반환한다() {
        CharacterEquipmentResponse equipmentResponse =
                createEquipmentResponse(List.of());

        given(characterClient.getOcid(CHARACTER_NAME))
                .willReturn(OCID);

        given(characterClient.getCharacterBasic(OCID))
                .willReturn(createBasicResponse());

        given(characterClient.getCharacterStat(OCID))
                .willReturn(
                        createStatResponse(List.of())
                );

        givenRankingResponses(RANKING_DATE);

        given(characterClient.getCharacterUnion(OCID))
                .willReturn(createUnionResponse());

        given(characterClient.getCharacterSymbol(OCID))
                .willReturn(createSymbolResponse());

        givenExtendedSummaryResponses();

        given(characterClient.getCharacterEquipment(OCID))
                .willReturn(equipmentResponse);

        GetCharacterSummaryResult result =
                characterQueryService.getCharacterSummary(
                        CHARACTER_NAME
                );

        assertThat(result.equipment().itemEquipment())
                .isEmpty();

        assertThat(result.equipment().itemEquipmentPreset1())
                .isEmpty();

        assertThat(result.equipment().itemEquipmentPreset2())
                .isEmpty();

        assertThat(result.equipment().itemEquipmentPreset3())
                .isEmpty();

        verify(characterClient).getOcid(CHARACTER_NAME);
        verify(characterClient).getCharacterBasic(OCID);
        verify(characterClient).getCharacterStat(OCID);
        verifyRankingCalls(RANKING_DATE);
        verify(characterClient).getCharacterUnion(OCID);
        verify(characterClient).getCharacterSymbol(OCID);
        verifyExtendedSummaryCalls();
        verify(characterClient).getCharacterEquipment(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    @Test
    void 포스스탯은최종스탯목록에그대로유지한다() {
        CharacterStatResponse statResponse =
                createStatResponse(
                        List.of(
                                new FinalStat(
                                        "전투력",
                                        "116871666"
                                ),
                                new FinalStat(
                                        "아케인포스",
                                        "1320"
                                ),
                                new FinalStat(
                                        "어센틱포스",
                                        "660"
                                )
                        )
                );

        given(characterClient.getOcid(CHARACTER_NAME))
                .willReturn(OCID);

        given(characterClient.getCharacterBasic(OCID))
                .willReturn(createBasicResponse());

        given(characterClient.getCharacterStat(OCID))
                .willReturn(statResponse);

        givenRankingResponses(RANKING_DATE);

        given(characterClient.getCharacterUnion(OCID))
                .willReturn(createUnionResponse());

        given(characterClient.getCharacterSymbol(OCID))
                .willReturn(createSymbolResponse());

        givenExtendedSummaryResponses();

        given(characterClient.getCharacterEquipment(OCID))
                .willReturn(
                        createEquipmentResponse(List.of())
                );

        GetCharacterSummaryResult result =
                characterQueryService.getCharacterSummary(
                        CHARACTER_NAME
                );

        assertThat(result.stat().finalStat())
                .extracting(
                        finalStat -> finalStat.statName(),
                        finalStat -> finalStat.statValue()
                )
                .containsExactly(
                        tuple("전투력", "116871666"),
                        tuple("아케인포스", "1320"),
                        tuple("어센틱포스", "660")
                );

        assertThat(result.stat().combatPower())
                .isEqualTo("116871666");

        verify(characterClient).getOcid(CHARACTER_NAME);
        verify(characterClient).getCharacterBasic(OCID);
        verify(characterClient).getCharacterStat(OCID);
        verifyRankingCalls(RANKING_DATE);
        verify(characterClient).getCharacterUnion(OCID);
        verify(characterClient).getCharacterSymbol(OCID);
        verifyExtendedSummaryCalls();
        verify(characterClient).getCharacterEquipment(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    @Test
    void 랭킹기준일은KST오전9시29분이면전일이다() {
        CharacterQueryService service =
                new CharacterQueryService(
                        characterClient,
                        additionalOptionCalculator,
                        Clock.fixed(
                                Instant.parse(
                                        "2026-07-21T00:29:00Z"
                                ),
                                ZoneId.of("Asia/Seoul")
                        )
                );

        LocalDate expectedRankingDate =
                LocalDate.of(2026, 7, 20);

        givenSummaryResponses(expectedRankingDate);

        service.getCharacterSummary(CHARACTER_NAME);

        verifyRankingCalls(expectedRankingDate);
        verifyExtendedSummaryCalls();
    }

    @Test
    void 랭킹기준일은KST오전9시30분이면당일이다() {
        CharacterQueryService service =
                new CharacterQueryService(
                        characterClient,
                        additionalOptionCalculator,
                        Clock.fixed(
                                Instant.parse(
                                        "2026-07-21T00:30:00Z"
                                ),
                                ZoneId.of("Asia/Seoul")
                        )
                );

        LocalDate expectedRankingDate =
                LocalDate.of(2026, 7, 21);

        givenSummaryResponses(expectedRankingDate);

        service.getCharacterSummary(CHARACTER_NAME);

        verifyRankingCalls(expectedRankingDate);
        verifyExtendedSummaryCalls();
    }

    @Test
    void 직업랭킹필터를구할수없으면직업랭킹을조회하지않는다() {
        CharacterRankingResponse emptyRankingResponse =
                new CharacterRankingResponse(List.of());

        given(characterClient.getOcid(CHARACTER_NAME))
                .willReturn(OCID);

        given(characterClient.getCharacterBasic(OCID))
                .willReturn(createBasicResponse());

        given(characterClient.getCharacterStat(OCID))
                .willReturn(createStatResponse(List.of()));

        given(characterClient.getOverallRanking(
                OCID,
                RANKING_DATE
        )).willReturn(emptyRankingResponse);

        given(characterClient.getWorldRanking(
                OCID,
                "루나",
                RANKING_DATE
        )).willReturn(emptyRankingResponse);

        given(characterClient.getCharacterDojang(OCID))
                .willReturn(createDojangResponse(null));

        given(characterClient.getCharacterUnion(OCID))
                .willReturn(createUnionResponse());

        given(characterClient.getCharacterSymbol(OCID))
                .willReturn(createSymbolResponse());

        givenExtendedSummaryResponses();

        given(characterClient.getCharacterEquipment(OCID))
                .willReturn(createEquipmentResponse(List.of()));

        GetCharacterSummaryResult result =
                characterQueryService.getCharacterSummary(
                        CHARACTER_NAME
                );

        assertThat(result.ranking().classRank())
                .isNull();

        assertThat(result.ranking().worldClassRank())
                .isNull();

        verify(characterClient).getOcid(CHARACTER_NAME);
        verify(characterClient).getCharacterBasic(OCID);
        verify(characterClient).getCharacterStat(OCID);
        verify(characterClient).getOverallRanking(OCID, RANKING_DATE);
        verify(characterClient).getWorldRanking(OCID, "루나", RANKING_DATE);
        verify(characterClient, never())
                .getClassRanking(
                        anyString(),
                        anyString(),
                        any(LocalDate.class)
                );

        verify(characterClient, never())
                .getWorldClassRanking(
                        anyString(),
                        anyString(),
                        anyString(),
                        any(LocalDate.class)
                );
        verify(characterClient).getCharacterDojang(OCID);
        verify(characterClient).getCharacterUnion(OCID);
        verify(characterClient).getCharacterSymbol(OCID);
        verifyExtendedSummaryCalls();
        verify(characterClient).getCharacterEquipment(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    @Test
    void 랭킹목록에서대상캐릭터명과일치하는순위를선택한다() {
        CharacterRankingResponse response =
                new CharacterRankingResponse(
                        List.of(
                                new CharacterRankingResponse.Ranking(
                                        1,
                                        "다른캐릭터",
                                        "루나",
                                        "팬텀",
                                        null
                                ),
                                new CharacterRankingResponse.Ranking(
                                        58333,
                                        CHARACTER_NAME,
                                        "루나",
                                        "팬텀",
                                        null
                                )
                        )
                );

        GetCharacterRankingResult result =
                GetCharacterRankingResult.of(
                        CHARACTER_NAME,
                        response,
                        response,
                        response,
                        response,
                        createDojangResponse(57)
                );

        assertThat(result.overallRank())
                .isEqualTo(58333);

        assertThat(result.worldRank())
                .isEqualTo(58333);

        assertThat(result.classRank())
                .isEqualTo(58333);

        assertThat(result.worldClassRank())
                .isEqualTo(58333);
    }

    @Test
    void 랭킹응답이200이지만목록이비어있으면순위는null이다() {
        CharacterRankingResponse response =
                new CharacterRankingResponse(List.of());

        GetCharacterRankingResult result =
                GetCharacterRankingResult.of(
                        CHARACTER_NAME,
                        response,
                        response,
                        response,
                        response,
                        createDojangResponse(57)
                );

        assertThat(result.overallRank())
                .isNull();

        assertThat(result.worldRank())
                .isNull();

        assertThat(result.classRank())
                .isNull();

        assertThat(result.worldClassRank())
                .isNull();
    }

    @Test
    void 심볼목록이없으면빈목록을반환한다() {
        CharacterSymbolResponse response =
                new CharacterSymbolResponse(
                        null,
                        "팬텀",
                        null
                );

        GetCharacterSymbolResult result =
                GetCharacterSymbolResult.from(response);

        assertThat(result.arcaneSymbols())
                .isEmpty();

        assertThat(result.authenticSymbols())
                .isEmpty();
    }

    @Test
    void 캐릭터명형식이잘못되면외부API를호출하지않는다() {
        String invalidCharacterName =
                "없는캐릭터이름123456";

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterQueryService
                                .getCharacterSummary(
                                        invalidCharacterName
                                ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.INVALID_CHARACTER_NAME
                );

        verifyNoInteractions(characterClient);
    }

    @Test
    void 종합조회장비네목록에추가옵션계산결과를포함한다() {
        CharacterEquipmentResponse.ItemEquipment item =
                createItemEquipment();

        givenSummaryResponses(RANKING_DATE);

        given(characterClient.getCharacterEquipment(OCID))
                .willReturn(
                        createEquipmentResponse(List.of(item))
                );

        GetCharacterSummaryResult result =
                characterQueryService.getCharacterSummary(
                        CHARACTER_NAME
                );

        assertThat(result.equipment().itemEquipment())
                .extracting(itemResult ->
                        itemResult.additionalOptionEvaluation().score()
                )
                .containsExactly(new BigDecimal("158.0"));

        assertThat(
                result.equipment()
                        .itemEquipment()
                        .get(0)
                        .itemAddOption()
                        .luk()
        ).isEqualTo("80");

        assertThat(result.equipment().itemEquipmentPreset1())
                .extracting(itemResult ->
                        itemResult.additionalOptionEvaluation().score()
                )
                .containsExactly(new BigDecimal("158.0"));
        assertThat(result.equipment().itemEquipmentPreset2())
                .extracting(itemResult ->
                        itemResult.additionalOptionEvaluation().score()
                )
                .containsExactly(new BigDecimal("158.0"));
        assertThat(result.equipment().itemEquipmentPreset3())
                .extracting(itemResult ->
                        itemResult.additionalOptionEvaluation().score()
                )
                .containsExactly(new BigDecimal("158.0"));
    }

    @Test
    void 장비단건조회에도동일한추가옵션계산기를사용한다() {
        given(characterClient.getOcid(CHARACTER_NAME))
                .willReturn(OCID);

        given(characterClient.getCharacterEquipment(OCID))
                .willReturn(
                        createEquipmentResponse(
                                List.of(createItemEquipment())
                        )
                );

        GetCharacterEquipmentResult result =
                characterQueryService.getCharacterEquipment(
                        CHARACTER_NAME
                );

        assertThat(
                result.itemEquipment()
                        .get(0)
                        .additionalOptionEvaluation()
                        .score()
        ).isEqualByComparingTo("158.0");

        verify(characterClient).getOcid(CHARACTER_NAME);
        verify(characterClient).getCharacterEquipment(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    private void givenSummaryResponses(
            LocalDate rankingDate
    ) {
        given(characterClient.getOcid(CHARACTER_NAME))
                .willReturn(OCID);

        given(characterClient.getCharacterBasic(OCID))
                .willReturn(createBasicResponse());

        given(characterClient.getCharacterStat(OCID))
                .willReturn(createStatResponse(List.of()));

        givenRankingResponses(rankingDate);

        given(characterClient.getCharacterUnion(OCID))
                .willReturn(createUnionResponse());

        given(characterClient.getCharacterSymbol(OCID))
                .willReturn(createSymbolResponse());

        givenExtendedSummaryResponses();

        given(characterClient.getCharacterEquipment(OCID))
                .willReturn(createEquipmentResponse(List.of()));
    }

    private void givenRankingResponses(
            LocalDate rankingDate
    ) {
        given(characterClient.getOverallRanking(OCID, rankingDate))
                .willReturn(createRankingResponse(
                        CHARACTER_NAME,
                        58333,
                        "팬텀",
                        null
                ));

        given(characterClient.getWorldRanking(OCID, "루나", rankingDate))
                .willReturn(createRankingResponse(
                        CHARACTER_NAME,
                        10244,
                        "팬텀",
                        null
                ));

        given(characterClient.getClassRanking(
                OCID,
                "팬텀-전체 전직",
                rankingDate
        )).willReturn(createRankingResponse(
                CHARACTER_NAME,
                1588,
                "팬텀",
                null
        ));

        given(characterClient.getWorldClassRanking(
                OCID,
                "루나",
                "팬텀-전체 전직",
                rankingDate
        )).willReturn(createRankingResponse(
                CHARACTER_NAME,
                321,
                "팬텀",
                null
        ));

        given(characterClient.getCharacterDojang(OCID))
                .willReturn(createDojangResponse(57));
    }

    private void verifyRankingCalls(
            LocalDate rankingDate
    ) {
        verify(characterClient)
                .getOverallRanking(OCID, rankingDate);

        verify(characterClient)
                .getWorldRanking(OCID, "루나", rankingDate);

        verify(characterClient)
                .getClassRanking(
                        OCID,
                        "팬텀-전체 전직",
                        rankingDate
                );

        verify(characterClient)
                .getWorldClassRanking(
                        OCID,
                        "루나",
                        "팬텀-전체 전직",
                        rankingDate
                );

        verify(characterClient)
                .getCharacterDojang(OCID);
    }

    private void givenExtendedSummaryResponses() {
        given(characterClient.getCharacterPopularity(OCID))
                .willReturn(createPopularityResponse());

        given(characterClient.getCharacterHyperStat(OCID))
                .willReturn(createHyperStatResponse());

        given(characterClient.getCharacterAbility(OCID))
                .willReturn(createAbilityResponse());

        given(characterClient.getCharacterSkill(OCID, "5"))
                .willReturn(createFifthSkillResponse());

        given(characterClient.getCharacterVMatrix(OCID))
                .willReturn(createVMatrixResponse());

        given(characterClient.getCharacterLinkSkill(OCID))
                .willReturn(createLinkSkillResponse());

        given(characterClient.getCharacterSkill(OCID, "6"))
                .willReturn(createSixthSkillResponse());

        given(characterClient.getCharacterHexaMatrix(OCID))
                .willReturn(createHexaMatrixResponse());

        given(characterClient.getCharacterHexaMatrixStat(OCID))
                .willReturn(createHexaMatrixStatResponse());
    }

    private void verifyExtendedSummaryCalls() {
        verify(characterClient)
                .getCharacterPopularity(OCID);

        verify(characterClient)
                .getCharacterHyperStat(OCID);

        verify(characterClient)
                .getCharacterAbility(OCID);

        verify(characterClient)
                .getCharacterSkill(OCID, "5");

        verify(characterClient)
                .getCharacterVMatrix(OCID);

        verify(characterClient)
                .getCharacterLinkSkill(OCID);

        verify(characterClient)
                .getCharacterSkill(OCID, "6");

        verify(characterClient)
                .getCharacterHexaMatrix(OCID);

        verify(characterClient)
                .getCharacterHexaMatrixStat(OCID);
    }

    private CharacterRankingResponse createRankingResponse(
            String characterName,
            Integer ranking,
            String className,
            String subClassName
    ) {
        return new CharacterRankingResponse(
                List.of(
                        new CharacterRankingResponse.Ranking(
                                ranking,
                                characterName,
                                "루나",
                                className,
                                subClassName
                        )
                )
        );
    }

    private CharacterBasicResponse createBasicResponse() {
        return new CharacterBasicResponse(
                null,
                CHARACTER_NAME,
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

    private CharacterStatResponse createStatResponse(
            List<FinalStat> finalStat
    ) {
        return new CharacterStatResponse(
                null,
                "팬텀",
                finalStat,
                0
        );
    }

    private CharacterUnionResponse createUnionResponse() {
        return new CharacterUnionResponse(
                9000,
                50
        );
    }

    private CharacterPopularityResponse createPopularityResponse() {
        return new CharacterPopularityResponse(
                "2026-07-19T00:00+09:00",
                1234L
        );
    }

    private CharacterHyperStatResponse createHyperStatResponse() {
        return new CharacterHyperStatResponse(
                "2026-07-19T00:00+09:00",
                "팬텀",
                "2",
                5L,
                List.of(
                        new CharacterHyperStatResponse.HyperStat(
                                "크리티컬 확률",
                                15L,
                                5,
                                "크리티컬 확률 5% 증가"
                        )
                ),
                10L,
                null,
                20L,
                List.of(),
                30L
        );
    }

    private CharacterAbilityResponse createAbilityResponse() {
        CharacterAbilityResponse.AbilityInfo abilityInfo =
                new CharacterAbilityResponse.AbilityInfo(
                        "1",
                        "레전드리",
                        "보스 몬스터 공격 시 데미지 20% 증가"
                );

        return new CharacterAbilityResponse(
                "2026-07-19T00:00+09:00",
                "레전드리",
                List.of(abilityInfo),
                100L,
                1,
                new CharacterAbilityResponse.AbilityPreset(
                        "레전드리",
                        List.of(abilityInfo)
                ),
                null,
                new CharacterAbilityResponse.AbilityPreset(
                        "에픽",
                        null
                )
        );
    }

    private CharacterDojangResponse createDojangResponse(
            Integer bestFloor
    ) {
        return new CharacterDojangResponse(
                "2026-07-19T00:00+09:00",
                "팬텀",
                "루나",
                bestFloor,
                "2026-07-18T00:00+09:00",
                bestFloor == null ? null : 600
        );
    }

    private CharacterSymbolResponse createSymbolResponse() {
        return new CharacterSymbolResponse(
                null,
                "팬텀",
                List.of(
                        new CharacterSymbolResponse.Symbol(
                                "아케인심볼 : 소멸의 여로",
                                20,
                                "https://example.com/vanishing.png"
                        ),
                        new CharacterSymbolResponse.Symbol(
                                "어센틱심볼 : 세르니움",
                                11,
                                "https://example.com/cernium.png"
                        ),
                        new CharacterSymbolResponse.Symbol(
                                "그랜드 어센틱심볼 : 탈라하트",
                                5,
                                "https://example.com/tallahart.png"
                        )
                )
        );
    }

    private CharacterSkillResponse createFifthSkillResponse() {
        return new CharacterSkillResponse(
                null,
                "팬텀",
                "5",
                List.of(
                        new CharacterSkillResponse.Skill(
                                "조커",
                                30,
                                "https://example.com/joker.png"
                        )
                )
        );
    }

    private CharacterVMatrixResponse createVMatrixResponse() {
        return new CharacterVMatrixResponse(
                null,
                "팬텀",
                List.of(
                        new CharacterVMatrixResponse.VCore(
                                "조커",
                                "직업 코어",
                                30
                        )
                )
        );
    }

    private CharacterLinkSkillResponse createLinkSkillResponse() {
        CharacterLinkSkillResponse.LinkSkill linkSkill =
                new CharacterLinkSkillResponse.LinkSkill(
                        "데들리 인스팅트",
                        2,
                        "https://example.com/deadly-instinct.png"
                );

        return new CharacterLinkSkillResponse(
                null,
                "팬텀",
                List.of(linkSkill),
                List.of(linkSkill),
                List.of(),
                List.of()
        );
    }

    private CharacterSkillResponse createSixthSkillResponse() {
        return new CharacterSkillResponse(
                null,
                "팬텀",
                "6",
                List.of(
                        new CharacterSkillResponse.Skill(
                                "템페스트 오브 카드 VI",
                                18,
                                "https://example.com/tempest-vi.png"
                        )
                )
        );
    }

    private CharacterHexaMatrixResponse createHexaMatrixResponse() {
        return new CharacterHexaMatrixResponse(
                null,
                List.of(
                        new CharacterHexaMatrixResponse.HexaCore(
                                "템페스트 오브 카드 VI",
                                18,
                                "마스터리 코어",
                                List.of(
                                        new CharacterHexaMatrixResponse.LinkedSkill(
                                                "템페스트 오브 카드 VI"
                                        )
                                )
                        )
                )
        );
    }

    private CharacterHexaMatrixStatResponse createHexaMatrixStatResponse() {
        return new CharacterHexaMatrixStatResponse(
                null,
                "팬텀",
                List.of(
                        new CharacterHexaMatrixStatResponse.HexaStatCore(
                                "0",
                                "크리티컬 데미지 증가",
                                "공격력 증가",
                                "주력 스탯 증가",
                                4,
                                8,
                                8
                        )
                ),
                List.of(),
                List.of()
        );
    }

    private CharacterEquipmentResponse createEquipmentResponse(
            List<CharacterEquipmentResponse.ItemEquipment> items
    ) {
        return new CharacterEquipmentResponse(
                null,
                "남",
                "팬텀",
                2,
                items,
                items,
                items,
                items
        );
    }

    private CharacterEquipmentResponse.ItemEquipment createItemEquipment() {
        return new CharacterEquipmentResponse.ItemEquipment(
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
                createAdditionalOption(),
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

    private CharacterEquipmentResponse.ItemOption createAdditionalOption() {
        return new CharacterEquipmentResponse.ItemOption(
                "0",
                "40",
                "0",
                "80",
                "0",
                null,
                "6",
                "0",
                null,
                null,
                null,
                null,
                null,
                "5",
                null,
                null,
                null,
                null
        );
    }
}
