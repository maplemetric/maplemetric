package com.maplemetric.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexon.api")
public record NexonApiProperties(
        String baseUrl,
        String key
) {
}
