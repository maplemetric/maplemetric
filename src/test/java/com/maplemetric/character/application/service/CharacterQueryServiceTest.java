package com.maplemetric.character.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.character.application.result.GetCharacterSymbolResult;
import com.maplemetric.character.application.result.GetCharacterSummaryResult;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterBasicResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterEquipmentResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSymbolResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterUnionResponse;
import com.maplemetric.character.infrastructure.client.dto.FinalStat;
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

    @Mock
    private CharacterClient characterClient;

    private CharacterQueryService characterQueryService;

    @BeforeEach
    void setUp() {
        characterQueryService =
                new CharacterQueryService(characterClient);
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

        given(characterClient.getCharacterUnion(OCID))
                .willReturn(createUnionResponse());

        given(characterClient.getCharacterSymbol(OCID))
                .willReturn(createSymbolResponse());

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
                        symbol -> symbol.symbolLevel()
                )
                .containsExactly(
                        tuple("어센틱심볼 : 세르니움", 11),
                        tuple("그랜드 어센틱심볼 : 탈라하트", 5)
                );

        assertThat(result.equipment().presetNo())
                .isEqualTo(2);

        verify(characterClient).getOcid(CHARACTER_NAME);
        verify(characterClient).getCharacterBasic(OCID);
        verify(characterClient).getCharacterStat(OCID);
        verify(characterClient).getCharacterUnion(OCID);
        verify(characterClient).getCharacterSymbol(OCID);
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

        given(characterClient.getCharacterUnion(OCID))
                .willReturn(createUnionResponse());

        given(characterClient.getCharacterSymbol(OCID))
                .willReturn(createSymbolResponse());

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
        verify(characterClient).getCharacterUnion(OCID);
        verify(characterClient).getCharacterSymbol(OCID);
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

        given(characterClient.getCharacterUnion(OCID))
                .willReturn(createUnionResponse());

        given(characterClient.getCharacterSymbol(OCID))
                .willReturn(createSymbolResponse());

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
        verify(characterClient).getCharacterUnion(OCID);
        verify(characterClient).getCharacterSymbol(OCID);
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

        given(characterClient.getCharacterUnion(OCID))
                .willReturn(createUnionResponse());

        given(characterClient.getCharacterSymbol(OCID))
                .willReturn(createSymbolResponse());

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
        verify(characterClient).getCharacterUnion(OCID);
        verify(characterClient).getCharacterSymbol(OCID);
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

        given(characterClient.getCharacterUnion(OCID))
                .willReturn(createUnionResponse());

        given(characterClient.getCharacterSymbol(OCID))
                .willReturn(createSymbolResponse());

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
        verify(characterClient).getCharacterUnion(OCID);
        verify(characterClient).getCharacterSymbol(OCID);
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

        given(characterClient.getCharacterUnion(OCID))
                .willReturn(createUnionResponse());

        given(characterClient.getCharacterSymbol(OCID))
                .willReturn(createSymbolResponse());

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
        verify(characterClient).getCharacterUnion(OCID);
        verify(characterClient).getCharacterSymbol(OCID);
        verify(characterClient).getCharacterEquipment(OCID);
        verifyNoMoreInteractions(characterClient);
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

    private CharacterSymbolResponse createSymbolResponse() {
        return new CharacterSymbolResponse(
                null,
                "팬텀",
                List.of(
                        new CharacterSymbolResponse.Symbol(
                                "아케인심볼 : 소멸의 여로",
                                20
                        ),
                        new CharacterSymbolResponse.Symbol(
                                "어센틱심볼 : 세르니움",
                                11
                        ),
                        new CharacterSymbolResponse.Symbol(
                                "그랜드 어센틱심볼 : 탈라하트",
                                5
                        )
                )
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
}
