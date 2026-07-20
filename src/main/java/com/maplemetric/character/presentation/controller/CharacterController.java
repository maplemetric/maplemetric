package com.maplemetric.character.presentation.controller;

import com.maplemetric.character.application.result.GetCharacterBasicResult;
import com.maplemetric.character.application.result.GetCharacterEquipmentResult;
import com.maplemetric.character.application.result.GetCharacterResult;
import com.maplemetric.character.application.service.CharacterQueryService;
import com.maplemetric.character.presentation.code.CharacterSuccessCode;
import com.maplemetric.character.presentation.dto.GetCharacterBasicResponse;
import com.maplemetric.character.presentation.dto.GetCharacterEquipmentResponse;
import com.maplemetric.character.presentation.dto.GetCharacterResponse;
import com.maplemetric.global.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/characters")
public class CharacterController {

    private final CharacterQueryService characterQueryService;

    public CharacterController(
            CharacterQueryService characterQueryService
    ) {
        this.characterQueryService = characterQueryService;
    }

    @GetMapping
    public ApiResponse<GetCharacterResponse> getCharacter(@RequestParam String characterName) {
        GetCharacterResult result =
                characterQueryService.getCharacter(characterName);

        return ApiResponse.ok(
                CharacterSuccessCode.CHARACTER_READ_SUCCESS, GetCharacterResponse.from(result));
    }

    @GetMapping("/basic")
    public ApiResponse<GetCharacterBasicResponse> getCharacterBasic(@RequestParam String characterName) {
        GetCharacterBasicResult result = characterQueryService.getCharacterBasic(characterName);

        return ApiResponse.ok(CharacterSuccessCode.GET_CHARACTER_BASIC_SUCCESS, GetCharacterBasicResponse.from(result));
    }

    @GetMapping("/equipment")
    public ApiResponse<GetCharacterEquipmentResponse> getCharacterEquipment(@RequestParam String characterName) {
        GetCharacterEquipmentResult result = characterQueryService.getCharacterEquipment(characterName);

        return ApiResponse.ok(
                CharacterSuccessCode.GET_CHARACTER_EQUIPMENT_SUCCESS,
                GetCharacterEquipmentResponse.from(result)
        );
    }
}