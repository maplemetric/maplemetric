package com.maplemetric.character.application.result;

import com.maplemetric.character.application.port.out.LoadCharacterHyperStatPort.CharacterHyperStat;
import com.maplemetric.character.application.port.out.LoadCharacterHyperStatPort.HyperStat;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
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
            CharacterHyperStat hyperStat
    ) {
        return new GetCharacterHyperStatResult(
                hyperStat.date(),
                hyperStat.characterClass(),
                convertPresetNo(hyperStat.appliedPresetNo()),
                hyperStat.availablePoints(),
                List.of(
                        createPreset(
                                1,
                                hyperStat.preset1RemainPoints(),
                                hyperStat.preset1()
                        ),
                        createPreset(
                                2,
                                hyperStat.preset2RemainPoints(),
                                hyperStat.preset2()
                        ),
                        createPreset(
                                3,
                                hyperStat.preset3RemainPoints(),
                                hyperStat.preset3()
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
            List<HyperStat> stats
    ) {
        return new HyperStatPresetResult(
                presetNo,
                remainPoints,
                convertStats(stats)
        );
    }

    private static List<HyperStatResult> convertStats(
            List<HyperStat> stats
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
