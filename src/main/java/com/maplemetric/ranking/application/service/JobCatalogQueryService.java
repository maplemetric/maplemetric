package com.maplemetric.ranking.application.service;

import com.maplemetric.ranking.api.CanonicalJob;
import com.maplemetric.ranking.api.JobCatalogQuery;
import com.maplemetric.ranking.application.port.out.LoadJobCatalogPort;
import com.maplemetric.ranking.application.port.out.LoadJobCatalogPort.CanonicalJobRow;
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
public class JobCatalogQueryService implements JobCatalogQuery {

    private final LoadJobCatalogPort loadJobCatalogPort;

    JobCatalogQueryService(LoadJobCatalogPort loadJobCatalogPort) {
        this.loadJobCatalogPort = loadJobCatalogPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CanonicalJob> findBySlug(String jobSlug) {
        if (jobSlug == null || jobSlug.isBlank()) {
            return Optional.empty();
        }

        return loadJobCatalogPort
                .findBySlug(jobSlug.trim())
                .map(row -> toCanonicalJob(row));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanonicalJob> findAll() {
        return loadJobCatalogPort.findAllOrderByDisplayOrder()
                .stream()
                .map(row -> toCanonicalJob(row))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, CanonicalJob> resolveAliases(
            Collection<String> aliasNames
    ) {
        Map<String, String> normalizedByAliasName =
                normalizeAll(aliasNames);

        if (normalizedByAliasName.isEmpty()) {
            return Map.of();
        }

        Map<String, CanonicalJobRow> jobByNormalizedAliasName =
                loadJobCatalogPort
                        .findActiveByNormalizedAliasNames(
                                Set.copyOf(normalizedByAliasName.values())
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                match -> match.normalizedAliasName(),
                                match -> match.job(),
                                (existing, duplicated) -> existing
                        ));

        Map<String, CanonicalJob> resolved = new LinkedHashMap<>();

        normalizedByAliasName.forEach((aliasName, normalizedAliasName) -> {
            CanonicalJobRow row =
                    jobByNormalizedAliasName.get(normalizedAliasName);

            if (row != null) {
                resolved.put(aliasName, toCanonicalJob(row));
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

    private CanonicalJob toCanonicalJob(CanonicalJobRow row) {
        return new CanonicalJob(
                row.jobSlug(),
                row.jobName(),
                row.jobGroup(),
                row.jobBranch(),
                row.available(),
                row.displayOrder()
        );
    }
}
