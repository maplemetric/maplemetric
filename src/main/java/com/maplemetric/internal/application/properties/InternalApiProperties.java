package com.maplemetric.internal.application.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "maplemetric.internal")
public record InternalApiProperties(
        String apiKey
) {
}
