package com.maplemetric.character.domain.exception;

import com.maplemetric.global.BusinessException;
import com.maplemetric.global.ErrorCode;

public class CharacterException extends BusinessException {

    public CharacterException(ErrorCode errorCode) {
        super(errorCode);
    }
}
