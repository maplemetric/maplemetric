package com.maplemetric.catalog.presentation.response;

import com.maplemetric.catalog.application.result.GetJobCatalogResult;
import java.util.List;

public record GetJobCatalogResponse(
        List<Job> jobs
) {

    public static GetJobCatalogResponse from(
            GetJobCatalogResult result
    ) {
        return new GetJobCatalogResponse(
                result.jobs()
                        .stream()
                        .map(job -> new Job(
                                job.jobSlug(),
                                job.jobName(),
                                job.jobGroup(),
                                job.jobBranch(),
                                job.available(),
                                job.displayOrder(),
                                job.iconUrl()
                        ))
                        .toList()
        );
    }

    public record Job(
            String jobSlug,
            String jobName,
            String jobGroup,
            String jobBranch,
            boolean available,
            int displayOrder,
            String iconUrl
    ) {
    }
}
