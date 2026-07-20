package com.maplemetric.character.presentation.code;

import com.maplemetric.global.SuccessCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CharacterSuccessCode implements SuccessCode {

//    CHARACTER_READ_SUCCESS("CHARACTER_READ_SUCCESS", "캐릭터 조회에 성공했습니다."),
    GET_CHARACTER_BASIC_SUCCESS("CHARACTER_BASIC_200", "캐릭터 기본 정보 조회에 성공했습니다."),
    GET_CHARACTER_EQUIPMENT_SUCCESS("CHARACTER_2004", "캐릭터 장비 정보 조회에 성공했습니다."),
    GET_CHARACTER_SUMMARY_SUCCESS("CHARACTER_2005", "캐릭터 통합 정보 조회에 성공했습니다.");

    private final String code;
    private final String message;


    CharacterSuccessCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}