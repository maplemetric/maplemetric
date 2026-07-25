package com.maplemetric.internal.infrastructure.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.internal.infrastructure.properties.InternalApiProperties;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class InternalApiKeyFilterTest {

    private static final String API_KEY_HEADER = "X-Internal-API-Key";
    private static final String CONFIGURED_KEY = "secret-key";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 정상키는통과한다() throws Exception {
        InternalApiKeyFilter filter =
                createFilter(CONFIGURED_KEY);

        MockHttpServletRequest request =
                createRequest("/internal/v1/collections/rankings/overall");
        request.addHeader(API_KEY_HEADER, CONFIGURED_KEY);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1))
                .doFilter(request, response);
    }

    @Test
    void 잘못된키는401을반환한다() throws Exception {
        InternalApiKeyFilter filter =
                createFilter(CONFIGURED_KEY);

        MockHttpServletRequest request =
                createRequest("/internal/v1/collections/rankings/overall");
        request.addHeader(API_KEY_HEADER, "wrong-key");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void Header가없으면401을반환한다() throws Exception {
        InternalApiKeyFilter filter =
                createFilter(CONFIGURED_KEY);

        MockHttpServletRequest request =
                createRequest("/internal/v1/collections/rankings/overall");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void Header가Blank이면401을반환한다() throws Exception {
        InternalApiKeyFilter filter =
                createFilter(CONFIGURED_KEY);

        MockHttpServletRequest request =
                createRequest("/internal/v1/collections/rankings/overall");
        request.addHeader(API_KEY_HEADER, "   ");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void 설정키가없으면401을반환한다() throws Exception {
        InternalApiKeyFilter filter = createFilter(null);

        MockHttpServletRequest request =
                createRequest("/internal/v1/collections/rankings/overall");
        request.addHeader(API_KEY_HEADER, CONFIGURED_KEY);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void 설정키가Blank이면401을반환한다() throws Exception {
        InternalApiKeyFilter filter = createFilter("   ");

        MockHttpServletRequest request =
                createRequest("/internal/v1/collections/rankings/overall");
        request.addHeader(API_KEY_HEADER, CONFIGURED_KEY);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void 공개API경로에는적용되지않는다() throws Exception {
        InternalApiKeyFilter filter =
                createFilter(CONFIGURED_KEY);

        MockHttpServletRequest request =
                createRequest("/api/v1/statistics/jobs");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1))
                .doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void Actuator경로에는적용되지않는다() throws Exception {
        InternalApiKeyFilter filter =
                createFilter(CONFIGURED_KEY);

        MockHttpServletRequest request =
                createRequest("/actuator/health");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1))
                .doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void ContextPath배포에서도내부경로에적용된다() throws Exception {
        InternalApiKeyFilter filter =
                createFilter(CONFIGURED_KEY);

        MockHttpServletRequest request =
                createRequest("/app/internal/v1/collections/rankings/overall");
        request.setContextPath("/app");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void ContextPath배포에서정상키는통과한다() throws Exception {
        InternalApiKeyFilter filter =
                createFilter(CONFIGURED_KEY);

        MockHttpServletRequest request =
                createRequest("/app/internal/v1/collections/rankings/overall");
        request.setContextPath("/app");
        request.addHeader(API_KEY_HEADER, CONFIGURED_KEY);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1))
                .doFilter(request, response);
    }

    @Test
    void 유사Prefix경로에는적용되지않는다() throws Exception {
        InternalApiKeyFilter filter =
                createFilter(CONFIGURED_KEY);

        for (String uri : new String[]{
                "/internality/v1/status",
                "/internal-api/v1/status"
        }) {
            MockHttpServletRequest request = createRequest(uri);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            filter.doFilter(request, response, filterChain);

            verify(filterChain, times(1))
                    .doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void 내부Root경로에도적용된다() throws Exception {
        InternalApiKeyFilter filter =
                createFilter(CONFIGURED_KEY);

        MockHttpServletRequest request = createRequest("/internal");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void 인증실패응답은JSON형식이며Key원문을포함하지않는다() throws Exception {
        InternalApiKeyFilter filter =
                createFilter(CONFIGURED_KEY);

        MockHttpServletRequest request =
                createRequest("/internal/v1/collections/rankings/overall");
        request.addHeader(API_KEY_HEADER, "wrong-key");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        String body = response.getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("\"success\":false");
        assertThat(body).doesNotContain(CONFIGURED_KEY);
        assertThat(body).doesNotContain("wrong-key");
        assertThat(body).doesNotContain(CONFIGURED_KEY.substring(0, 3));
        assertThat(body).doesNotContain(
                String.valueOf(CONFIGURED_KEY.length())
        );
        assertThat(body).doesNotContain(
                String.valueOf(CONFIGURED_KEY.hashCode())
        );
    }

    @Test
    void 인증실패로그에Key정보를남기지않는다() throws Exception {
        Logger logger =
                (Logger) LoggerFactory.getLogger(InternalApiKeyFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            InternalApiKeyFilter filter =
                    createFilter(CONFIGURED_KEY);

            MockHttpServletRequest request =
                    createRequest("/internal/v1/collections/rankings/overall");
            request.addHeader(API_KEY_HEADER, "wrong-key");

            filter.doFilter(
                    request,
                    new MockHttpServletResponse(),
                    mock(FilterChain.class)
            );

            assertThat(appender.list).isNotEmpty();
            assertThat(appender.list)
                    .allSatisfy(event -> {
                        String message = event.getFormattedMessage();

                        assertThat(message)
                                .doesNotContain(CONFIGURED_KEY)
                                .doesNotContain("wrong-key")
                                .doesNotContain(
                                        CONFIGURED_KEY.substring(0, 3)
                                )
                                .doesNotContain(
                                        String.valueOf(
                                                CONFIGURED_KEY.length()
                                        )
                                )
                                .doesNotContain(
                                        String.valueOf(
                                                CONFIGURED_KEY.hashCode()
                                        )
                                );
                    });
        } finally {
            logger.detachAppender(appender);
        }
    }

    private InternalApiKeyFilter createFilter(String configuredKey) {
        return new InternalApiKeyFilter(
                new InternalApiProperties(configuredKey),
                objectMapper
        );
    }

    private MockHttpServletRequest createRequest(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}
