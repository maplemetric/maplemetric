package com.maplemetric.character.application.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.maplemetric.character.application.port.out.LoadCharacterHyperStatPort.CharacterHyperStat;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetCharacterHyperStatResultTest {

    @Test
    void 적용프리셋을숫자로변환하고null목록을빈목록으로변환한다() {
        CharacterHyperStat hyperStat =
                createResponse("2");

        GetCharacterHyperStatResult result =
                GetCharacterHyperStatResult.from(hyperStat);

        assertThat(result.appliedPresetNo())
                .isEqualTo(2);

        assertThat(result.presets())
                .hasSize(3);

        assertThat(result.presets().get(0).stats())
                .isEmpty();
    }

    @Test
    void 적용프리셋이숫자가아니면잘못된응답예외를반환한다() {
        CharacterHyperStat hyperStat =
                createResponse("invalid");

        CharacterException exception =
                catchThrowableOfType(
                        () -> GetCharacterHyperStatResult.from(hyperStat),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_RESPONSE_INVALID
                );
    }

    @Test
    void 적용프리셋이범위를벗어나면잘못된응답예외를반환한다() {
        CharacterHyperStat hyperStat =
                createResponse("4");

        CharacterException exception =
                catchThrowableOfType(
                        () -> GetCharacterHyperStatResult.from(hyperStat),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_RESPONSE_INVALID
                );
    }

    private CharacterHyperStat createResponse(
            String presetNo
    ) {
        return new CharacterHyperStat(
                "2026-07-19T00:00+09:00",
                "팬텀",
                presetNo,
                5L,
                null,
                10L,
                List.of(),
                20L,
                List.of(),
                30L
        );
    }
}
