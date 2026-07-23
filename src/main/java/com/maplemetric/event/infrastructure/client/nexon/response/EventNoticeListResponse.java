package com.maplemetric.event.infrastructure.client.nexon.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EventNoticeListResponse(
        List<EventNotice> eventNotice
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EventNotice(
            Long noticeId,
            String title,
            String url,
            String date,
            String dateEventStart,
            String dateEventEnd
    ) {
    }
}
