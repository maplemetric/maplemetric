package com.maplemetric.notice.infrastructure.client;

import com.maplemetric.notice.infrastructure.client.dto.CashshopNoticeListResponse;
import com.maplemetric.notice.infrastructure.client.dto.NoticeListResponse;
import com.maplemetric.notice.infrastructure.client.dto.UpdateNoticeListResponse;

public interface NoticeClient {

    NoticeListResponse getNotices();

    UpdateNoticeListResponse getUpdateNotices();

    CashshopNoticeListResponse getCashshopNotices();
}
