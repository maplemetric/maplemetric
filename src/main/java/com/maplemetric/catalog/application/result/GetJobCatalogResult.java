package com.maplemetric.catalog.application.result;

import com.maplemetric.ranking.api.CanonicalJob;
import java.util.List;

public record GetJobCatalogResult(
        List<JobResult> jobs
) {

    public static GetJobCatalogResult from(
            List<CanonicalJob> canonicalJobs
    ) {
        return new GetJobCatalogResult(
                canonicalJobs.stream()
                        .map(canonicalJob -> JobResult.from(canonicalJob))
                        .toList()
        );
    }

    public record JobResult(
            String jobSlug,
            String jobName,
            String jobGroup,
            String jobBranch,
            boolean available,
            int displayOrder,
            String iconUrl
    ) {

        private static JobResult from(CanonicalJob canonicalJob) {
            return new JobResult(
                    canonicalJob.jobSlug(),
                    canonicalJob.jobName(),
                    canonicalJob.jobGroup(),
                    canonicalJob.jobBranch(),
                    canonicalJob.available(),
                    canonicalJob.displayOrder(),
                    CatalogAssetPath.jobIconUrl(canonicalJob.jobSlug())
            );
        }
    }
}
