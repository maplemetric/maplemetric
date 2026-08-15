package com.maplemetric.internal.application.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Overall Ranking 수집 결과의 보존 한도다.
 *
 * {@code enabled}는 기본이 꺼짐이다. 삭제는 되돌릴 수 없고 보존 일수는 아직 운영
 * 데이터를 측정해 확정하지 않았다. 승인 전까지는 대상 산정만 가능하다.
 *
 * {@code retentionDays}는 확정된 정책값이 아니라 켜기 전에 반드시 다시 정해야 하는
 * 자리다. 기본값은 어떤 조회 기간보다도 넉넉하게 잡아 실수로 켜도 최근 데이터가
 * 사라지지 않게 한다.
 */
@Validated
@ConfigurationProperties(
        prefix = "maplemetric.internal.ranking.overall-ranking-retention"
)
public record OverallRankingRetentionProperties(
        boolean enabled,
        @Min(1) @Max(3650) int retentionDays,
        @Min(1) @Max(365) int maxDatesPerRun
) {
}
