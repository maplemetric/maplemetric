package com.maplemetric.character.application.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;

import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.CharacterHexa;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.HexaCore;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.HexaStatCore;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.LinkedSkill;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.SixthSkill;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetCharacterHexaResultTest {

    @Test
    void HEXA연결스킬과6차스킬이정확히일치하면첫아이콘을매핑한다() {
        List<HexaCore> cores = List.of(
                createHexaCore("템페스트 오브 카드 VI"),
                createHexaCore("템페스트 오브 카드 VI 강화")
        );

        List<SixthSkill> sixthSkills = Arrays.asList(
                new SixthSkill(
                        "템페스트 오브 카드 VI",
                        "first-icon"
                ),
                new SixthSkill(
                        "템페스트 오브 카드 VI",
                        "second-icon"
                ),
                null
        );

        GetCharacterHexaResult result =
                GetCharacterHexaResult.from(
                        new CharacterHexa(
                                cores,
                                sixthSkills,
                                List.of(),
                                List.of(),
                                List.of()
                        )
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
        CharacterHexa hexa = new CharacterHexa(
                null,
                null,
                List.of(createStat("0", "공격력 증가", "")),
                List.of(createStat("1", "보스 데미지 증가", null)),
                List.of(createStat("2", "크리티컬 데미지 증가", "주력 스탯 증가"))
        );

        GetCharacterHexaResult result =
                GetCharacterHexaResult.from(hexa);

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
        CharacterHexa hexa = new CharacterHexa(
                List.of(),
                List.of(),
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
                        () -> GetCharacterHexaResult.from(hexa),
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
                GetCharacterHexaResult.from(
                        new CharacterHexa(
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                );

        assertThat(result.cores()).isEmpty();
        assertThat(result.stats()).isEmpty();
    }

    private HexaCore createHexaCore(
            String skillName
    ) {
        return new HexaCore(
                skillName,
                "마스터리 코어",
                18,
                List.of(
                        new LinkedSkill(
                                skillName
                        )
                )
        );
    }

    private HexaStatCore createStat(
            String slotId,
            String subStatName1,
            String subStatName2
    ) {
        return new HexaStatCore(
                slotId,
                "크리티컬 데미지 증가",
                4,
                subStatName1,
                8,
                subStatName2,
                8
        );
    }
}
