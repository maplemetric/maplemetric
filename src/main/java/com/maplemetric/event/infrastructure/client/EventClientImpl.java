package com.maplemetric.event.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.common.nexon.NexonApiFailure;
import com.maplemetric.common.nexon.NexonApiRequester;
import com.maplemetric.event.domain.exception.EventErrorCode;
import com.maplemetric.event.domain.exception.EventException;
import com.maplemetric.event.infrastructure.client.dto.EventNoticeListResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class EventClientImpl implements EventClient {

    private static final String EVENT_NOTICE_PATH =
            "/maplestory/v1/notice-event";

    private static final String EVENT_NOTICE_API =
            "메이플스토리 진행 중 이벤트 목록";

    private final NexonApiRequester nexonApiRequester;

    public EventClientImpl(
            @Qualifier("nexonRestClient") RestClient nexonRestClient,
            ObjectMapper objectMapper
    ) {
        this.nexonApiRequester = new NexonApiRequester(
                nexonRestClient,
                objectMapper,
                this::createException,
                (apiName, errorCode) -> false
        );
    }

    @Override
    public EventNoticeListResponse getOngoingEvents() {
        EventNoticeListResponse response =
                nexonApiRequester.request(
                        EVENT_NOTICE_PATH,
                        Map.of(),
                        EventNoticeListResponse.class,
                        EVENT_NOTICE_API,
                        "status",
                        "ongoing"
                );

        if (response.eventNotice() == null
                || response.eventNotice()
                .stream()
                .anyMatch(event -> event == null)) {
            throw new EventException(
                    EventErrorCode.NEXON_API_RESPONSE_INVALID
            );
        }

        return response;
    }

    private EventException createException(
            NexonApiFailure failure
    ) {
        EventErrorCode errorCode = switch (failure) {
            case NOT_FOUND, CLIENT_ERROR ->
                    EventErrorCode.NEXON_API_CLIENT_ERROR;
            case SERVER_ERROR ->
                    EventErrorCode.NEXON_API_SERVER_ERROR;
            case TIMEOUT ->
                    EventErrorCode.NEXON_API_TIMEOUT;
            case RESPONSE_INVALID ->
                    EventErrorCode.NEXON_API_RESPONSE_INVALID;
        };

        return new EventException(errorCode);
    }
}
