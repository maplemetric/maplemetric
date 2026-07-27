package com.maplemetric.world.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorldAliasRepository
        extends JpaRepository<WorldAliasEntity, UUID> {
}
