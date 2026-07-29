package com.maplemetric.ranking.infrastructure.persistence;

import com.maplemetric.ranking.application.port.out.LoadJobCatalogPort;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class JobCatalogPersistenceAdapter implements LoadJobCatalogPort {

    private final JobRepository jobRepository;

    private final JobAliasRepository jobAliasRepository;

    JobCatalogPersistenceAdapter(
            JobRepository jobRepository,
            JobAliasRepository jobAliasRepository
    ) {
        this.jobRepository = jobRepository;
        this.jobAliasRepository = jobAliasRepository;
    }

    @Override
    public Optional<CanonicalJobRow> findBySlug(String jobSlug) {
        return jobRepository
                .findByJobSlugAndDeletedAtIsNull(jobSlug)
                .map(job -> toRow(job));
    }

    @Override
    public List<CanonicalJobRow> findAllOrderByDisplayOrder() {
        return jobRepository
                .findAllByDeletedAtIsNullOrderByDisplayOrderAscJobNameAsc()
                .stream()
                .map(job -> toRow(job))
                .toList();
    }

    @Override
    public List<AliasMatch> findActiveByNormalizedAliasNames(
            Collection<String> normalizedAliasNames
    ) {
        if (normalizedAliasNames == null || normalizedAliasNames.isEmpty()) {
            return List.of();
        }

        return jobAliasRepository
                .findActiveByNormalizedAliasNames(normalizedAliasNames)
                .stream()
                .map(alias -> new AliasMatch(
                        normalize(alias.getAliasName()),
                        toRow(alias.getJob())
                ))
                .toList();
    }

    private CanonicalJobRow toRow(JobEntity job) {
        return new CanonicalJobRow(
                job.getJobSlug(),
                job.getJobName(),
                job.getJobGroup(),
                job.getJobBranch(),
                job.isAvailable(),
                job.getDisplayOrder()
        );
    }

    private String normalize(String aliasName) {
        return aliasName.trim().toLowerCase(Locale.ROOT);
    }
}
