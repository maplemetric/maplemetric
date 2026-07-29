package com.maplemetric.world.application.port.out;

import com.maplemetric.world.api.CanonicalWorld;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadWorldCatalogPort {

    Optional<CanonicalWorldRow> findBySlug(String worldSlug);

    List<CanonicalWorldRow> findAllOrderByDisplayOrder();

    List<AliasMatch> findActiveByNormalizedAliasNames(
            Collection<String> normalizedAliasNames
    );

    record CanonicalWorldRow(
            String worldSlug,
            String worldName,
            CanonicalWorld.Status status,
            int displayOrder
    ) {
    }

    record AliasMatch(
            String normalizedAliasName,
            CanonicalWorldRow world
    ) {
    }
}
