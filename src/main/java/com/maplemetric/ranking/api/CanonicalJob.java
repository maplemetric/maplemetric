package com.maplemetric.ranking.api;

public record CanonicalJob(
        String jobSlug,
        String jobName,
        String jobGroup,
        String jobBranch,
        boolean available,
        int displayOrder
) {
}
