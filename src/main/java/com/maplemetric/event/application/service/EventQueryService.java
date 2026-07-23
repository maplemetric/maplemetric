package com.maplemetric.event.application.service;

import com.maplemetric.event.application.port.out.LoadOngoingEventsPort;
import com.maplemetric.event.application.result.GetOngoingEventListResult;
import org.springframework.stereotype.Service;

@Service
public class EventQueryService {

    private final LoadOngoingEventsPort loadOngoingEventsPort;

    public EventQueryService(
            LoadOngoingEventsPort loadOngoingEventsPort
    ) {
        this.loadOngoingEventsPort = loadOngoingEventsPort;
    }

    public GetOngoingEventListResult getOngoingEvents() {
        return loadOngoingEventsPort.loadOngoingEvents();
    }
}
