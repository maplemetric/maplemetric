package com.maplemetric.event.infrastructure.client.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.event.application.exception.EventException;
import com.maplemetric.event.application.exception.EventFailure;
import com.maplemetric.event.infrastructure.client.nexon.response.EventNoticeListResponse;
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
class NexonEventClientTest {

    private static final String BASE_URL =
            "https://open.api.nexon.com";

    private static final String NEXON_API_KEY =
            "test-nexon-api-key";

    private static final String EVENT_NOTICE_PATH =
            "/maplestory/v1/notice-event";

    private ObjectMapper objectMapper;
    private MockRestServiceServer mockServer;
    private NexonEventClient eventClient;

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

        eventClient = new NexonEventClient(
                restClientBuilder.build(),
                objectMapper
        );
    }

    @Test
    void 진행중이벤트를공식경로로조회하고역직렬화한다() {
        expectGetRequest(
                withSuccess(
                        """
                        {
                          "event_notice": [
                            {
                              "notice_id": 201,
                              "title": "여름 이벤트",
                              "url": "https://maplestory.nexon.com/201",
                              "date": "2026-07-20T10:00+09:00",
                              "date_event_start": "2026-07-21T00:00+09:00",
                              "date_event_end": "2026-08-20T23:59+09:00"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        EventNoticeListResponse result =
                eventClient.getOngoingEvents();

        assertThat(result.eventNotice())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.noticeId())
                            .isEqualTo(201L);
                    assertThat(event.title())
                            .isEqualTo("여름 이벤트");
                    assertThat(event.dateEventStart())
                            .isEqualTo(
                                    "2026-07-21T00:00+09:00"
                            );
                    assertThat(event.dateEventEnd())
                            .isEqualTo(
                                    "2026-08-20T23:59+09:00"
                            );
                });

        mockServer.verify();
    }

    @Test
    void 이백응답의빈이벤트목록은그대로반환한다() {
        expectGetRequest(
                withSuccess(
                        "{\"event_notice\": []}",
                        MediaType.APPLICATION_JSON
                )
        );

        EventNoticeListResponse result =
                eventClient.getOngoingEvents();

        assertThat(result.eventNotice()).isEmpty();

        mockServer.verify();
    }

    @Test
    void 이벤트목록이누락되면잘못된응답오류를반환한다() {
        expectGetRequest(
                withSuccess(
                        "{}",
                        MediaType.APPLICATION_JSON
                )
        );

        EventException exception = catchThrowableOfType(
                () -> eventClient.getOngoingEvents(),
                EventException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(EventFailure.RESPONSE_INVALID);

        mockServer.verify();
    }

    @Test
    void 일반사엑스엑스응답은클라이언트오류로변환한다(
            CapturedOutput output
    ) {
        expectGetRequest(
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

        EventException exception = catchThrowableOfType(
                () -> eventClient.getOngoingEvents(),
                EventException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(EventFailure.CLIENT_ERROR);

        assertThat(output)
                .contains("메이플스토리 진행 중 이벤트 목록")
                .contains("OPENAPI00004")
                .doesNotContain(NEXON_API_KEY)
                .doesNotContain("x-nxopen-api-key");

        mockServer.verify();
    }

    @Test
    void 오엑스엑스응답은서버오류로변환한다() {
        expectGetRequest(
                withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        );

        EventException exception = catchThrowableOfType(
                () -> eventClient.getOngoingEvents(),
                EventException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(EventFailure.SERVER_ERROR);

        mockServer.verify();
    }

    @Test
    void 타임아웃은타임아웃오류로변환한다() {
        NexonEventClient timeoutClient = createFailingClient(
                new SocketTimeoutException("read timeout")
        );

        EventException exception = catchThrowableOfType(
                () -> timeoutClient.getOngoingEvents(),
                EventException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(EventFailure.TIMEOUT);
    }

    private void expectGetRequest(
            ResponseCreator responseCreator
    ) {
        mockServer.expect(request -> {
                    assertThat(request.getMethod())
                            .isEqualTo(HttpMethod.GET);
                    assertThat(request.getURI().getPath())
                            .isEqualTo(EVENT_NOTICE_PATH);
                    assertThat(request.getURI().getRawQuery())
                            .isNull();
                })
                .andRespond(responseCreator);
    }

    private NexonEventClient createFailingClient(
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

        return new NexonEventClient(
                restClient,
                objectMapper
        );
    }
}
