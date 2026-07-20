package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterSkillsResult;
import java.util.List;

public record GetCharacterSkillsResponse(
        VMatrixResponse vMatrix,
        LinkSkillsResponse linkSkills
) {

    public static GetCharacterSkillsResponse from(
            GetCharacterSkillsResult result
    ) {
        return new GetCharacterSkillsResponse(
                VMatrixResponse.from(result.vMatrix()),
                LinkSkillsResponse.from(result.linkSkills())
        );
    }

    public record VMatrixResponse(
            List<VCoreResponse> cores
    ) {

        public static VMatrixResponse from(
                GetCharacterSkillsResult.VMatrixResult result
        ) {
            return new VMatrixResponse(
                    result.cores()
                            .stream()
                            .map(core -> VCoreResponse.from(core))
                            .toList()
            );
        }
    }

    public record VCoreResponse(
            String coreName,
            String coreType,
            Integer coreLevel,
            List<SkillResponse> skills
    ) {

        public static VCoreResponse from(
                GetCharacterSkillsResult.VCoreResult result
        ) {
            return new VCoreResponse(
                    result.coreName(),
                    result.coreType(),
                    result.coreLevel(),
                    result.skills()
                            .stream()
                            .map(skill -> SkillResponse.from(skill))
                            .toList()
            );
        }
    }

    public record SkillResponse(
            String skillName,
            String skillIcon
    ) {

        public static SkillResponse from(
                GetCharacterSkillsResult.SkillResult result
        ) {
            return new SkillResponse(
                    result.skillName(),
                    result.skillIcon()
            );
        }
    }

    public record LinkSkillsResponse(
            List<LinkSkillResponse> currentSkills,
            List<Integer> matchedPresetNos,
            List<LinkPresetResponse> presets
    ) {

        public static LinkSkillsResponse from(
                GetCharacterSkillsResult.LinkSkillsResult result
        ) {
            return new LinkSkillsResponse(
                    result.currentSkills()
                            .stream()
                            .map(skill -> LinkSkillResponse.from(skill))
                            .toList(),
                    result.matchedPresetNos(),
                    result.presets()
                            .stream()
                            .map(preset -> LinkPresetResponse.from(preset))
                            .toList()
            );
        }
    }

    public record LinkPresetResponse(
            Integer presetNo,
            List<LinkSkillResponse> skills
    ) {

        public static LinkPresetResponse from(
                GetCharacterSkillsResult.LinkPresetResult result
        ) {
            return new LinkPresetResponse(
                    result.presetNo(),
                    result.skills()
                            .stream()
                            .map(skill -> LinkSkillResponse.from(skill))
                            .toList()
            );
        }
    }

    public record LinkSkillResponse(
            String skillName,
            Integer skillLevel,
            String skillIcon
    ) {

        public static LinkSkillResponse from(
                GetCharacterSkillsResult.LinkSkillResult result
        ) {
            return new LinkSkillResponse(
                    result.skillName(),
                    result.skillLevel(),
                    result.skillIcon()
            );
        }
    }
}
