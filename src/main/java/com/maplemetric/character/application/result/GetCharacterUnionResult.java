package com.maplemetric.character.application.result;

import com.maplemetric.character.application.port.out.LoadCharacterUnionPort.CharacterUnion;

public record GetCharacterUnionResult(
        Integer unionLevel,
        Integer unionArtifactLevel
) {

    public static GetCharacterUnionResult from(
            CharacterUnion union
    ) {
        return new GetCharacterUnionResult(
                union.unionLevel(),
                union.unionArtifactLevel()
        );
    }
}
