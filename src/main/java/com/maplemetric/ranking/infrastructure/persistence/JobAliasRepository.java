package com.maplemetric.ranking.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobAliasRepository
        extends JpaRepository<JobAliasEntity, UUID> {

    @Query("""
            SELECT a
            FROM JobAliasEntity a
            JOIN FETCH a.job j
            WHERE LOWER(TRIM(a.aliasName)) = :normalizedAliasName
              AND a.deletedAt IS NULL
              AND j.deletedAt IS NULL
            """)
    Optional<JobAliasEntity> findActiveByNormalizedAliasName(
            @Param("normalizedAliasName") String normalizedAliasName
    );

    @Query("""
            SELECT a
            FROM JobAliasEntity a
            JOIN FETCH a.job j
            WHERE LOWER(TRIM(a.aliasName)) IN :normalizedAliasNames
              AND a.deletedAt IS NULL
              AND j.deletedAt IS NULL
            """)
    List<JobAliasEntity> findActiveByNormalizedAliasNames(
            @Param("normalizedAliasNames")
            Collection<String> normalizedAliasNames
    );
}
