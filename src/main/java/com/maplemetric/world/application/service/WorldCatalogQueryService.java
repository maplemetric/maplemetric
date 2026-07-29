package com.maplemetric.world.application.service;

import com.maplemetric.world.api.CanonicalWorld;
import com.maplemetric.world.api.WorldCatalogQuery;
import com.maplemetric.world.application.port.out.LoadWorldCatalogPort;
import com.maplemetric.world.application.port.out.LoadWorldCatalogPort.CanonicalWorldRow;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorldCatalogQueryService implements WorldCatalogQuery {

    private final LoadWorldCatalogPort loadWorldCatalogPort;

    WorldCatalogQueryService(LoadWorldCatalogPort loadWorldCatalogPort) {
        this.loadWorldCatalogPort = loadWorldCatalogPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CanonicalWorld> findBySlug(String worldSlug) {
        if (worldSlug == null || worldSlug.isBlank()) {
            return Optional.empty();
        }

        return loadWorldCatalogPort
                .findBySlug(worldSlug.trim())
                .map(row -> toCanonicalWorld(row));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanonicalWorld> findAll() {
        return loadWorldCatalogPort.findAllOrderByDisplayOrder()
                .stream()
                .map(row -> toCanonicalWorld(row))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, CanonicalWorld> resolveAliases(
            Collection<String> aliasNames
    ) {
        Map<String, String> normalizedByAliasName =
                normalizeAll(aliasNames);

        if (normalizedByAliasName.isEmpty()) {
            return Map.of();
        }

        Map<String, CanonicalWorldRow> worldByNormalizedAliasName =
                loadWorldCatalogPort
                        .findActiveByNormalizedAliasNames(
                                Set.copyOf(normalizedByAliasName.values())
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                match -> match.normalizedAliasName(),
                                match -> match.world(),
                                (existing, duplicated) -> existing
                        ));

        Map<String, CanonicalWorld> resolved = new LinkedHashMap<>();

        normalizedByAliasName.forEach((aliasName, normalizedAliasName) -> {
            CanonicalWorldRow row =
                    worldByNormalizedAliasName.get(normalizedAliasName);

            if (row != null) {
                resolved.put(aliasName, toCanonicalWorld(row));
            }
        });

        return Collections.unmodifiableMap(resolved);
    }

    private Map<String, String> normalizeAll(Collection<String> aliasNames) {
        if (aliasNames == null || aliasNames.isEmpty()) {
            return Map.of();
        }

        Map<String, String> normalizedByAliasName = new LinkedHashMap<>();

        for (String aliasName : aliasNames) {
            if (aliasName == null || aliasName.isBlank()) {
                continue;
            }

            normalizedByAliasName.putIfAbsent(
                    aliasName,
                    aliasName.trim().toLowerCase(Locale.ROOT)
            );
        }

        return normalizedByAliasName;
    }

    private CanonicalWorld toCanonicalWorld(CanonicalWorldRow row) {
        return new CanonicalWorld(
                row.worldSlug(),
                row.worldName(),
                row.status(),
                row.displayOrder()
        );
    }
}
