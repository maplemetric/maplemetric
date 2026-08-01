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

    /**
     * 캐릭터 종합 정보를 조회한다.
     *
     * 기본은 저장된 조회 결과를 돌려준다. {@code refresh=true}면 Nexon에서 다시
     * 수집한다. 응답의 {@code dataUpdatedAt}이 그 데이터를 가져온 시각이다.
     */
    @GetMapping("/search")
    public ApiResponse<GetCharacterSummaryResponse> searchCharacter(
            @RequestParam @NotBlank(message = "캐릭터명은 필수입니다.") String characterName,
            @RequestParam(defaultValue = "false") boolean refresh
    ) {
        GetCharacterSummaryResult result =
                characterQueryService.getCharacterSummary(
                        characterName,
                        refresh
                );

        return ApiResponse.ok(
                CharacterSuccessCode.CHARACTER_SEARCH_SUCCESS,
                GetCharacterSummaryResponse.from(result)
        );
    }
}