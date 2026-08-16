package com.maplemetric.common.nexon;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nexon 요청의 초당 한도와 허가 대기 상한이다.
 *
 * Nexon은 애플리케이션 단위로 초당 호출 수를 제한한다. 개발 단계와 서비스 단계의
 * 값이 다르므로 설정으로 둔다.
 *
 * {@code maxWait}는 허가를 기다리는 한도다. 상한이 없으면 요청이 몰릴 때 처리
 * Thread가 언제 끝날지 모르는 대기에 묶인다. 넘기면 시간 초과로 알리고 자리를
 * 비워 주는 편이 낫다.
 */
@ConfigurationProperties(prefix = "maplemetric.nexon.rate-limit")
public record NexonRateLimitProperties(
        int requestsPerSecond,
        Duration maxWait
) {

    public NexonRateLimitProperties {
        if (requestsPerSecond < 1) {
            throw new IllegalArgumentException(
                    "넥슨 초당 호출 한도는 1 이상이어야 합니다."
            );
        }

        if (maxWait == null || maxWait.isNegative() || maxWait.isZero()) {
            throw new IllegalArgumentException(
                    "넥슨 허가 대기 상한은 0보다 커야 합니다."
            );
        }
    }
}
