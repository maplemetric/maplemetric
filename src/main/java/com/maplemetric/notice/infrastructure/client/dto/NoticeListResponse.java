package com.maplemetric.notice.infrastructure.client.dto;

import java.util.List;

public record NoticeListResponse(
        List<NoticeItemResponse> notice
) {
}
