package com.maplemetric.ranking.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository
        extends JpaRepository<JobEntity, UUID> {

    Optional<JobEntity> findByJobNameAndDeletedAtIsNull(
            String jobName
    );
}
