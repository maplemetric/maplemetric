package com.maplemetric.character.application.result;

import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import com.maplemetric.character.infrastructure.client.dto.CharacterHyperStatResponse;
import java.util.List;
import org.springframework.util.StringUtils;

public record GetCharacterHyperStatResult(
        String date,
        String characterClass,
        Integer appliedPresetNo,
        Long availablePoints,
        List<HyperStatPresetResult> presets
) {

    private static final int MIN_PRESET_NO = 1;
    private static final int MAX_PRESET_NO = 3;

    public static GetCharacterHyperStatResult from(
            CharacterHyperStatResponse response
    ) {
        return new GetCharacterHyperStatResult(
                response.date(),
                response.characterClass(),
                convertPresetNo(response.usePresetNo()),
                response.useAvailableHyperStat(),
                List.of(
                        createPreset(
                                1,
                                response.hyperStatPreset1RemainPoint(),
                                response.hyperStatPreset1()
                        ),
                        createPreset(
                                2,
                                response.hyperStatPreset2RemainPoint(),
                                response.hyperStatPreset2()
                        ),
                        createPreset(
                                3,
                                response.hyperStatPreset3RemainPoint(),
                                response.hyperStatPreset3()
                        )
                )
        );
    }

    private static Integer convertPresetNo(
            String presetNo
    ) {
        if (!StringUtils.hasText(presetNo)) {
            return null;
        }

        try {
            int convertedPresetNo = Integer.parseInt(presetNo);

            if (convertedPresetNo < MIN_PRESET_NO
                    || convertedPresetNo > MAX_PRESET_NO) {
                throw new CharacterException(
                        CharacterErrorCode.NEXON_API_RESPONSE_INVALID
                );
            }

            return convertedPresetNo;
        } catch (NumberFormatException exception) {
            throw new CharacterException(
                    CharacterErrorCode.NEXON_API_RESPONSE_INVALID
            );
        }
    }

    private static HyperStatPresetResult createPreset(
            int presetNo,
            Long remainPoints,
            List<CharacterHyperStatResponse.HyperStat> stats
    ) {
        return new HyperStatPresetResult(
                presetNo,
                remainPoints,
                convertStats(stats)
        );
    }

    private static List<HyperStatResult> convertStats(
            List<CharacterHyperStatResponse.HyperStat> stats
    ) {
        if (stats == null) {
            return List.of();
        }

        return stats.stream()
                .filter(stat -> stat != null)
                .map(stat -> new HyperStatResult(
                        stat.statType(),
                        stat.statPoint(),
                        stat.statLevel(),
                        stat.statIncrease()
                ))
                .toList();
    }

    public record HyperStatPresetResult(
            Integer presetNo,
            Long remainPoints,
            List<HyperStatResult> stats
    ) {
    }

    public record HyperStatResult(
            String statType,
            Long statPoint,
            Integer statLevel,
            String statIncrease
    ) {
    }
}
