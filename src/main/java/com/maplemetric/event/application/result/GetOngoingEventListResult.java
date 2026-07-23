package com.maplemetric.event.application.result;

import com.maplemetric.event.domain.exception.EventErrorCode;
import com.maplemetric.event.domain.exception.EventException;
import com.maplemetric.event.infrastructure.client.dto.EventNoticeListResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.util.StringUtils;

public record GetOngoingEventListResult(
        List<Event> events,
        int total,
        String source
) {

    private static final String STATUS =
            "ongoing";

    private static final String SOURCE =
            "NEXON_OPEN_API";

    private static final ZoneId KOREA_ZONE_ID =
            ZoneId.of("Asia/Seoul");

    public static GetOngoingEventListResult from(
            EventNoticeListResponse response
    ) {
        List<Event> events = response.eventNotice()
                .stream()
                .map(item -> new Event(
                        item.noticeId(),
                        item.title(),
                        item.url(),
                        parseDate(item.date()),
                        parseDate(item.dateEventStart()),
                        parseDate(item.dateEventEnd()),
                        STATUS
                ))
                .toList();

        return new GetOngoingEventListResult(
                events,
                events.size(),
                SOURCE
        );
    }

    private static LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value)
                    .atZoneSameInstant(KOREA_ZONE_ID)
                    .toLocalDate();
        } catch (DateTimeParseException exception) {
            throw new EventException(
                    EventErrorCode.NEXON_API_RESPONSE_INVALID
            );
        }
    }

    public record Event(
            Long eventId,
            String title,
            String url,
            LocalDate date,
            LocalDate startDate,
            LocalDate endDate,
            String status
    ) {
    }
}
