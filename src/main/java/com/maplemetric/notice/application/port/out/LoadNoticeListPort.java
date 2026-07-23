package com.maplemetric.notice.application.port.out;

import com.maplemetric.notice.application.result.GetNoticeListResult;
import com.maplemetric.notice.domain.model.NoticeCategory;

public interface LoadNoticeListPort {

    GetNoticeListResult loadNotices(
            NoticeCategory category,
            int limit
    );
}
