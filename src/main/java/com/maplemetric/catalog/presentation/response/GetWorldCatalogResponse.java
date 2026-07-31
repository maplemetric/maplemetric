package com.maplemetric.catalog.presentation.response;

import com.maplemetric.catalog.application.result.GetWorldCatalogResult;
import com.maplemetric.world.api.CanonicalWorld;
import java.util.List;

public record GetWorldCatalogResponse(
        List<World> worlds
) {

    public static GetWorldCatalogResponse from(
            GetWorldCatalogResult result
    ) {
        return new GetWorldCatalogResponse(
                result.worlds()
                        .stream()
                        .map(world -> new World(
                                world.worldSlug(),
                                world.worldName(),
                                world.status(),
                                world.displayOrder(),
                                world.logoUrl()
                        ))
                        .toList()
        );
    }

    public record World(
            String worldSlug,
            String worldName,
            CanonicalWorld.Status status,
            int displayOrder,
            String logoUrl
    ) {
    }
}
