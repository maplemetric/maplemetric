package com.maplemetric.event.application.port.out;

import com.maplemetric.event.application.result.GetOngoingEventListResult;

public interface LoadOngoingEventsPort {

    GetOngoingEventListResult loadOngoingEvents();
}
