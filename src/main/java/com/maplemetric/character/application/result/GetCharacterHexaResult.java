package com.maplemetric.character.application.result;

import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSkillResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.util.StringUtils;

public record GetCharacterHexaResult(
        List<HexaCoreResult> cores,
        List<HexaStatResult> stats
) {

    public static GetCharacterHexaResult of(
            CharacterHexaMatrixResponse matrixResponse,
            CharacterHexaMatrixStatResponse statResponse,
            CharacterSkillResponse sixthSkillResponse
    ) {
        Map<String, CharacterSkillResponse.Skill> sixthSkills =
                createSkillMap(sixthSkillResponse);

        List<HexaStatResult> stats =
                new ArrayList<>();

        stats.addAll(convertStats(
                1,
                statResponse.characterHexaStatCore()
        ));

        stats.addAll(convertStats(
                2,
                statResponse.characterHexaStatCore2()
        ));

        stats.addAll(convertStats(
                3,
                statResponse.characterHexaStatCore3()
        ));

        return new GetCharacterHexaResult(
                convertCores(
                        matrixResponse.characterHexaCoreEquipment(),
                        sixthSkills
                ),
                List.copyOf(stats)
        );
    }

    private static Map<String, CharacterSkillResponse.Skill> createSkillMap(
            CharacterSkillResponse response
    ) {
        if (response == null
                || response.characterSkill() == null) {
            return Map.of();
        }

        return response.characterSkill()
                .stream()
                .filter(skill -> skill != null)
                .filter(skill -> StringUtils.hasText(
                        skill.skillName()
                ))
                .collect(Collectors.toMap(
                        skill -> skill.skillName(),
                        skill -> skill,
                        (firstSkill, ignoredSkill) -> firstSkill,
                        () -> new LinkedHashMap<>()
                ));
    }

    private static List<HexaCoreResult> convertCores(
            List<CharacterHexaMatrixResponse.HexaCore> cores,
            Map<String, CharacterSkillResponse.Skill> sixthSkills
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
                                core.linkedSkill(),
                                sixthSkills
                        )
                ))
                .toList();
    }

    private static List<LinkedSkillResult> convertLinkedSkills(
            List<CharacterHexaMatrixResponse.LinkedSkill> linkedSkills,
            Map<String, CharacterSkillResponse.Skill> sixthSkills
    ) {
        if (linkedSkills == null) {
            return List.of();
        }

        return linkedSkills.stream()
                .filter(linkedSkill -> linkedSkill != null)
                .map(linkedSkill -> {
                    CharacterSkillResponse.Skill skill =
                            sixthSkills.get(
                                    linkedSkill.hexaSkillId()
                            );

                    return new LinkedSkillResult(
                            linkedSkill.hexaSkillId(),
                            skill == null
                                    ? null
                                    : skill.skillIcon()
                    );
                })
                .toList();
    }

    private static List<HexaStatResult> convertStats(
            int statCoreNo,
            List<CharacterHexaMatrixStatResponse.HexaStatCore> stats
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
                        convertSubStats(stat)
                ))
                .toList();
    }

    private static List<SubStatResult> convertSubStats(
            CharacterHexaMatrixStatResponse.HexaStatCore stat
    ) {
        return Stream.of(
                        new SubStatResult(
                                stat.subStatName1(),
                                stat.subStatLevel1()
                        ),
                        new SubStatResult(
                                stat.subStatName2(),
                                stat.subStatLevel2()
                        )
                )
                .filter(subStat -> StringUtils.hasText(
                        subStat.statName()
                ))
                .toList();
    }

    private static Integer convertSlotNo(
            String slotId
    ) {
        if (!StringUtils.hasText(slotId)) {
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
            String skillIcon
    ) {
    }

    public record HexaStatResult(
            Integer statCoreNo,
            Integer slotNo,
            String mainStatName,
            Integer mainStatLevel,
            List<SubStatResult> subStats
    ) {
    }

    public record SubStatResult(
            String statName,
            Integer statLevel
    ) {
    }
}
