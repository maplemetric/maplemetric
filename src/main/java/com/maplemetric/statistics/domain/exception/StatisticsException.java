package com.maplemetric.statistics.domain.exception;

import com.maplemetric.common.BusinessException;
import com.maplemetric.common.ErrorCode;

public class StatisticsException extends BusinessException {

    public StatisticsException(ErrorCode errorCode) {
        super(errorCode);
    }
}
