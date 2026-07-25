package com.maplemetric.internal.infrastructure.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(
        prefix = "maplemetric.internal.ranking.overall-ranking-collection"
)
public record OverallRankingCollectionProperties(
        @Min(1) @Max(100) int maxPages
) {
}
