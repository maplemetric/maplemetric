package com.maplemetric.internal.presentation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.internal.presentation.filter.InternalApiKeyFilter;
import com.maplemetric.internal.application.properties.InternalApiProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class InternalApiKeyFilterConfig {

    @Bean
    FilterRegistrationBean<InternalApiKeyFilter> internalApiKeyFilter(
            InternalApiProperties properties,
            ObjectMapper objectMapper
    ) {
        FilterRegistrationBean<InternalApiKeyFilter> registration =
                new FilterRegistrationBean<>(
                        new InternalApiKeyFilter(properties, objectMapper)
                );

        registration.addUrlPatterns("/internal/*");

        return registration;
    }
}
