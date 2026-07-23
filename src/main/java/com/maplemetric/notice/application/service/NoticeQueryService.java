package com.maplemetric.notice.application.service;

import com.maplemetric.notice.application.result.GetNoticeListResult;
import com.maplemetric.notice.application.port.out.LoadNoticeListPort;
import com.maplemetric.notice.domain.model.NoticeCategory;
import org.springframework.stereotype.Service;

@Service
public class NoticeQueryService {

    private final LoadNoticeListPort loadNoticeListPort;

    public NoticeQueryService(
            LoadNoticeListPort loadNoticeListPort
    ) {
        this.loadNoticeListPort = loadNoticeListPort;
    }

    public GetNoticeListResult getNotices(
            String categoryValue,
            int limit
    ) {
        NoticeCategory category =
                NoticeCategory.from(categoryValue);

        return loadNoticeListPort.loadNotices(
                category,
                limit
        );
    }
}
