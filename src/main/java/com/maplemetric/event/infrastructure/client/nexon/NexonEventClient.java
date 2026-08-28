package com.maplemetric.event.infrastructure.client.nexon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.common.nexon.NexonApiFailure;
import com.maplemetric.common.nexon.NexonApiRequester;
import com.maplemetric.common.nexon.NexonRequestRateGate;
import com.maplemetric.event.application.exception.EventException;
import com.maplemetric.event.application.exception.EventFailure;
import com.maplemetric.event.infrastructure.client.nexon.response.EventNoticeListResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class NexonEventClient {

    private static final String EVENT_NOTICE_PATH =
            "/maplestory/v1/notice-event";

    private static final String EVENT_NOTICE_API =
            "메이플스토리 진행 중 이벤트 목록";

    private final NexonApiRequester nexonApiRequester;

    NexonEventClient(
            @Qualifier("nexonRestClient") RestClient nexonRestClient,
            ObjectMapper objectMapper,
            NexonRequestRateGate rateGate
    ) {
        this.nexonApiRequester = new NexonApiRequester(
                nexonRestClient,
                objectMapper,
                this::createException,
                (apiName, errorCode) -> false,
                rateGate
        );
    }

    EventNoticeListResponse getOngoingEvents() {
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
                    EventFailure.RESPONSE_INVALID
            );
        }

        return response;
    }

    private EventException createException(
            NexonApiFailure failure
    ) {
        EventFailure eventFailure = switch (failure) {
            case NOT_FOUND, CLIENT_ERROR ->
                    EventFailure.CLIENT_ERROR;
            case SERVER_ERROR ->
                    EventFailure.SERVER_ERROR;
            // 한도 초과는 소비 측에 시간 초과와 같은 결과다. 지금 받을 수 없다.
            case RATE_LIMITED, TIMEOUT ->
                    EventFailure.TIMEOUT;
            case RESPONSE_INVALID ->
                    EventFailure.RESPONSE_INVALID;
        };

        return new EventException(eventFailure);
    }
}
