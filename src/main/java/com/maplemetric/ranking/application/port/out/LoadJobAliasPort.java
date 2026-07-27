package com.maplemetric.ranking.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface LoadJobAliasPort {

    Optional<MatchedJob> findActiveByNormalizedAliasName(
            String normalizedAliasName
    );

    record MatchedJob(
            UUID jobId,
            String jobName
    ) {
    }
}
