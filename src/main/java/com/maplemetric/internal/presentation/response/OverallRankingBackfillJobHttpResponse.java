package com.maplemetric.internal.presentation.response;

import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillDate;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillErrorType;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillJob;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OverallRankingBackfillJobHttpResponse(
        UUID backfillJobId,
        LocalDate requestedFrom,
        LocalDate requestedTo,
        BackfillStatus status,
        int succeededDateCount,
        int failedDateCount,
        int skippedDateCount,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        List<BackfillDateHttpResponse> dates
) {

    public static OverallRankingBackfillJobHttpResponse from(
            BackfillJob job,
            List<BackfillDate> dates
    ) {
        return new OverallRankingBackfillJobHttpResponse(
                job.id(),
                job.requestedFrom(),
                job.requestedTo(),
                job.status(),
                job.succeededDateCount(),
                job.failedDateCount(),
                job.skippedDateCount(),
                job.createdAt(),
                job.startedAt(),
                job.finishedAt(),
                dates.stream()
                        .map(date -> BackfillDateHttpResponse.from(date))
                        .toList()
        );
    }

    public record BackfillDateHttpResponse(
            LocalDate snapshotDate,
            BackfillStatus status,
            int attemptCount,
            BackfillErrorType lastErrorType
    ) {

        static BackfillDateHttpResponse from(BackfillDate date) {
            return new BackfillDateHttpResponse(
                    date.snapshotDate(),
                    date.status(),
                    date.attemptCount(),
                    date.lastErrorType()
            );
        }
    }
}
