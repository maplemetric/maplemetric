package com.maplemetric.character.application.result;

import com.maplemetric.character.infrastructure.client.dto.CharacterUnionResponse;

public record GetCharacterUnionResult(
        Integer unionLevel,
        Integer unionArtifactLevel
) {

    public static GetCharacterUnionResult from(
            CharacterUnionResponse response
    ) {
        return new GetCharacterUnionResult(
                response.unionLevel(),
                response.unionArtifactLevel()
        );
    }
}
