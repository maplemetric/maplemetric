package com.maplemetric.analysis.application.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 통계 설명 생성 한도다.
 *
 * {@code maxPerRun}은 한 번의 실행이 만들 수 있는 최대 건수다. 유료 호출이므로
 * 잘못된 실행 한 번이 예산을 소진하지 않아야 한다.
 *
 * {@code rangePreset}은 어떤 기간의 Fact로 설명을 만들지다. 기본이 `90D`인 이유는
 * 실측상 하루·일주일 단위 변화가 노이즈이기 때문이다.
 */
@Validated
@ConfigurationProperties(prefix = "maplemetric.analysis.statistics-insight")
public record StatisticsInsightProperties(
        @Min(1) @Max(200) int maxPerRun,

        @NotBlank
        String rangePreset
) {
}
