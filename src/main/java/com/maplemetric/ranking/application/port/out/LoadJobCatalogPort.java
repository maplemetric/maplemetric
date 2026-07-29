package com.maplemetric.ranking.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadJobCatalogPort {

    Optional<CanonicalJobRow> findBySlug(String jobSlug);

    List<CanonicalJobRow> findAllOrderByDisplayOrder();

    List<AliasMatch> findActiveByNormalizedAliasNames(
            Collection<String> normalizedAliasNames
    );

    record CanonicalJobRow(
            String jobSlug,
            String jobName,
            String jobGroup,
            String jobBranch,
            boolean available,
            int displayOrder
    ) {
    }

    record AliasMatch(
            String normalizedAliasName,
            CanonicalJobRow job
    ) {
    }
}
