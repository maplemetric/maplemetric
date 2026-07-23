package com.maplemetric.analysis.infrastructure.openai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "maplemetric.openai")
public record OpenAiProperties(

        boolean enabled,

        @NotBlank(message = "OpenAI API Base URL은 필수입니다.")
        String baseUrl,

        String key,

        String model,

        @NotNull(message = "OpenAI API 연결 제한 시간은 필수입니다.")
        Duration connectTimeout,

        @NotNull(message = "OpenAI API 응답 제한 시간은 필수입니다.")
        Duration readTimeout
) {
}
