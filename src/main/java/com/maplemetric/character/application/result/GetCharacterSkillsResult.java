package com.maplemetric.character.application.result;

import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort.CharacterSkills;
import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort.FifthSkill;
import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort.LinkSkill;
import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort.VCore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record GetCharacterSkillsResult(
        VMatrixResult vMatrix,
        LinkSkillsResult linkSkills
) {

    public static GetCharacterSkillsResult from(
            CharacterSkills skills
    ) {
        Map<String, FifthSkill> fifthSkills =
                createFifthSkillMap(
                        skills.fifthSkills()
                );

        List<LinkPresetResult> presets = List.of(
                createPreset(
                        1,
                        skills.linkSkillPreset1()
                ),
                createPreset(
                        2,
                        skills.linkSkillPreset2()
                ),
                createPreset(
                        3,
                        skills.linkSkillPreset3()
                )
        );

        List<LinkSkillResult> currentSkills =
                convertTransferredLinkSkills(
                        skills.currentLinkSkills()
                );

        return new GetCharacterSkillsResult(
                new VMatrixResult(
                        convertVCores(
                                skills.vCores(),
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

    private static Map<String, FifthSkill> createFifthSkillMap(
            List<FifthSkill> fifthSkills
    ) {
        if (fifthSkills == null) {
            return Map.of();
        }

        return fifthSkills.stream()
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

    private static List<VCoreResult> convertVCores(
            List<VCore> cores,
            Map<String, FifthSkill> fifthSkills
    ) {
        if (cores == null) {
            return List.of();
        }

        // Nexon은 V매트릭스 슬롯을 항상 고정 개수로 내려주고 사용하지 않는 칸을
        // 이름·종류가 null인 빈 값으로 채운다. 그대로 두면 이름도 아이콘도 없는
        // 코어가 응답에 실려 소비 측이 빈 항목을 그리게 되므로 여기서 제외한다.
        // 이름이 있으면 스킬이 비어 있어도 실제 장착 코어이므로 남긴다.
        return cores.stream()
                .filter(core -> core != null)
                .filter(core -> core.vCoreName() != null
                        && !core.vCoreName().isBlank())
                .map(core -> {
                    FifthSkill skill =
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
            List<LinkSkill> skills
    ) {
        return new LinkPresetResult(
                presetNo,
                convertTransferredLinkSkills(skills)
        );
    }

    private static List<LinkSkillResult> convertTransferredLinkSkills(
            List<LinkSkill> skills
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