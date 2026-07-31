package com.maplemetric.catalog.presentation.controller;

import com.maplemetric.catalog.application.result.GetJobCatalogResult;
import com.maplemetric.catalog.application.result.GetWorldCatalogResult;
import com.maplemetric.catalog.application.service.CatalogQueryService;
import com.maplemetric.catalog.presentation.code.CatalogSuccessCode;
import com.maplemetric.catalog.presentation.response.GetJobCatalogResponse;
import com.maplemetric.catalog.presentation.response.GetWorldCatalogResponse;
import com.maplemetric.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final CatalogQueryService catalogQueryService;

    public CatalogController(CatalogQueryService catalogQueryService) {
        this.catalogQueryService = catalogQueryService;
    }

    @GetMapping("/jobs")
    public ApiResponse<GetJobCatalogResponse> getJobCatalog() {
        GetJobCatalogResult result = catalogQueryService.getJobCatalog();

        return ApiResponse.ok(
                CatalogSuccessCode.JOB_CATALOG_SEARCH_SUCCESS,
                GetJobCatalogResponse.from(result)
        );
    }

    @GetMapping("/worlds")
    public ApiResponse<GetWorldCatalogResponse> getWorldCatalog() {
        GetWorldCatalogResult result = catalogQueryService.getWorldCatalog();

        return ApiResponse.ok(
                CatalogSuccessCode.WORLD_CATALOG_SEARCH_SUCCESS,
                GetWorldCatalogResponse.from(result)
        );
    }
}
