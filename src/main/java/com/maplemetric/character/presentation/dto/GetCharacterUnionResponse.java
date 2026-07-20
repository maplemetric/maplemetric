package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterUnionResult;

public record GetCharacterUnionResponse(
        Integer unionLevel,
        Integer unionArtifactLevel
) {

    public static GetCharacterUnionResponse from(
            GetCharacterUnionResult result
    ) {
        return new GetCharacterUnionResponse(
                result.unionLevel(),
                result.unionArtifactLevel()
        );
    }
}
