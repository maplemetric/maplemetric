package com.maplemetric.ranking.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.maplemetric.ranking.application.port.out.LoadJobCatalogPort.AliasMatch;
import com.maplemetric.ranking.application.port.out.LoadJobCatalogPort.CanonicalJobRow;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest(
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true"
        }
)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(JobCatalogPersistenceAdapter.class)
class JobCatalogPersistenceAdapterTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final Instant DELETED_AT =
            Instant.parse("2026-07-29T00:00:00Z");

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private JobCatalogPersistenceAdapter adapter;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobAliasRepository jobAliasRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void Seed된Canonical직업을Slug로조회한다() {
        assertThat(adapter.findBySlug("hero"))
                .isPresent()
                .get()
                .extracting(row -> row.jobName())
                .isEqualTo("히어로");
    }

    @Test
    void 존재하지않는Slug는빈결과를반환한다() {
        assertThat(adapter.findBySlug("존재하지-않는-슬러그")).isEmpty();
    }

    @Test
    void Seed된Canonical직업48개를표시순서대로반환한다() {
        List<CanonicalJobRow> rows = adapter.findAllOrderByDisplayOrder();

        assertThat(rows).hasSize(48);
        assertThat(rows)
                .extracting(row -> row.displayOrder())
                .isSorted();
        assertThat(rows)
                .extracting(row -> row.jobSlug())
                .doesNotHaveDuplicates();
    }

    @Test
    void SoftDelete된직업은Slug조회와목록에서제외한다() {
        JobEntity job = createJob("삭제대상직업", "deleted-job");

        assertThat(adapter.findBySlug("deleted-job")).isPresent();

        job.softDelete(DELETED_AT);
        jobRepository.saveAndFlush(job);
        entityManager.clear();

        assertThat(adapter.findBySlug("deleted-job")).isEmpty();
        assertThat(adapter.findAllOrderByDisplayOrder())
                .extracting(row -> row.jobSlug())
                .doesNotContain("deleted-job");
    }

    @Test
    void 원본이름집합을한번의조회로Canonical직업에해석한다() {
        List<AliasMatch> matches = adapter.findActiveByNormalizedAliasNames(
                Set.of("히어로", "비숍")
        );

        assertThat(matches).hasSize(2);
        assertThat(matches)
                .extracting(match -> match.job().jobSlug())
                .containsExactlyInAnyOrder("hero", "bishop");
    }

    @Test
    void 여러Alias가같은Canonical직업을가리키면각각반환한다() {
        JobEntity job = createJob("다중Alias직업", "multi-alias-job");

        jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "다중별칭1", JobAliasType.NEXON)
        );
        jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "다중별칭2", JobAliasType.HISTORICAL)
        );
        entityManager.clear();

        List<AliasMatch> matches = adapter.findActiveByNormalizedAliasNames(
                Set.of("다중별칭1", "다중별칭2")
        );

        assertThat(matches).hasSize(2);
        assertThat(matches)
                .extracting(match -> match.job().jobSlug())
                .containsOnly("multi-alias-job");
    }

    @Test
    void 미매칭이름은결과에포함하지않는다() {
        List<AliasMatch> matches = adapter.findActiveByNormalizedAliasNames(
                Set.of("히어로", "등록되지않은직업")
        );

        assertThat(matches)
                .extracting(match -> match.normalizedAliasName())
                .containsExactly("히어로");
    }

    @Test
    void 대소문자와앞뒤공백이다른Alias도정규화이름으로해석한다() {
        JobEntity job = createJob("정규화직업", "normalized-job");

        jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "  MixedCase  ", JobAliasType.NEXON)
        );
        entityManager.clear();

        List<AliasMatch> matches = adapter.findActiveByNormalizedAliasNames(
                Set.of("mixedcase")
        );

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).normalizedAliasName())
                .isEqualTo("mixedcase");
        assertThat(matches.get(0).job().jobSlug())
                .isEqualTo("normalized-job");
    }

    @Test
    void SoftDelete된Alias는해석대상에서제외한다() {
        JobEntity job = createJob("삭제Alias직업", "deleted-alias-job");

        JobAliasEntity alias = jobAliasRepository.saveAndFlush(
                JobAliasEntity.create(job, "삭제될별칭", JobAliasType.NEXON)
        );

        alias.softDelete(DELETED_AT);
        jobAliasRepository.saveAndFlush(alias);
        entityManager.clear();

        assertThat(
                adapter.findActiveByNormalizedAliasNames(Set.of("삭제될별칭"))
        ).isEmpty();
    }

    @Test
    void 빈이름집합은조회하지않고빈결과를반환한다() {
        assertThat(adapter.findActiveByNormalizedAliasNames(Set.of()))
                .isEmpty();
    }

    private JobEntity createJob(String jobName, String jobSlug) {
        JobEntity job = jobRepository.saveAndFlush(
                JobEntity.create(
                        jobName,
                        jobSlug,
                        null,
                        "테스트계열",
                        "전사",
                        true,
                        900
                )
        );

        entityManager.clear();

        return jobRepository.findById(job.getId()).orElseThrow();
    }
}
