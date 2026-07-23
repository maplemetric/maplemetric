package com.maplemetric.event.infrastructure.client.nexon;

import com.maplemetric.event.application.exception.EventException;
import com.maplemetric.event.application.exception.EventFailure;
import com.maplemetric.event.application.port.out.LoadOngoingEventsPort;
import com.maplemetric.event.application.result.GetOngoingEventListResult;
import com.maplemetric.event.infrastructure.client.nexon.response.EventNoticeListResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class NexonEventAdapter
        implements LoadOngoingEventsPort {

    private static final ZoneId KOREA_ZONE_ID =
            ZoneId.of("Asia/Seoul");

    private final NexonEventClient nexonEventClient;

    NexonEventAdapter(
            NexonEventClient nexonEventClient
    ) {
        this.nexonEventClient = nexonEventClient;
    }

    @Override
    public GetOngoingEventListResult loadOngoingEvents() {
        EventNoticeListResponse response =
                nexonEventClient.getOngoingEvents();

        List<GetOngoingEventListResult.Event> events =
                response.eventNotice()
                        .stream()
                        .map(item ->
                                GetOngoingEventListResult.Event
                                        .ongoing(
                                                item.noticeId(),
                                                item.title(),
                                                item.url(),
                                                parseDate(
                                                        item.date()
                                                ),
                                                parseDate(
                                                        item.dateEventStart()
                                                ),
                                                parseDate(
                                                        item.dateEventEnd()
                                                )
                                        ))
                        .toList();

        return GetOngoingEventListResult.from(events);
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value)
                    .atZoneSameInstant(KOREA_ZONE_ID)
                    .toLocalDate();
        } catch (DateTimeParseException exception) {
            throw new EventException(
                    EventFailure.RESPONSE_INVALID
            );
        }
    }
}
