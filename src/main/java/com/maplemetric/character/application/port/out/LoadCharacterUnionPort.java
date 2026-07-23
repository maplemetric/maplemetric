package com.maplemetric.character.application.port.out;

public interface LoadCharacterUnionPort {

    CharacterUnion loadCharacterUnion(String ocid);

    record CharacterUnion(
            Integer unionLevel,
            Integer unionArtifactLevel
    ) {
    }
}
