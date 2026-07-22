package com.maplemetric.ranking.domain.exception;

import com.maplemetric.global.BusinessException;
import com.maplemetric.global.ErrorCode;

public class RankingException extends BusinessException {

    public RankingException(ErrorCode errorCode) {
        super(errorCode);
    }
}
