package com.maplemetric.catalog.application.service;

import com.maplemetric.catalog.application.result.GetJobCatalogResult;
import com.maplemetric.catalog.application.result.GetWorldCatalogResult;
import com.maplemetric.ranking.api.JobCatalogQuery;
import com.maplemetric.world.api.WorldCatalogQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Canonical 기준정보 목록을 제공한다.
 *
 * 최신 Snapshot에 등장한 이름을 Catalog로 자동 승격하지 않는다. Source는 승인된
 * Canonical Seed뿐이며, 정렬과 삭제 필터는 각 모듈의 Catalog 계약이 보장한다.
 */
@Service
@Transactional(readOnly = true)
public class CatalogQueryService {

    private final JobCatalogQuery jobCatalogQuery;
    private final WorldCatalogQuery worldCatalogQuery;

    public CatalogQueryService(
            JobCatalogQuery jobCatalogQuery,
            WorldCatalogQuery worldCatalogQuery
    ) {
        this.jobCatalogQuery = jobCatalogQuery;
        this.worldCatalogQuery = worldCatalogQuery;
    }

    public GetJobCatalogResult getJobCatalog() {
        return GetJobCatalogResult.from(jobCatalogQuery.findAll());
    }

    public GetWorldCatalogResult getWorldCatalog() {
        return GetWorldCatalogResult.from(worldCatalogQuery.findAll());
    }
}
