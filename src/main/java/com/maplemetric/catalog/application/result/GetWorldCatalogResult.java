package com.maplemetric.catalog.application.result;

import com.maplemetric.world.api.CanonicalWorld;
import java.util.List;

public record GetWorldCatalogResult(
        List<WorldResult> worlds
) {

    public static GetWorldCatalogResult from(
            List<CanonicalWorld> canonicalWorlds
    ) {
        return new GetWorldCatalogResult(
                canonicalWorlds.stream()
                        .map(canonicalWorld -> WorldResult.from(canonicalWorld))
                        .toList()
        );
    }

    public record WorldResult(
            String worldSlug,
            String worldName,
            CanonicalWorld.Status status,
            int displayOrder,
            String logoUrl
    ) {

        private static WorldResult from(CanonicalWorld canonicalWorld) {
            return new WorldResult(
                    canonicalWorld.worldSlug(),
                    canonicalWorld.worldName(),
                    canonicalWorld.status(),
                    canonicalWorld.displayOrder(),
                    CatalogAssetPath.worldLogoUrl(canonicalWorld.worldSlug())
            );
        }
    }
}
