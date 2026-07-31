package com.maplemetric.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.maplemetric.catalog.application.result.GetJobCatalogResult;
import com.maplemetric.catalog.application.result.GetWorldCatalogResult;
import com.maplemetric.ranking.api.CanonicalJob;
import com.maplemetric.ranking.api.JobCatalogQuery;
import com.maplemetric.world.api.CanonicalWorld;
import com.maplemetric.world.api.WorldCatalogQuery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class CatalogQueryServiceTest {

    @Mock
    private JobCatalogQuery jobCatalogQuery;

    @Mock
    private WorldCatalogQuery worldCatalogQuery;

    private CatalogQueryService service;

    @BeforeEach
    void setUp() {
        service = new CatalogQueryService(
                jobCatalogQuery,
                worldCatalogQuery
        );
    }

    @Test
    void 직업기준정보는Catalog순서를유지하고아이콘경로를Slug에서만든다() {
        given(jobCatalogQuery.findAll())
                .willReturn(List.of(
                        new CanonicalJob(
                                "hero",
                                "히어로",
                                "모험가",
                                "전사",
                                true,
                                10
                        ),
                        new CanonicalJob(
                                "zero",
                                "제로",
                                "제로",
                                null,
                                false,
                                20
                        )
                ));

        GetJobCatalogResult result = service.getJobCatalog();

        assertThat(result.jobs())
                .extracting(
                        job -> job.jobSlug(),
                        job -> job.jobBranch(),
                        job -> job.available(),
                        job -> job.displayOrder(),
                        job -> job.iconUrl()
                )
                .containsExactly(
                        tuple(
                                "hero",
                                "전사",
                                true,
                                10,
                                "/assets/jobs/hero/icon.webp"
                        ),
                        tuple(
                                "zero",
                                null,
                                false,
                                20,
                                "/assets/jobs/zero/icon.webp"
                        )
                );

        verify(jobCatalogQuery).findAll();
        verifyNoInteractions(worldCatalogQuery);
    }

    @Test
    void 월드기준정보는상태와로고경로를제공한다() {
        given(worldCatalogQuery.findAll())
                .willReturn(List.of(
                        new CanonicalWorld(
                                "luna",
                                "루나",
                                CanonicalWorld.Status.ACTIVE,
                                10
                        ),
                        new CanonicalWorld(
                                "reboot",
                                "리부트",
                                CanonicalWorld.Status.CLOSED,
                                20
                        )
                ));

        GetWorldCatalogResult result = service.getWorldCatalog();

        assertThat(result.worlds())
                .extracting(
                        world -> world.worldSlug(),
                        world -> world.status(),
                        world -> world.displayOrder(),
                        world -> world.logoUrl()
                )
                .containsExactly(
                        tuple(
                                "luna",
                                CanonicalWorld.Status.ACTIVE,
                                10,
                                "/assets/worlds/luna/logo.webp"
                        ),
                        tuple(
                                "reboot",
                                CanonicalWorld.Status.CLOSED,
                                20,
                                "/assets/worlds/reboot/logo.webp"
                        )
                );

        verify(worldCatalogQuery).findAll();
        verifyNoInteractions(jobCatalogQuery);
    }

    @Test
    void Catalog가비어있어도빈목록을반환한다() {
        given(jobCatalogQuery.findAll()).willReturn(List.of());
        given(worldCatalogQuery.findAll()).willReturn(List.of());

        assertThat(service.getJobCatalog().jobs()).isEmpty();
        assertThat(service.getWorldCatalog().worlds()).isEmpty();
    }

    @Test
    void Slug가비어있으면Asset경로를만들지않는다() {
        given(jobCatalogQuery.findAll())
                .willReturn(List.of(
                        new CanonicalJob(
                                null,
                                "미배정",
                                "모험가",
                                null,
                                true,
                                10
                        )
                ));
        given(worldCatalogQuery.findAll())
                .willReturn(List.of(
                        new CanonicalWorld(
                                "  ",
                                "미배정",
                                CanonicalWorld.Status.ACTIVE,
                                10
                        )
                ));

        assertThat(service.getJobCatalog().jobs().get(0).iconUrl())
                .isNull();
        assertThat(service.getWorldCatalog().worlds().get(0).logoUrl())
                .isNull();
    }

    @Test
    void 조회서비스는readOnlyTransaction경계다() {
        Transactional transactional =
                CatalogQueryService.class
                        .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }
}
