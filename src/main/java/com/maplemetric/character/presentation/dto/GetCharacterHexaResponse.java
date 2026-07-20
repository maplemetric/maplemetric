package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterHexaResult;
import java.util.List;

public record GetCharacterHexaResponse(
        List<HexaCoreResponse> cores,
        List<HexaStatResponse> stats
) {

    public static GetCharacterHexaResponse from(
            GetCharacterHexaResult result
    ) {
        return new GetCharacterHexaResponse(
                result.cores()
                        .stream()
                        .map(core -> HexaCoreResponse.from(core))
                        .toList(),
                result.stats()
                        .stream()
                        .map(stat -> HexaStatResponse.from(stat))
                        .toList()
        );
    }

    public record HexaCoreResponse(
            String coreName,
            String coreType,
            Integer coreLevel,
            List<LinkedSkillResponse> linkedSkills
    ) {

        public static HexaCoreResponse from(
                GetCharacterHexaResult.HexaCoreResult result
        ) {
            return new HexaCoreResponse(
                    result.coreName(),
                    result.coreType(),
                    result.coreLevel(),
                    result.linkedSkills()
                            .stream()
                            .map(skill -> LinkedSkillResponse.from(skill))
                            .toList()
            );
        }
    }

    public record LinkedSkillResponse(
            String skillName,
            String skillIcon
    ) {

        public static LinkedSkillResponse from(
                GetCharacterHexaResult.LinkedSkillResult result
        ) {
            return new LinkedSkillResponse(
                    result.skillName(),
                    result.skillIcon()
            );
        }
    }

    public record HexaStatResponse(
            Integer statCoreNo,
            Integer slotNo,
            String mainStatName,
            Integer mainStatLevel,
            List<SubStatResponse> subStats
    ) {

        public static HexaStatResponse from(
                GetCharacterHexaResult.HexaStatResult result
        ) {
            return new HexaStatResponse(
                    result.statCoreNo(),
                    result.slotNo(),
                    result.mainStatName(),
                    result.mainStatLevel(),
                    result.subStats()
                            .stream()
                            .map(subStat -> SubStatResponse.from(subStat))
                            .toList()
            );
        }
    }

    public record SubStatResponse(
            String statName,
            Integer statLevel
    ) {

        public static SubStatResponse from(
                GetCharacterHexaResult.SubStatResult result
        ) {
            return new SubStatResponse(
                    result.statName(),
                    result.statLevel()
            );
        }
    }
}
