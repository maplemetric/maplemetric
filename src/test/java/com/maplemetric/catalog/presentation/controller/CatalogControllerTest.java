package com.maplemetric.catalog.presentation.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maplemetric.catalog.application.result.GetJobCatalogResult;
import com.maplemetric.catalog.application.result.GetWorldCatalogResult;
import com.maplemetric.catalog.application.service.CatalogQueryService;
import com.maplemetric.world.api.CanonicalWorld;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogQueryService catalogQueryService;

    @Test
    void 직업기준정보응답을반환한다() throws Exception {
        given(catalogQueryService.getJobCatalog())
                .willReturn(new GetJobCatalogResult(List.of(
                        new GetJobCatalogResult.JobResult(
                                "hero",
                                "히어로",
                                "모험가",
                                "전사",
                                true,
                                10,
                                "/assets/jobs/hero/icon.webp"
                        )
                )));

        mockMvc.perform(get("/api/v1/catalog/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.code")
                                .value("JOB_CATALOG_SEARCH_SUCCESS")
                )
                .andExpect(jsonPath("$.data.jobs.length()").value(1))
                .andExpect(jsonPath("$.data.jobs[0].jobSlug").value("hero"))
                .andExpect(jsonPath("$.data.jobs[0].jobName").value("히어로"))
                .andExpect(jsonPath("$.data.jobs[0].jobGroup").value("모험가"))
                .andExpect(jsonPath("$.data.jobs[0].jobBranch").value("전사"))
                .andExpect(jsonPath("$.data.jobs[0].available").value(true))
                .andExpect(jsonPath("$.data.jobs[0].displayOrder").value(10))
                .andExpect(
                        jsonPath("$.data.jobs[0].iconUrl")
                                .value("/assets/jobs/hero/icon.webp")
                );
    }

    @Test
    void 월드기준정보응답을반환한다() throws Exception {
        given(catalogQueryService.getWorldCatalog())
                .willReturn(new GetWorldCatalogResult(List.of(
                        new GetWorldCatalogResult.WorldResult(
                                "luna",
                                "루나",
                                CanonicalWorld.Status.ACTIVE,
                                10,
                                "/assets/worlds/luna/logo.webp"
                        )
                )));

        mockMvc.perform(get("/api/v1/catalog/worlds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.code")
                                .value("WORLD_CATALOG_SEARCH_SUCCESS")
                )
                .andExpect(jsonPath("$.data.worlds.length()").value(1))
                .andExpect(
                        jsonPath("$.data.worlds[0].worldSlug").value("luna")
                )
                .andExpect(
                        jsonPath("$.data.worlds[0].worldName").value("루나")
                )
                .andExpect(
                        jsonPath("$.data.worlds[0].status").value("ACTIVE")
                )
                .andExpect(
                        jsonPath("$.data.worlds[0].displayOrder").value(10)
                )
                .andExpect(
                        jsonPath("$.data.worlds[0].logoUrl")
                                .value("/assets/worlds/luna/logo.webp")
                );
    }

    @Test
    void Catalog가비어있어도200과빈배열을반환한다() throws Exception {
        given(catalogQueryService.getJobCatalog())
                .willReturn(new GetJobCatalogResult(List.of()));

        mockMvc.perform(get("/api/v1/catalog/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobs.length()").value(0));
    }
}
