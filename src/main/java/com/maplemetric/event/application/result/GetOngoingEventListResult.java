package com.maplemetric.event.application.result;

import java.time.LocalDate;
import java.util.List;

public record GetOngoingEventListResult(
        List<Event> events,
        int total,
        String source
) {

    private static final String SOURCE =
            "NEXON_OPEN_API";

    public static GetOngoingEventListResult from(
            List<Event> events
    ) {
        List<Event> copiedEvents = List.copyOf(events);

        return new GetOngoingEventListResult(
                copiedEvents,
                copiedEvents.size(),
                SOURCE
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

        private static final String ONGOING_STATUS =
                "ongoing";

        public static Event ongoing(
                Long eventId,
                String title,
                String url,
                LocalDate date,
                LocalDate startDate,
                LocalDate endDate
        ) {
            return new Event(
                    eventId,
                    title,
                    url,
                    date,
                    startDate,
                    endDate,
                    ONGOING_STATUS
            );
        }
    }
}
