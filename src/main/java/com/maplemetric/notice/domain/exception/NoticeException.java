package com.maplemetric.notice.domain.exception;

import com.maplemetric.common.BusinessException;

public class NoticeException extends BusinessException {

    public NoticeException(NoticeErrorCode errorCode) {
        super(errorCode);
    }
}
