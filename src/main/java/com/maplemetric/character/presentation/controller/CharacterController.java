package com.maplemetric.character.presentation.controller;

import com.maplemetric.character.application.result.GetCharacterResult;
import com.maplemetric.character.application.service.CharacterQueryService;
import com.maplemetric.character.presentation.code.CharacterSuccessCode;
import com.maplemetric.character.presentation.dto.GetCharacterResponse;
import com.maplemetric.global.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/characters")
public class CharacterController {

    private final CharacterQueryService characterQueryService;

    public CharacterController(CharacterQueryService characterQueryService) {
        this.characterQueryService = characterQueryService;
    }


    @GetMapping("/{characterName}")
    public ApiResponse<GetCharacterResponse> getCharacter(
            @PathVariable String characterName
    ) {
        GetCharacterResult result =
                characterQueryService.getCharacter(characterName);

        return ApiResponse.ok(CharacterSuccessCode.CHARACTER_READ_SUCCESS, GetCharacterResponse.from(result));
    }
}