package com.maplemetric.notice.infrastructure.client.nexon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.common.nexon.NexonApiFailure;
import com.maplemetric.common.nexon.NexonApiRequester;
import com.maplemetric.common.nexon.NexonRequestRateGate;
import com.maplemetric.notice.application.exception.NoticeException;
import com.maplemetric.notice.application.exception.NoticeFailure;
import com.maplemetric.notice.infrastructure.client.nexon.response.CashshopNoticeListResponse;
import com.maplemetric.notice.infrastructure.client.nexon.response.NoticeListResponse;
import com.maplemetric.notice.infrastructure.client.nexon.response.UpdateNoticeListResponse;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class NexonNoticeClient {

    private static final String NOTICE_PATH =
            "/maplestory/v1/notice";

    private static final String UPDATE_NOTICE_PATH =
            "/maplestory/v1/notice-update";

    private static final String CASHSHOP_NOTICE_PATH =
            "/maplestory/v1/notice-cashshop";

    private static final String NOTICE_API =
            "메이플스토리 공지 목록";

    private static final String UPDATE_NOTICE_API =
            "메이플스토리 업데이트 목록";

    private static final String CASHSHOP_NOTICE_API =
            "메이플스토리 캐시샵 공지 목록";

    private final NexonApiRequester nexonApiRequester;

    NexonNoticeClient(
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

    NoticeListResponse getNotices() {
        NoticeListResponse response = request(
                NOTICE_PATH,
                NoticeListResponse.class,
                NOTICE_API,
                "general"
        );

        validateNotices(response.notice());

        return response;
    }

    UpdateNoticeListResponse getUpdateNotices() {
        UpdateNoticeListResponse response = request(
                UPDATE_NOTICE_PATH,
                UpdateNoticeListResponse.class,
                UPDATE_NOTICE_API,
                "update"
        );

        validateNotices(response.updateNotice());

        return response;
    }

    CashshopNoticeListResponse getCashshopNotices() {
        CashshopNoticeListResponse response = request(
                CASHSHOP_NOTICE_PATH,
                CashshopNoticeListResponse.class,
                CASHSHOP_NOTICE_API,
                "cashshop"
        );

        validateNotices(response.cashshopNotice());

        return response;
    }

    private <T> T request(
            String path,
            Class<T> responseType,
            String apiName,
            String category
    ) {
        return nexonApiRequester.request(
                path,
                Map.of(),
                responseType,
                apiName,
                "category",
                category
        );
    }

    private void validateNotices(List<?> notices) {
        if (notices == null
                || notices.stream()
                .anyMatch(notice -> notice == null)) {
            throw new NoticeException(
                    NoticeFailure.RESPONSE_INVALID
            );
        }
    }

    private NoticeException createException(
            NexonApiFailure failure
    ) {
        NoticeFailure noticeFailure = switch (failure) {
            case NOT_FOUND, CLIENT_ERROR ->
                    NoticeFailure.CLIENT_ERROR;
            case SERVER_ERROR ->
                    NoticeFailure.SERVER_ERROR;
            // 한도 초과는 소비 측에 시간 초과와 같은 결과다. 지금 받을 수 없다.
            case RATE_LIMITED, TIMEOUT ->
                    NoticeFailure.TIMEOUT;
            case RESPONSE_INVALID ->
                    NoticeFailure.RESPONSE_INVALID;
        };

        return new NoticeException(noticeFailure);
    }
}
