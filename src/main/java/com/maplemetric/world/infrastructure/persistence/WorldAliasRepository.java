package com.maplemetric.world.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorldAliasRepository
        extends JpaRepository<WorldAliasEntity, UUID> {

    @Query("""
            SELECT a
            FROM WorldAliasEntity a
            JOIN FETCH a.world w
            WHERE LOWER(TRIM(a.aliasName)) = :normalizedAliasName
              AND a.deletedAt IS NULL
              AND w.deletedAt IS NULL
            """)
    Optional<WorldAliasEntity> findActiveByNormalizedAliasName(
            @Param("normalizedAliasName") String normalizedAliasName
    );
}
