package com.maplemetric.event.infrastructure.client;

import com.maplemetric.event.infrastructure.client.dto.EventNoticeListResponse;

public interface EventClient {

    EventNoticeListResponse getOngoingEvents();
}
