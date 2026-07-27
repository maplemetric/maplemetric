package com.maplemetric.world.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface LoadWorldAliasPort {

    Optional<MatchedWorld> findActiveByNormalizedAliasName(
            String normalizedAliasName
    );

    record MatchedWorld(
            UUID worldId,
            String worldName
    ) {
    }
}
