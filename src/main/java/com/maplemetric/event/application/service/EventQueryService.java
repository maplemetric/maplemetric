package com.maplemetric.event.application.service;

import com.maplemetric.event.application.result.GetOngoingEventListResult;
import com.maplemetric.event.infrastructure.client.EventClient;
import com.maplemetric.event.infrastructure.client.dto.EventNoticeListResponse;
import org.springframework.stereotype.Service;

@Service
public class EventQueryService {

    private final EventClient eventClient;

    public EventQueryService(EventClient eventClient) {
        this.eventClient = eventClient;
    }

    public GetOngoingEventListResult getOngoingEvents() {
        EventNoticeListResponse response =
                eventClient.getOngoingEvents();

        return GetOngoingEventListResult.from(response);
    }
}
