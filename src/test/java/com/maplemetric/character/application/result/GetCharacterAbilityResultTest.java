package com.maplemetric.character.application.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.maplemetric.character.application.port.out.LoadCharacterAbilityPort.CharacterAbility;
import org.junit.jupiter.api.Test;

class GetCharacterAbilityResultTest {

    @Test
    void null옵션과프리셋을빈목록으로변환한다() {
        CharacterAbility ability =
                new CharacterAbility(
                        "2026-07-19T00:00+09:00",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        GetCharacterAbilityResult result =
                GetCharacterAbilityResult.from(ability);

        assertThat(result.currentOptions())
                .isEmpty();

        assertThat(result.presets())
                .hasSize(3)
                .allSatisfy(preset ->
                        assertThat(preset.options()).isEmpty()
                );
    }
}
