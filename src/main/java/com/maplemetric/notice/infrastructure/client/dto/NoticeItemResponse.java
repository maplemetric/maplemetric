package com.maplemetric.notice.infrastructure.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record NoticeItemResponse(
        Long noticeId,
        String title,
        String url,
        String date
) {
}
