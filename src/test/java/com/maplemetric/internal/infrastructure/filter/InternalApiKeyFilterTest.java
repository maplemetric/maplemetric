package com.maplemetric.internal.infrastructure.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.internal.infrastructure.properties.InternalApiProperties;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
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
