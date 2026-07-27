package com.maplemetric.world.infrastructure.persistence;

import com.maplemetric.world.application.port.out.LoadWorldAliasPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class WorldAliasPersistenceAdapter implements LoadWorldAliasPort {

    private final WorldAliasRepository worldAliasRepository;

    WorldAliasPersistenceAdapter(WorldAliasRepository worldAliasRepository) {
        this.worldAliasRepository = worldAliasRepository;
    }

    @Override
    public Optional<MatchedWorld> findActiveByNormalizedAliasName(
            String normalizedAliasName
    ) {
        return worldAliasRepository
                .findActiveByNormalizedAliasName(normalizedAliasName)
                .map(alias -> new MatchedWorld(
                        alias.getWorld().getId(),
                        alias.getWorld().getWorldName()
                ));
    }
}
