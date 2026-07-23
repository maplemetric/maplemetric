package com.maplemetric.notice.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.notice.domain.exception.NoticeErrorCode;
import com.maplemetric.notice.domain.exception.NoticeException;
import com.maplemetric.notice.infrastructure.client.dto.CashshopNoticeListResponse;
import com.maplemetric.notice.infrastructure.client.dto.NoticeListResponse;
import com.maplemetric.notice.infrastructure.client.dto.UpdateNoticeListResponse;
import java.io.IOException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

@ExtendWith(OutputCaptureExtension.class)
class NoticeClientImplTest {

    private static final String BASE_URL =
            "https://open.api.nexon.com";

    private static final String NEXON_API_KEY =
            "test-nexon-api-key";

    private static final String NOTICE_PATH =
            "/maplestory/v1/notice";

    private static final String UPDATE_NOTICE_PATH =
            "/maplestory/v1/notice-update";

    private static final String CASHSHOP_NOTICE_PATH =
            "/maplestory/v1/notice-cashshop";

    private ObjectMapper objectMapper;
    private MockRestServiceServer mockServer;
    private NoticeClientImpl noticeClient;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        RestClient.Builder restClientBuilder =
                RestClient.builder()
                        .baseUrl(BASE_URL)
                        .defaultHeader(
                                "x-nxopen-api-key",
                                NEXON_API_KEY
                        );

        mockServer = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();

        noticeClient = new NoticeClientImpl(
                restClientBuilder.build(),
                objectMapper
        );
    }

    @Test
    void 공지세분류를공식경로로조회하고역직렬화한다() {
        expectGetRequest(
                NOTICE_PATH,
                withSuccess(
                        """
                        {
                          "notice": [
                            {
                              "notice_id": 101,
                              "title": "일반 공지",
                              "url": "https://maplestory.nexon.com/101",
                              "date": "2026-07-21T10:00+09:00"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        expectGetRequest(
                UPDATE_NOTICE_PATH,
                withSuccess(
                        """
                        {
                          "update_notice": [
                            {
                              "notice_id": 102,
                              "title": "업데이트 공지",
                              "url": "https://maplestory.nexon.com/102",
                              "date": "2026-07-22T11:00+09:00"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        expectGetRequest(
                CASHSHOP_NOTICE_PATH,
                withSuccess(
                        """
                        {
                          "cashshop_notice": [
                            {
                              "notice_id": 103,
                              "title": "캐시샵 공지",
                              "url": "https://maplestory.nexon.com/103",
                              "date": "2026-07-23T12:00+09:00",
                              "date_sale_start": "2026-07-24T00:00+09:00",
                              "date_sale_end": "2026-08-01T23:59+09:00",
                              "ongoing_flag": "false"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        NoticeListResponse general =
                noticeClient.getNotices();

        UpdateNoticeListResponse update =
                noticeClient.getUpdateNotices();

        CashshopNoticeListResponse cashshop =
                noticeClient.getCashshopNotices();

        assertThat(general.notice())
                .singleElement()
                .satisfies(notice -> {
                    assertThat(notice.noticeId())
                            .isEqualTo(101L);
                    assertThat(notice.title())
                            .isEqualTo("일반 공지");
                });

        assertThat(update.updateNotice())
                .singleElement()
                .satisfies(notice ->
                        assertThat(notice.noticeId())
                                .isEqualTo(102L)
                );

        assertThat(cashshop.cashshopNotice())
                .singleElement()
                .satisfies(notice -> {
                    assertThat(notice.dateSaleStart())
                            .isEqualTo(
                                    "2026-07-24T00:00+09:00"
                            );
                    assertThat(notice.ongoingFlag())
                            .isEqualTo("false");
                });

        mockServer.verify();
    }

    @Test
    void 이백응답의빈공지목록은그대로반환한다() {
        expectGetRequest(
                NOTICE_PATH,
                withSuccess(
                        "{\"notice\": []}",
                        MediaType.APPLICATION_JSON
                )
        );

        NoticeListResponse result =
                noticeClient.getNotices();

        assertThat(result.notice()).isEmpty();

        mockServer.verify();
    }

    @Test
    void 공지목록이누락되면잘못된응답오류를반환한다() {
        expectGetRequest(
                NOTICE_PATH,
                withSuccess(
                        "{}",
                        MediaType.APPLICATION_JSON
                )
        );

        NoticeException exception = catchThrowableOfType(
                () -> noticeClient.getNotices(),
                NoticeException.class
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        NoticeErrorCode.NEXON_API_RESPONSE_INVALID
                );

        mockServer.verify();
    }

    @Test
    void 일반사엑스엑스응답은클라이언트오류로변환한다(
            CapturedOutput output
    ) {
        expectGetRequest(
                UPDATE_NOTICE_PATH,
                withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                """
                                {
                                  "error": {
                                    "name": "OPENAPI00004",
                                    "message": "Please input valid parameter"
                                  }
                                }
                                """
                        )
        );

        NoticeException exception = catchThrowableOfType(
                () -> noticeClient.getUpdateNotices(),
                NoticeException.class
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        NoticeErrorCode.NEXON_API_CLIENT_ERROR
                );

        assertThat(output)
                .contains("메이플스토리 업데이트 목록")
                .contains("OPENAPI00004")
                .doesNotContain(NEXON_API_KEY)
                .doesNotContain("x-nxopen-api-key");

        mockServer.verify();
    }

    @Test
    void 오엑스엑스응답은서버오류로변환한다() {
        expectGetRequest(
                CASHSHOP_NOTICE_PATH,
                withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        );

        NoticeException exception = catchThrowableOfType(
                () -> noticeClient.getCashshopNotices(),
                NoticeException.class
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        NoticeErrorCode.NEXON_API_SERVER_ERROR
                );

        mockServer.verify();
    }

    @Test
    void 타임아웃은타임아웃오류로변환한다() {
        NoticeClientImpl timeoutClient = createFailingClient(
                new SocketTimeoutException("read timeout")
        );

        NoticeException exception = catchThrowableOfType(
                () -> timeoutClient.getNotices(),
                NoticeException.class
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        NoticeErrorCode.NEXON_API_TIMEOUT
                );
    }

    private void expectGetRequest(
            String path,
            ResponseCreator responseCreator
    ) {
        mockServer.expect(request -> {
                    assertThat(request.getMethod())
                            .isEqualTo(HttpMethod.GET);
                    assertThat(request.getURI().getPath())
                            .isEqualTo(path);
                    assertThat(request.getURI().getRawQuery())
                            .isNull();
                })
                .andRespond(responseCreator);
    }

    private NoticeClientImpl createFailingClient(
            IOException exception
    ) {
        ClientHttpRequestFactory requestFactory =
                (uri, httpMethod) -> {
                    throw exception;
                };

        RestClient restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader(
                        "x-nxopen-api-key",
                        NEXON_API_KEY
                )
                .requestFactory(requestFactory)
                .build();

        return new NoticeClientImpl(
                restClient,
                objectMapper
        );
    }
}
