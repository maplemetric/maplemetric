package com.maplemetric.character.application.result;

import com.maplemetric.character.infrastructure.client.dto.CharacterLinkSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterVMatrixResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record GetCharacterSkillsResult(
        VMatrixResult vMatrix,
        LinkSkillsResult linkSkills
) {

    public static GetCharacterSkillsResult of(
            CharacterVMatrixResponse vMatrixResponse,
            CharacterSkillResponse fifthSkillResponse,
            CharacterLinkSkillResponse linkSkillResponse
    ) {
        Map<String, CharacterSkillResponse.Skill> fifthSkills =
                CharacterSkillResultMapper.createSkillMap(
                        fifthSkillResponse
                );

        List<LinkPresetResult> presets = List.of(
                createPreset(
                        1,
                        linkSkillResponse.characterLinkSkillPreset1()
                ),
                createPreset(
                        2,
                        linkSkillResponse.characterLinkSkillPreset2()
                ),
                createPreset(
                        3,
                        linkSkillResponse.characterLinkSkillPreset3()
                )
        );

        List<LinkSkillResult> currentSkills =
                convertTransferredLinkSkills(
                        linkSkillResponse.characterLinkSkill()
                );

        return new GetCharacterSkillsResult(
                new VMatrixResult(
                        convertVCores(
                                vMatrixResponse.characterVCoreEquipment(),
                                fifthSkills
                        )
                ),
                new LinkSkillsResult(
                        currentSkills,
                        findMatchedPresetNos(
                                currentSkills,
                                presets
                        ),
                        presets
                )
        );
    }

    private static List<VCoreResult> convertVCores(
            List<CharacterVMatrixResponse.VCore> cores,
            Map<String, CharacterSkillResponse.Skill> fifthSkills
    ) {
        if (cores == null) {
            return List.of();
        }

        return cores.stream()
                .filter(core -> core != null)
                .map(core -> {
                    CharacterSkillResponse.Skill skill =
                            fifthSkills.get(core.vCoreName());

                    List<SkillResult> skills =
                            skill == null
                                    ? List.of()
                                    : List.of(
                                    new SkillResult(
                                            skill.skillName(),
                                            skill.skillIcon()
                                    )
                            );

                    return new VCoreResult(
                            core.vCoreName(),
                            core.vCoreType(),
                            core.vCoreLevel(),
                            skills
                    );
                })
                .toList();
    }

    private static LinkPresetResult createPreset(
            int presetNo,
            List<CharacterLinkSkillResponse.LinkSkill> skills
    ) {
        return new LinkPresetResult(
                presetNo,
                convertTransferredLinkSkills(skills)
        );
    }

    private static List<LinkSkillResult> convertTransferredLinkSkills(
            List<CharacterLinkSkillResponse.LinkSkill> skills
    ) {
        if (skills == null) {
            return List.of();
        }

        return skills.stream()
                .filter(skill -> skill != null)
                .map(skill -> new LinkSkillResult(
                        skill.skillName(),
                        skill.skillLevel(),
                        skill.skillIcon()
                ))
                .toList();
    }

    private static List<Integer> findMatchedPresetNos(
            List<LinkSkillResult> currentSkills,
            List<LinkPresetResult> presets
    ) {
        Map<LinkSkillKey, Long> currentSkillCounts =
                countLinkSkills(currentSkills);

        return presets.stream()
                .filter(preset -> currentSkillCounts.equals(
                        countLinkSkills(preset.skills())
                ))
                .map(preset -> preset.presetNo())
                .toList();
    }

    private static Map<LinkSkillKey, Long> countLinkSkills(
            List<LinkSkillResult> skills
    ) {
        return skills.stream()
                .collect(Collectors.groupingBy(
                        skill -> new LinkSkillKey(
                                skill.skillName(),
                                skill.skillLevel()
                        ),
                        Collectors.counting()
                ));
    }

    private record LinkSkillKey(
            String skillName,
            Integer skillLevel
    ) {
    }

    public record VMatrixResult(
            List<VCoreResult> cores
    ) {
    }

    public record VCoreResult(
            String coreName,
            String coreType,
            Integer coreLevel,
            List<SkillResult> skills
    ) {
    }

    public record SkillResult(
            String skillName,
            String skillIcon
    ) {
    }

    public record LinkSkillsResult(
            List<LinkSkillResult> currentSkills,
            List<Integer> matchedPresetNos,
            List<LinkPresetResult> presets
    ) {
    }

    public record LinkPresetResult(
            Integer presetNo,
            List<LinkSkillResult> skills
    ) {
    }

    public record LinkSkillResult(
            String skillName,
            Integer skillLevel,
            String skillIcon
    ) {
    }
}