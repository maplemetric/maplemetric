package com.maplemetric.ranking.domain.exception;

import com.maplemetric.common.BusinessException;
import com.maplemetric.common.ErrorCode;

public class RankingException extends BusinessException {

    public RankingException(ErrorCode errorCode) {
        super(errorCode);
    }
}
