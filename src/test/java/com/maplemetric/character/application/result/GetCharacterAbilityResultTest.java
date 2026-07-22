package com.maplemetric.character.application.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.maplemetric.character.infrastructure.client.dto.CharacterAbilityResponse;
import org.junit.jupiter.api.Test;

class GetCharacterAbilityResultTest {

    @Test
    void null옵션과프리셋을빈목록으로변환한다() {
        CharacterAbilityResponse response =
                new CharacterAbilityResponse(
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
                GetCharacterAbilityResult.from(response);

        assertThat(result.currentOptions())
                .isEmpty();

        assertThat(result.presets())
                .hasSize(3)
                .allSatisfy(preset ->
                        assertThat(preset.options()).isEmpty()
                );
    }
}
