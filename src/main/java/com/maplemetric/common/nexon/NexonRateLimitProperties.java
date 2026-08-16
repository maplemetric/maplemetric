package com.maplemetric.common.nexon;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nexon 요청의 초당 한도다.
 *
 * Nexon은 애플리케이션 단위로 초당 호출 수를 제한한다. 개발 단계와 서비스 단계의
 * 값이 다르므로 설정으로 둔다.
 */
@ConfigurationProperties(prefix = "maplemetric.nexon.rate-limit")
public record NexonRateLimitProperties(
        int requestsPerSecond
) {

    public NexonRateLimitProperties {
        if (requestsPerSecond < 1) {
            throw new IllegalArgumentException(
                    "넥슨 초당 호출 한도는 1 이상이어야 합니다."
            );
        }
    }
}
