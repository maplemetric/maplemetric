package com.maplemetric.notice.application.service;

import com.maplemetric.notice.application.result.GetNoticeListResult;
import com.maplemetric.notice.domain.NoticeCategory;
import com.maplemetric.notice.infrastructure.client.NoticeClient;
import org.springframework.stereotype.Service;

@Service
public class NoticeQueryService {

    private final NoticeClient noticeClient;

    public NoticeQueryService(NoticeClient noticeClient) {
        this.noticeClient = noticeClient;
    }

    public GetNoticeListResult getNotices(
            String categoryValue,
            int limit
    ) {
        NoticeCategory category =
                NoticeCategory.from(categoryValue);

        return switch (category) {
            case GENERAL -> GetNoticeListResult.from(
                    noticeClient.getNotices(),
                    limit
            );
            case UPDATE -> GetNoticeListResult.from(
                    noticeClient.getUpdateNotices(),
                    limit
            );
            case CASHSHOP -> GetNoticeListResult.from(
                    noticeClient.getCashshopNotices(),
                    limit
            );
        };
    }
}
