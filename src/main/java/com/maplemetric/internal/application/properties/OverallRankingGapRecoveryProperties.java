package com.maplemetric.internal.application.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 정기 수집 뒤 비어 있는 기준일을 메우는 설정이다.
 *
 * 정기 수집은 앱이 떠 있는 동안에만 돈다. 앱이 꺼져 있던 날은 그대로 빈다. 사람이
 * 기억해 메우는 절차는 잊히고, 잊힌 채로 외부의 제공 기간이 지나면 그 기준일은 영영
 * 받을 수 없다.
 *
 * {@code lookbackDays}는 되짚어 볼 기간이다. 외부가 이력을 주는 기간을 넘겨 잡아도
 * 채울 수 없는 날만 세게 되므로 그보다 넉넉히 잡지 않는다.
 *
 * {@code maxDatesPerRun}은 한 번에 메울 기준일 수다. 기준일 1개가 여러 번의 외부
 * 호출을 쓰므로 하루 호출 한도 안에 들도록 좁게 잡는다.
 */
@Validated
@ConfigurationProperties(
        prefix = "maplemetric.internal.ranking.overall-ranking-gap-recovery"
)
public record OverallRankingGapRecoveryProperties(
        boolean enabled,
        @Min(1) @Max(1095) int lookbackDays,
        @Min(1) @Max(365) int maxDatesPerRun
) {
}
