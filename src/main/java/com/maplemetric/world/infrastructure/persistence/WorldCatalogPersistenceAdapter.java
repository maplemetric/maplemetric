package com.maplemetric.world.infrastructure.persistence;

import com.maplemetric.world.api.CanonicalWorld;
import com.maplemetric.world.application.port.out.LoadWorldCatalogPort;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class WorldCatalogPersistenceAdapter implements LoadWorldCatalogPort {

    private final WorldRepository worldRepository;

    private final WorldAliasRepository worldAliasRepository;

    WorldCatalogPersistenceAdapter(
            WorldRepository worldRepository,
            WorldAliasRepository worldAliasRepository
    ) {
        this.worldRepository = worldRepository;
        this.worldAliasRepository = worldAliasRepository;
    }

    @Override
    public Optional<CanonicalWorldRow> findBySlug(String worldSlug) {
        return worldRepository
                .findByWorldSlugAndDeletedAtIsNull(worldSlug)
                .map(world -> toRow(world));
    }

    @Override
    public List<CanonicalWorldRow> findAllOrderByDisplayOrder() {
        return worldRepository
                .findAllByDeletedAtIsNullOrderByDisplayOrderAscWorldNameAsc()
                .stream()
                .map(world -> toRow(world))
                .toList();
    }

    @Override
    public List<AliasMatch> findActiveByNormalizedAliasNames(
            Collection<String> normalizedAliasNames
    ) {
        if (normalizedAliasNames == null || normalizedAliasNames.isEmpty()) {
            return List.of();
        }

        return worldAliasRepository
                .findActiveByNormalizedAliasNames(normalizedAliasNames)
                .stream()
                .map(alias -> new AliasMatch(
                        normalize(alias.getAliasName()),
                        toRow(alias.getWorld())
                ))
                .toList();
    }

    private CanonicalWorldRow toRow(WorldEntity world) {
        return new CanonicalWorldRow(
                world.getWorldSlug(),
                world.getWorldName(),
                toStatus(world.getStatus()),
                world.getDisplayOrder()
        );
    }

    private CanonicalWorld.Status toStatus(WorldEntity.Status status) {
        return switch (status) {
            case ACTIVE -> CanonicalWorld.Status.ACTIVE;
            case CLOSED -> CanonicalWorld.Status.CLOSED;
        };
    }

    private String normalize(String aliasName) {
        return aliasName.trim().toLowerCase(Locale.ROOT);
    }
}
