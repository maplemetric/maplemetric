package com.maplemetric.internal.infrastructure.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Backfill 실행 한도다.
 *
 * {@code requestInterval}은 Nexon 공식 Rate Limit 수치가 아니다. 공식 계약을 확인하기
 * 전까지 추측한 값을 코드에 고정하지 않으려고 설정값으로 두며, 기본값은 과거 데이터를
 * 몰아서 받지 않기 위한 보수적인 간격일 뿐이다.
 *
 * {@code maxDatesPerRun}은 한 번의 실행이 끝없이 도는 것을 막는다. 남은 기준일은 다음
 * 실행에서 이어서 처리한다.
 *
 * {@code staleClaimTimeout}은 강제 종료로 점유된 채 남은 기준일을 회수하는 기준이다.
 * 한 기준일의 정상 수집 소요보다 넉넉해야 정상 실행 중인 기준일을 뺏지 않는다.
 */
@Validated
@ConfigurationProperties(
        prefix = "maplemetric.internal.ranking.overall-ranking-backfill"
)
public record OverallRankingBackfillProperties(
        @Min(1) @Max(100) int maxPages,
        @Min(1) @Max(10) int maxAttemptsPerDate,
        @Min(1) @Max(365) int maxDatesPerRun,
        Duration requestInterval,
        Duration staleClaimTimeout
) {

    public OverallRankingBackfillProperties {
        if (requestInterval == null || requestInterval.isNegative()) {
            throw new IllegalArgumentException(
                    "Backfill 호출 간격은 0 이상이어야 합니다."
            );
        }

        if (staleClaimTimeout == null
                || staleClaimTimeout.isZero()
                || staleClaimTimeout.isNegative()) {
            throw new IllegalArgumentException(
                    "Backfill 점유 회수 임계 시간은 0보다 커야 합니다."
            );
        }
    }
}
