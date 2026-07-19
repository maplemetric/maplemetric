package com.maplemetric.character.infrastructure.client;

import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import com.maplemetric.character.infrastructure.client.dto.OcidResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
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
        try {
            OcidResponse response = nexonRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(CHARACTER_OCID_PATH)
                            .queryParam("character_name", characterName)
                            .build()
                    )
                    .retrieve()
                    .body(OcidResponse.class);

            if (response == null || response.ocid() == null) {
                throw new CharacterException(
                        CharacterErrorCode.CHARACTER_NOT_FOUND
                );
            }

            return response.ocid();

        } catch (HttpClientErrorException.NotFound exception) {
            throw new CharacterException(
                    CharacterErrorCode.CHARACTER_NOT_FOUND
            );

        } catch (HttpServerErrorException exception) {
            throw new CharacterException(
                    CharacterErrorCode.CHARACTER_API_ERROR
            );
        }
    }
}