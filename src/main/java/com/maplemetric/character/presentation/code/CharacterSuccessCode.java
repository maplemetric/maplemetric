package com.maplemetric.character.presentation.code;

import com.maplemetric.global.SuccessCode;
import lombok.Getter;

@Getter
public enum CharacterSuccessCode implements SuccessCode {

    CHARACTER_READ_SUCCESS("CHARACTER_READ_SUCCESS", "캐릭터 조회에 성공했습니다.");

    private final String code;
    private final String message;


    CharacterSuccessCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}