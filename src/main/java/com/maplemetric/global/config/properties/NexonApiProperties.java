package com.maplemetric.global.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "maplemetric.nexon.open-api")
public record NexonApiProperties(

        @NotBlank(message = "Nexon Open API Base URL은 필수입니다.")
        String baseUrl,

        @NotBlank(message = "Nexon Open API Key는 필수입니다.")
        String key
) {
}