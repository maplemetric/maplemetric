package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterHyperStatResult;
import java.util.List;

public record GetCharacterHyperStatResponse(
        String date,
        String characterClass,
        Integer appliedPresetNo,
        Long availablePoints,
        List<HyperStatPresetResponse> presets
) {

    public static GetCharacterHyperStatResponse from(
            GetCharacterHyperStatResult result
    ) {
        return new GetCharacterHyperStatResponse(
                result.date(),
                result.characterClass(),
                result.appliedPresetNo(),
                result.availablePoints(),
                result.presets().stream()
                        .map(preset -> new HyperStatPresetResponse(
                                preset.presetNo(),
                                preset.remainPoints(),
                                preset.stats().stream()
                                        .map(stat -> new HyperStatResponse(
                                                stat.statType(),
                                                stat.statPoint(),
                                                stat.statLevel(),
                                                stat.statIncrease()
                                        ))
                                        .toList()
                        ))
                        .toList()
        );
    }

    public record HyperStatPresetResponse(
            Integer presetNo,
            Long remainPoints,
            List<HyperStatResponse> stats
    ) {
    }

    public record HyperStatResponse(
            String statType,
            Long statPoint,
            Integer statLevel,
            String statIncrease
    ) {
    }
}
