package com.maplemetric.internal.infrastructure.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(
        prefix = "maplemetric.internal.ranking"
                + ".overall-ranking-collection.scheduler"
)
public record OverallRankingCollectionSchedulerProperties(
        boolean enabled,

        @NotBlank
        String cron,

        @NotBlank
        String zone
) {
}
