package com.maplemetric.ranking.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobAliasRepository
        extends JpaRepository<JobAliasEntity, UUID> {
}
