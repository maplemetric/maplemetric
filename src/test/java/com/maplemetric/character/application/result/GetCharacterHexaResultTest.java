package com.maplemetric.character.application.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;

import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSkillResponse;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetCharacterHexaResultTest {

    @Test
    void HEXA연결스킬과6차스킬이정확히일치하면첫아이콘을매핑한다() {
        CharacterHexaMatrixResponse matrixResponse =
                new CharacterHexaMatrixResponse(
                        null,
                        List.of(
                                createHexaCore(
                                        "템페스트 오브 카드 VI"
                                ),
                                createHexaCore(
                                        "템페스트 오브 카드 VI 강화"
                                )
                        )
                );

        CharacterSkillResponse skillResponse =
                new CharacterSkillResponse(
                        null,
                        "팬텀",
                        "6",
                        Arrays.asList(
                                new CharacterSkillResponse.Skill(
                                        "템페스트 오브 카드 VI",
                                        18,
                                        "first-icon"
                                ),
                                new CharacterSkillResponse.Skill(
                                        "템페스트 오브 카드 VI",
                                        18,
                                        "second-icon"
                                ),
                                null
                        )
                );

        GetCharacterHexaResult result =
                GetCharacterHexaResult.of(
                        matrixResponse,
                        createEmptyStatResponse(),
                        skillResponse
                );

        assertThat(result.cores())
                .extracting(
                        core -> core.linkedSkills().get(0).skillName(),
                        core -> core.linkedSkills().get(0).skillIcon()
                )
                .containsExactly(
                        tuple("템페스트 오브 카드 VI", "first-icon"),
                        tuple("템페스트 오브 카드 VI 강화", null)
                );
    }

    @Test
    void HEXA스탯세목록을병합하고빈서브스탯을제거한다() {
        CharacterHexaMatrixStatResponse statResponse =
                new CharacterHexaMatrixStatResponse(
                        null,
                        "팬텀",
                        List.of(createStat("0", "공격력 증가", "")),
                        List.of(createStat("1", "보스 데미지 증가", null)),
                        List.of(createStat("2", "크리티컬 데미지 증가", "주력 스탯 증가"))
                );

        GetCharacterHexaResult result =
                GetCharacterHexaResult.of(
                        new CharacterHexaMatrixResponse(
                                null,
                                null
                        ),
                        statResponse,
                        new CharacterSkillResponse(
                                null,
                                "팬텀",
                                "6",
                                null
                        )
                );

        assertThat(result.cores()).isEmpty();

        assertThat(result.stats())
                .extracting(
                        stat -> stat.statCoreNo(),
                        stat -> stat.slotNo(),
                        stat -> stat.subStats().size()
                )
                .containsExactly(
                        tuple(1, 1, 1),
                        tuple(2, 2, 1),
                        tuple(3, 3, 2)
                );
    }

    @Test
    void HEXA스탯슬롯번호가숫자가아니면응답오류로변환한다() {
        CharacterHexaMatrixStatResponse response =
                new CharacterHexaMatrixStatResponse(
                        null,
                        "팬텀",
                        List.of(createStat(
                                "invalid",
                                "공격력 증가",
                                "주력 스탯 증가"
                        )),
                        List.of(),
                        List.of()
                );

        CharacterException exception =
                catchThrowableOfType(
                        () -> GetCharacterHexaResult.of(
                                new CharacterHexaMatrixResponse(
                                        null,
                                        List.of()
                                ),
                                response,
                                new CharacterSkillResponse(
                                        null,
                                        "팬텀",
                                        "6",
                                        List.of()
                                )
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_RESPONSE_INVALID
                );
    }

    @Test
    void HEXA목록이null이면빈목록을반환한다() {
        GetCharacterHexaResult result =
                GetCharacterHexaResult.of(
                        new CharacterHexaMatrixResponse(
                                null,
                                null
                        ),
                        new CharacterHexaMatrixStatResponse(
                                null,
                                "팬텀",
                                null,
                                null,
                                null
                        ),
                        new CharacterSkillResponse(
                                null,
                                "팬텀",
                                "6",
                                null
                        )
                );

        assertThat(result.cores()).isEmpty();
        assertThat(result.stats()).isEmpty();
    }

    private CharacterHexaMatrixResponse.HexaCore createHexaCore(
            String skillName
    ) {
        return new CharacterHexaMatrixResponse.HexaCore(
                skillName,
                18,
                "마스터리 코어",
                List.of(
                        new CharacterHexaMatrixResponse.LinkedSkill(
                                skillName
                        )
                )
        );
    }

    private CharacterHexaMatrixStatResponse createEmptyStatResponse() {
        return new CharacterHexaMatrixStatResponse(
                null,
                "팬텀",
                List.of(),
                List.of(),
                List.of()
        );
    }

    private CharacterHexaMatrixStatResponse.HexaStatCore createStat(
            String slotId,
            String subStatName1,
            String subStatName2
    ) {
        return new CharacterHexaMatrixStatResponse.HexaStatCore(
                slotId,
                "크리티컬 데미지 증가",
                subStatName1,
                subStatName2,
                4,
                8,
                8
        );
    }
}
