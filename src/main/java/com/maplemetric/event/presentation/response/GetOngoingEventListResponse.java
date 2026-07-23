package com.maplemetric.event.presentation.response;

import com.maplemetric.event.application.result.GetOngoingEventListResult;
import java.time.LocalDate;
import java.util.List;

public record GetOngoingEventListResponse(
        List<Event> events,
        int total,
        String source
) {

    public static GetOngoingEventListResponse from(
            GetOngoingEventListResult result
    ) {
        List<Event> events = result.events()
                .stream()
                .map(item -> new Event(
                        item.eventId(),
                        item.title(),
                        item.url(),
                        item.date(),
                        item.startDate(),
                        item.endDate(),
                        item.status()
                ))
                .toList();

        return new GetOngoingEventListResponse(
                events,
                result.total(),
                result.source()
        );
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
