package com.maplemetric.character.infrastructure.client;

import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import com.maplemetric.character.infrastructure.client.dto.CharacterBasicResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterEquipmentResponse;
import com.maplemetric.character.infrastructure.client.dto.OcidResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CharacterClientImpl implements CharacterClient {

    private static final String CHARACTER_OCID_PATH = "/maplestory/v1/id";
    private static final String CHARACTER_BASIC_PATH = "/maplestory/v1/character/basic";
    private static final String CHARACTER_EQUIPMENT_PATH = "/maplestory/v1/character/item-equipment";

    private final RestClient restClient;

    public CharacterClientImpl(
            @Qualifier("nexonRestClient") RestClient nexonRestClient
    ) {
        this.restClient = nexonRestClient;
    }

    @Override
    public String getOcid(String characterName) {
        try {
            OcidResponse response = restClient.get()
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
            throw new CharacterException(CharacterErrorCode.CHARACTER_NOT_FOUND);
        } catch (HttpClientErrorException.BadRequest exception) {
            throw new CharacterException(CharacterErrorCode.CHARACTER_API_ERROR);
        } catch (RestClientException exception) {
            throw new CharacterException(CharacterErrorCode.CHARACTER_API_ERROR);
        }
    }

    @Override
    public CharacterBasicResponse getCharacterBasic(String ocid) {
        try {
            CharacterBasicResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(CHARACTER_BASIC_PATH)
                            .queryParam("ocid", ocid)
                            .build()
                    )
                    .retrieve()
                    .body(CharacterBasicResponse.class);

            if (response == null) {
                throw new CharacterException(
                        CharacterErrorCode.CHARACTER_API_ERROR
                );
            }

            return response;

        } catch (HttpClientErrorException.NotFound exception) {
            throw new CharacterException(CharacterErrorCode.CHARACTER_NOT_FOUND);
        } catch (HttpClientErrorException.BadRequest exception) {
            throw new CharacterException(CharacterErrorCode.CHARACTER_API_ERROR);
        } catch (RestClientException exception) {
            throw new CharacterException(CharacterErrorCode.CHARACTER_API_ERROR);
        }
    }

    @Override
    public CharacterEquipmentResponse getCharacterEquipment(String ocid) {
        try {
            CharacterEquipmentResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(CHARACTER_EQUIPMENT_PATH)
                            .queryParam("ocid", ocid)
                            .build()
                    )
                    .retrieve()
                    .body(CharacterEquipmentResponse.class);

            if (response == null) {
                throw new CharacterException(CharacterErrorCode.CHARACTER_API_ERROR);
            }

            return response;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new CharacterException(CharacterErrorCode.CHARACTER_NOT_FOUND);
        } catch (HttpClientErrorException.BadRequest exception) {
            throw new CharacterException(CharacterErrorCode.CHARACTER_API_ERROR);
        } catch (RestClientException exception) {
            throw new CharacterException(CharacterErrorCode.CHARACTER_API_ERROR);
        }

    }
}