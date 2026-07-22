package com.maplemetric.character.presentation.controller;

import com.maplemetric.character.application.result.GetCharacterSummaryResult;
import com.maplemetric.character.application.service.CharacterQueryService;
import com.maplemetric.character.presentation.code.CharacterSuccessCode;
import com.maplemetric.character.presentation.dto.GetCharacterSummaryResponse;
import com.maplemetric.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/characters")
public class CharacterController {

    private final CharacterQueryService characterQueryService;

    public CharacterController(CharacterQueryService characterQueryService) {
        this.characterQueryService = characterQueryService;
    }

    @GetMapping("/search")
    public ApiResponse<GetCharacterSummaryResponse> searchCharacter(@RequestParam @NotBlank(message = "캐릭터명은 필수입니다.") String characterName) {
        GetCharacterSummaryResult result =
                characterQueryService.getCharacterSummary(characterName);

        return ApiResponse.ok(
                CharacterSuccessCode.CHARACTER_SEARCH_SUCCESS,
                GetCharacterSummaryResponse.from(result)
        );
    }
}