package com.maplemetric.internal.application.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Overall Ranking 수집 결과의 보존 한도다.
 *
 * 자동 삭제는 하지 않기로 정했다. {@code enabled}는 꺼진 채로 둔다.
 *
 * 실제로 재어 보니 기준일 1개가 약 0.63 MB라 1년에 약 230 MB 늘어난다. 그런데
 * Nexon은 랭킹 이력을 2년만 주므로 2년 밖으로 나간 기준일은 지우면 다시 받을 수
 * 없다. 통계 이력의 전체 기간 조회는 남아 있는 전부를 보기 때문에, 지우는 만큼 그
 * 지표의 시작점이 매일 앞으로 밀린다. 아끼는 용량에 비해 잃는 것이 영구적이다.
 *
 * {@code retentionDays} 3650은 삭제를 시작하겠다는 뜻이 아니다. 10년이 지나 저장
 * 비용이 실제로 문제가 됐을 때 다시 판단하기 위한 상한이다. 켜더라도 그 전까지는
 * 지울 대상이 없다.
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
