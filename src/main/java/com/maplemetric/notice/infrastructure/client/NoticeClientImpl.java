package com.maplemetric.notice.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.common.nexon.NexonApiFailure;
import com.maplemetric.common.nexon.NexonApiRequester;
import com.maplemetric.notice.domain.exception.NoticeErrorCode;
import com.maplemetric.notice.domain.exception.NoticeException;
import com.maplemetric.notice.infrastructure.client.dto.CashshopNoticeListResponse;
import com.maplemetric.notice.infrastructure.client.dto.NoticeListResponse;
import com.maplemetric.notice.infrastructure.client.dto.UpdateNoticeListResponse;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NoticeClientImpl implements NoticeClient {

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

    public NoticeClientImpl(
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
    public NoticeListResponse getNotices() {
        NoticeListResponse response = request(
                NOTICE_PATH,
                NoticeListResponse.class,
                NOTICE_API,
                "general"
        );

        validateNotices(response.notice());

        return response;
    }

    @Override
    public UpdateNoticeListResponse getUpdateNotices() {
        UpdateNoticeListResponse response = request(
                UPDATE_NOTICE_PATH,
                UpdateNoticeListResponse.class,
                UPDATE_NOTICE_API,
                "update"
        );

        validateNotices(response.updateNotice());

        return response;
    }

    @Override
    public CashshopNoticeListResponse getCashshopNotices() {
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
                    NoticeErrorCode.NEXON_API_RESPONSE_INVALID
            );
        }
    }

    private NoticeException createException(
            NexonApiFailure failure
    ) {
        NoticeErrorCode errorCode = switch (failure) {
            case NOT_FOUND, CLIENT_ERROR ->
                    NoticeErrorCode.NEXON_API_CLIENT_ERROR;
            case SERVER_ERROR ->
                    NoticeErrorCode.NEXON_API_SERVER_ERROR;
            case TIMEOUT ->
                    NoticeErrorCode.NEXON_API_TIMEOUT;
            case RESPONSE_INVALID ->
                    NoticeErrorCode.NEXON_API_RESPONSE_INVALID;
        };

        return new NoticeException(errorCode);
    }
}
