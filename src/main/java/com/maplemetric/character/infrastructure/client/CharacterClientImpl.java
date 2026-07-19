package com.maplemetric.character.infrastructure.client;

import com.maplemetric.character.infrastructure.client.dto.OcidResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CharacterClientImpl implements CharacterClient {

    private static final String CHARACTER_OCID_PATH = "/maplestory/v1/id";

    private final RestClient nexonRestClient;

    public CharacterClientImpl(
            @Qualifier("nexonRestClient") RestClient nexonRestClient
    ) {
        this.nexonRestClient = nexonRestClient;
    }

    @Override
    public String getOcid(String characterName) {
        OcidResponse response = nexonRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(CHARACTER_OCID_PATH)
                        .queryParam("character_name", characterName)
                        .build()
                )
                .retrieve()
                .body(OcidResponse.class);

        if (response == null || response.ocid() == null) {
            throw new IllegalStateException(
                    "넥슨 API에서 캐릭터 OCID를 조회하지 못했습니다."
            );
        }

        return response.ocid();
    }
}