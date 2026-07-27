package com.maplemetric.ranking.infrastructure.persistence;

import com.maplemetric.ranking.application.port.out.LoadJobAliasPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class JobAliasPersistenceAdapter implements LoadJobAliasPort {

    private final JobAliasRepository jobAliasRepository;

    JobAliasPersistenceAdapter(JobAliasRepository jobAliasRepository) {
        this.jobAliasRepository = jobAliasRepository;
    }

    @Override
    public Optional<MatchedJob> findActiveByNormalizedAliasName(
            String normalizedAliasName
    ) {
        return jobAliasRepository
                .findActiveByNormalizedAliasName(normalizedAliasName)
                .map(alias -> new MatchedJob(
                        alias.getJob().getId(),
                        alias.getJob().getJobName()
                ));
    }
}
