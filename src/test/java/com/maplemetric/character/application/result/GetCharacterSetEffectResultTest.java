package com.maplemetric.character.application.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.maplemetric.character.application.port.out.LoadCharacterSetEffectPort.CharacterSetEffect;
import com.maplemetric.character.application.port.out.LoadCharacterSetEffectPort.SetEffect;
import com.maplemetric.character.application.port.out.LoadCharacterSetEffectPort.SetOption;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetCharacterSetEffectResultTest {

    @Test
    void 착용하지않은세트를제외한다() {
        CharacterSetEffect setEffect = new CharacterSetEffect(
                Arrays.asList(
                        createSetEffect("여명의 보스 세트", 2),
                        createSetEffect("루타비스 세트(도적)", 0),
                        createSetEffect("착용수미상 세트", null),
                        null
                )
        );

        GetCharacterSetEffectResult result =
                GetCharacterSetEffectResult.from(setEffect);

        assertThat(result.setEffects())
                .extracting(
                        effect -> effect.setName(),
                        effect -> effect.totalSetCount()
                )
                .containsExactly(
                        tuple("여명의 보스 세트", 2)
                );
    }

    @Test
    void 세트효과목록이null이면빈목록을반환한다() {
        GetCharacterSetEffectResult result =
                GetCharacterSetEffectResult.from(
                        new CharacterSetEffect(null)
                );

        assertThat(result.setEffects()).isEmpty();
    }

    @Test
    void 옵션목록이null이면빈목록으로변환한다() {
        CharacterSetEffect setEffect = new CharacterSetEffect(
                List.of(
                        new SetEffect(
                                "여명의 보스 세트",
                                2,
                                null,
                                null
                        )
                )
        );

        GetCharacterSetEffectResult result =
                GetCharacterSetEffectResult.from(setEffect);

        assertThat(result.setEffects().get(0).appliedOptions())
                .isEmpty();

        assertThat(result.setEffects().get(0).fullOptions())
                .isEmpty();
    }

    private SetEffect createSetEffect(
            String setName,
            Integer totalSetCount
    ) {
        return new SetEffect(
                setName,
                totalSetCount,
                List.of(
                        new SetOption(
                                2,
                                "보스 몬스터 공격 시 데미지 : +10%"
                        )
                ),
                List.of(
                        new SetOption(
                                2,
                                "보스 몬스터 공격 시 데미지 : +10%"
                        )
                )
        );
    }
}
