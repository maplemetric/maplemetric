package com.maplemetric.notice.infrastructure.client.nexon.response;

import java.util.List;

public record NoticeListResponse(
        List<NoticeItemResponse> notice
) {
}
