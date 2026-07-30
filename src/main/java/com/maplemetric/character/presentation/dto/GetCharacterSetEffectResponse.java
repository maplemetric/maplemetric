package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterSetEffectResult;
import java.util.List;

public record GetCharacterSetEffectResponse(
        List<SetEffectResponse> setEffects
) {

    public static GetCharacterSetEffectResponse from(
            GetCharacterSetEffectResult result
    ) {
        return new GetCharacterSetEffectResponse(
                result.setEffects()
                        .stream()
                        .map(effect -> SetEffectResponse.from(effect))
                        .toList()
        );
    }

    public record SetEffectResponse(
            String setName,
            Integer totalSetCount,
            List<SetOptionResponse> appliedOptions,
            List<SetOptionResponse> fullOptions
    ) {

        public static SetEffectResponse from(
                GetCharacterSetEffectResult.SetEffectResult result
        ) {
            return new SetEffectResponse(
                    result.setName(),
                    result.totalSetCount(),
                    convert(result.appliedOptions()),
                    convert(result.fullOptions())
            );
        }

        private static List<SetOptionResponse> convert(
                List<GetCharacterSetEffectResult.SetOptionResult> options
        ) {
            return options.stream()
                    .map(option -> new SetOptionResponse(
                            option.setCount(),
                            option.setOption()
                    ))
                    .toList();
        }
    }

    public record SetOptionResponse(
            Integer setCount,
            String setOption
    ) {
    }
}
