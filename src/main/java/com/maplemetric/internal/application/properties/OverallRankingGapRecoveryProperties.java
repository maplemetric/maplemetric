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
 * {@code lookbackDays}는 되짚어 볼 기간이다. 앱이 꺼져 있어 빠진 최근 며칠을 메우는
 * 것이 목적이므로 짧게 잡는다. 길게 잡으면 오래전에 한 번도 받지 않은 기간까지 빈
 * 날로 세어, 매일 그 과거를 받아 오느라 정작 최근에 빠진 날이 뒤로 밀린다.
 *
 * 이 기간을 넘긴 공백은 자동으로 메우지 않는다. 그만큼 오래 멈춰 있었다면 무엇을
 * 어디까지 받을지는 사람이 정할 일이고, 그 경로는 수동 수집으로 이미 있다.
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
        @Min(1) @Max(365) int lookbackDays,
        @Min(1) @Max(365) int maxDatesPerRun
) {
}
