package com.maplemetric.character.application.result;

import com.maplemetric.character.application.port.out.LoadCharacterSetEffectPort.CharacterSetEffect;
import com.maplemetric.character.application.port.out.LoadCharacterSetEffectPort.SetEffect;
import com.maplemetric.character.application.port.out.LoadCharacterSetEffectPort.SetOption;
import java.util.List;

public record GetCharacterSetEffectResult(
        List<SetEffectResult> setEffects
) {

    public static GetCharacterSetEffectResult from(
            CharacterSetEffect setEffect
    ) {
        if (setEffect.setEffects() == null) {
            return new GetCharacterSetEffectResult(List.of());
        }

        // Nexon은 착용 개수가 0인 세트도 함께 내려준다. 그대로 두면 적용되지도 않은
        // 세트가 목록에 실리므로 여기서 제외한다.
        return new GetCharacterSetEffectResult(
                setEffect.setEffects().stream()
                        .filter(effect -> effect != null)
                        .filter(effect -> isApplied(effect))
                        .map(effect -> SetEffectResult.from(effect))
                        .toList()
        );
    }

    private static boolean isApplied(SetEffect effect) {
        return effect.totalSetCount() != null
                && effect.totalSetCount() > 0;
    }

    public record SetEffectResult(
            String setName,
            Integer totalSetCount,
            List<SetOptionResult> appliedOptions,
            List<SetOptionResult> fullOptions
    ) {

        static SetEffectResult from(SetEffect effect) {
            return new SetEffectResult(
                    effect.setName(),
                    effect.totalSetCount(),
                    convert(effect.appliedOptions()),
                    convert(effect.fullOptions())
            );
        }

        private static List<SetOptionResult> convert(
                List<SetOption> options
        ) {
            if (options == null) {
                return List.of();
            }

            return options.stream()
                    .filter(option -> option != null)
                    .map(option -> new SetOptionResult(
                            option.setCount(),
                            option.setOption()
                    ))
                    .toList();
        }
    }

    public record SetOptionResult(
            Integer setCount,
            String setOption
    ) {
    }
}
