package com.maplemetric.character.application.result;

import com.maplemetric.character.application.calculator.HexaStatIncrease;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.CharacterHexa;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.HexaCore;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.HexaStatCore;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.LinkedSkill;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.SixthSkill;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record GetCharacterHexaResult(
        List<HexaCoreResult> cores,
        List<HexaStatResult> stats
) {

    public static GetCharacterHexaResult from(
            CharacterHexa hexa
    ) {
        Map<String, SixthSkill> sixthSkills =
                createSixthSkillMap(
                        hexa.sixthSkills()
                );

        List<HexaStatResult> stats =
                new ArrayList<>();

        stats.addAll(convertStats(
                1,
                hexa.hexaStatCore1()
        ));

        stats.addAll(convertStats(
                2,
                hexa.hexaStatCore2()
        ));

        stats.addAll(convertStats(
                3,
                hexa.hexaStatCore3()
        ));

        return new GetCharacterHexaResult(
                convertCores(
                        hexa.hexaCoreEquipment(),
                        sixthSkills
                ),
                List.copyOf(stats)
        );
    }

    private static Map<String, SixthSkill> createSixthSkillMap(
            List<SixthSkill> sixthSkills
    ) {
        if (sixthSkills == null) {
            return Map.of();
        }

        return sixthSkills.stream()
                .filter(skill -> skill != null)
                .filter(skill -> skill.skillName() != null
                        && !skill.skillName().isBlank())
                .collect(Collectors.toMap(
                        skill -> skill.skillName(),
                        skill -> skill,
                        (firstSkill, ignoredSkill) -> firstSkill,
                        () -> new LinkedHashMap<>()
                ));
    }

    private static List<HexaCoreResult> convertCores(
            List<HexaCore> cores,
            Map<String, SixthSkill> sixthSkills
    ) {
        if (cores == null) {
            return List.of();
        }

        return cores.stream()
                .filter(core -> core != null)
                .map(core -> new HexaCoreResult(
                        core.hexaCoreName(),
                        core.hexaCoreType(),
                        core.hexaCoreLevel(),
                        convertLinkedSkills(
                                core.linkedSkills(),
                                sixthSkills
                        )
                ))
                .toList();
    }

    private static List<LinkedSkillResult> convertLinkedSkills(
            List<LinkedSkill> linkedSkills,
            Map<String, SixthSkill> sixthSkills
    ) {
        if (linkedSkills == null) {
            return List.of();
        }

        return linkedSkills.stream()
                .filter(linkedSkill -> linkedSkill != null)
                .map(linkedSkill -> {
                    SixthSkill skill =
                            sixthSkills.get(
                                    linkedSkill.hexaSkillId()
                            );

                    return new LinkedSkillResult(
                            linkedSkill.hexaSkillId(),
                            skill == null ? null : skill.skillIcon(),
                            skill == null ? null : skill.skillDescription(),
                            skill == null ? null : skill.skillEffect(),
                            skill == null ? null : skill.skillEffectNext()
                    );
                })
                .toList();
    }

    private static List<HexaStatResult> convertStats(
            int statCoreNo,
            List<HexaStatCore> stats
    ) {
        if (stats == null) {
            return List.of();
        }

        return stats.stream()
                .filter(stat -> stat != null)
                .map(stat -> new HexaStatResult(
                        statCoreNo,
                        convertSlotNo(stat.slotId()),
                        stat.mainStatName(),
                        stat.mainStatLevel(),
                        HexaStatIncrease.ofMain(
                                stat.mainStatName(),
                                stat.mainStatLevel()
                        ),
                        convertSubStats(stat)
                ))
                .toList();
    }

    private static List<SubStatResult> convertSubStats(
            HexaStatCore stat
    ) {
        return Stream.of(
                        toSubStat(
                                stat.subStatName1(),
                                stat.subStatLevel1()
                        ),
                        toSubStat(
                                stat.subStatName2(),
                                stat.subStatLevel2()
                        )
                )
                .filter(subStat -> subStat.statName() != null
                        && !subStat.statName().isBlank())
                .toList();
    }

    private static SubStatResult toSubStat(
            String statName,
            Integer statLevel
    ) {
        return new SubStatResult(
                statName,
                statLevel,
                HexaStatIncrease.ofSub(statName, statLevel)
        );
    }

    private static Integer convertSlotNo(
            String slotId
    ) {
        if (slotId == null || slotId.isBlank()) {
            throw new CharacterException(
                    CharacterErrorCode.NEXON_API_RESPONSE_INVALID
            );
        }

        try {
            int slotIdValue =
                    Integer.parseInt(slotId);

            if (slotIdValue < 0
                    || slotIdValue == Integer.MAX_VALUE) {
                throw new CharacterException(
                        CharacterErrorCode.NEXON_API_RESPONSE_INVALID
                );
            }

            return slotIdValue + 1;

        } catch (NumberFormatException exception) {
            throw new CharacterException(
                    CharacterErrorCode.NEXON_API_RESPONSE_INVALID
            );
        }
    }

    public record HexaCoreResult(
            String coreName,
            String coreType,
            Integer coreLevel,
            List<LinkedSkillResult> linkedSkills
    ) {
    }

    public record LinkedSkillResult(
            String skillName,
            String skillIcon,
            String skillDescription,
            String skillEffect,
            String skillEffectNext
    ) {
    }

    public record HexaStatResult(
            Integer statCoreNo,
            Integer slotNo,
            String mainStatName,
            Integer mainStatLevel,
            BigDecimal mainStatIncrease,
            List<SubStatResult> subStats
    ) {
    }

    public record SubStatResult(
            String statName,
            Integer statLevel,
            BigDecimal statIncrease
    ) {
    }
}