package com.maplemetric.character.infrastructure.client.nexon;

import com.maplemetric.character.application.port.out.LoadCharacterUnionPort;
import com.maplemetric.character.application.port.out.LoadCharacterUnionPort.CharacterUnion;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterUnionResponse;
import org.springframework.stereotype.Component;

@Component
class NexonCharacterUnionAdapter
        implements LoadCharacterUnionPort {

    private final CharacterClient characterClient;

    NexonCharacterUnionAdapter(
            CharacterClient characterClient
    ) {
        this.characterClient = characterClient;
    }

    @Override
    public CharacterUnion loadCharacterUnion(
            String ocid
    ) {
        CharacterUnionResponse response =
                characterClient.getCharacterUnion(ocid);

        return new CharacterUnion(
                response.unionLevel(),
                response.unionArtifactLevel()
        );
    }
}
