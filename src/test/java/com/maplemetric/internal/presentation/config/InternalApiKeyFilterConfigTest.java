package com.maplemetric.internal.presentation.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.internal.presentation.filter.InternalApiKeyFilter;
import com.maplemetric.internal.application.properties.InternalApiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

/**
 * 내부 API 인증이 어느 경로에 걸리는지 고정한다.
 *
 * {@code InternalApiKeyFilterTest}는 필터가 {@code /internal} 경로를 막는다는 것을
 * 검증한다. 그 필터가 실제로 그 경로에 등록되는지는 별개이며, 등록 Pattern이
 * 좁아지면 새로 추가한 내부 Endpoint가 인증 없이 열린다. 그래서 Pattern 자체를
 * 여기서 못박는다.
 */
class InternalApiKeyFilterConfigTest {

    @Test
    void 내부경로전체에인증필터를등록한다() {
        FilterRegistrationBean<InternalApiKeyFilter> registration =
                new InternalApiKeyFilterConfig().internalApiKeyFilter(
                        new InternalApiProperties("secret-key"),
                        new ObjectMapper()
                );

        // Endpoint별로 나열하지 않는다. 하나라도 빠지면 그 경로가 그대로 열린다.
        assertThat(registration.getUrlPatterns())
                .containsExactly("/internal/*");

        assertThat(registration.getFilter())
                .isInstanceOf(InternalApiKeyFilter.class);
    }
}
