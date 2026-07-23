package com.maplemetric.ranking.domain.exception;

import com.maplemetric.common.BusinessException;
import com.maplemetric.common.nexon.NexonApiFailure;
import com.maplemetric.ranking.presentation.code.RankingErrorCode;

public class RankingException extends BusinessException {

    private final RankingErrorCode errorCode;

    public RankingException(RankingErrorCode errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public NexonApiFailure getFailure() {
        return errorCode.getFailure();
    }
}
