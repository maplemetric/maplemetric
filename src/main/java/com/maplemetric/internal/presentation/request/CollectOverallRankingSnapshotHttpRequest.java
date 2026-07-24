package com.maplemetric.internal.presentation.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;

public record CollectOverallRankingSnapshotHttpRequest(
        LocalDate rankingDate,

        @Min(1) @Max(100)
        Integer maxPages
) {
}
