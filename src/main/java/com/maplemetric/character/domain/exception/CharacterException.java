package com.maplemetric.character.domain.exception;

import com.maplemetric.common.BusinessException;
import com.maplemetric.common.ErrorCode;

public class CharacterException extends BusinessException {

    public CharacterException(ErrorCode errorCode) {
        super(errorCode);
    }
}
