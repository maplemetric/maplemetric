package com.maplemetric.world.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorldRepository
        extends JpaRepository<WorldEntity, UUID> {

    Optional<WorldEntity> findByWorldNameAndDeletedAtIsNull(
            String worldName
    );
}
