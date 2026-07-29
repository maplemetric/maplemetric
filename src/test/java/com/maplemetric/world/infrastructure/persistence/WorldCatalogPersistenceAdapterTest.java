package com.maplemetric.world.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.maplemetric.world.api.CanonicalWorld;
import com.maplemetric.world.application.port.out.LoadWorldCatalogPort.AliasMatch;
import com.maplemetric.world.application.port.out.LoadWorldCatalogPort.CanonicalWorldRow;
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
@Import(WorldCatalogPersistenceAdapter.class)
class WorldCatalogPersistenceAdapterTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final Instant DELETED_AT =
            Instant.parse("2026-07-29T00:00:00Z");

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private WorldCatalogPersistenceAdapter adapter;

    @Autowired
    private WorldRepository worldRepository;

    @Autowired
    private WorldAliasRepository worldAliasRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void Seed된Canonical월드를Slug로조회한다() {
        assertThat(adapter.findBySlug("luna"))
                .isPresent()
                .get()
                .extracting(row -> row.worldName())
                .isEqualTo("루나");
    }

    @Test
    void 존재하지않는Slug는빈결과를반환한다() {
        assertThat(adapter.findBySlug("존재하지-않는-슬러그")).isEmpty();
    }

    @Test
    void Seed된Canonical월드14개를표시순서대로반환한다() {
        List<CanonicalWorldRow> rows = adapter.findAllOrderByDisplayOrder();

        assertThat(rows).hasSize(14);
        assertThat(rows)
                .extracting(row -> row.displayOrder())
                .isSorted();
        assertThat(rows)
                .extracting(row -> row.worldSlug())
                .doesNotHaveDuplicates();
    }

    @Test
    void 종료상태월드도Status를그대로반환한다() {
        createWorld("종료월드", "closed-world", WorldEntity.Status.CLOSED);

        assertThat(adapter.findBySlug("closed-world"))
                .isPresent()
                .get()
                .extracting(row -> row.status())
                .isEqualTo(CanonicalWorld.Status.CLOSED);
    }

    @Test
    void SoftDelete된월드는Slug조회와목록에서제외한다() {
        WorldEntity world = createWorld(
                "삭제대상월드",
                "deleted-world",
                WorldEntity.Status.ACTIVE
        );

        assertThat(adapter.findBySlug("deleted-world")).isPresent();

        world.softDelete(DELETED_AT);
        worldRepository.saveAndFlush(world);
        entityManager.clear();

        assertThat(adapter.findBySlug("deleted-world")).isEmpty();
        assertThat(adapter.findAllOrderByDisplayOrder())
                .extracting(row -> row.worldSlug())
                .doesNotContain("deleted-world");
    }

    @Test
    void 원본이름집합을한번의조회로Canonical월드에해석한다() {
        List<AliasMatch> matches = adapter.findActiveByNormalizedAliasNames(
                Set.of("루나", "스카니아")
        );

        assertThat(matches).hasSize(2);
        assertThat(matches)
                .extracting(match -> match.world().worldSlug())
                .containsExactlyInAnyOrder("luna", "scania");
    }

    @Test
    void 여러Alias가같은Canonical월드를가리키면각각반환한다() {
        WorldEntity world = createWorld(
                "다중Alias월드",
                "multi-alias-world",
                WorldEntity.Status.ACTIVE
        );

        worldAliasRepository.saveAndFlush(
                WorldAliasEntity.create(world, "다중월드1", WorldAliasType.NEXON)
        );
        worldAliasRepository.saveAndFlush(
                WorldAliasEntity.create(
                        world,
                        "다중월드2",
                        WorldAliasType.HISTORICAL
                )
        );
        entityManager.clear();

        List<AliasMatch> matches = adapter.findActiveByNormalizedAliasNames(
                Set.of("다중월드1", "다중월드2")
        );

        assertThat(matches).hasSize(2);
        assertThat(matches)
                .extracting(match -> match.world().worldSlug())
                .containsOnly("multi-alias-world");
    }

    @Test
    void 미매칭이름은결과에포함하지않는다() {
        List<AliasMatch> matches = adapter.findActiveByNormalizedAliasNames(
                Set.of("루나", "등록되지않은월드")
        );

        assertThat(matches)
                .extracting(match -> match.normalizedAliasName())
                .containsExactly("루나");
    }

    @Test
    void 대소문자와앞뒤공백이다른Alias도정규화이름으로해석한다() {
        WorldEntity world = createWorld(
                "정규화월드",
                "normalized-world",
                WorldEntity.Status.ACTIVE
        );

        worldAliasRepository.saveAndFlush(
                WorldAliasEntity.create(
                        world,
                        "  MixedWorld  ",
                        WorldAliasType.NEXON
                )
        );
        entityManager.clear();

        List<AliasMatch> matches = adapter.findActiveByNormalizedAliasNames(
                Set.of("mixedworld")
        );

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).normalizedAliasName())
                .isEqualTo("mixedworld");
        assertThat(matches.get(0).world().worldSlug())
                .isEqualTo("normalized-world");
    }

    @Test
    void SoftDelete된Alias는해석대상에서제외한다() {
        WorldEntity world = createWorld(
                "삭제Alias월드",
                "deleted-alias-world",
                WorldEntity.Status.ACTIVE
        );

        WorldAliasEntity alias = worldAliasRepository.saveAndFlush(
                WorldAliasEntity.create(world, "삭제될월드별칭", WorldAliasType.NEXON)
        );

        alias.softDelete(DELETED_AT);
        worldAliasRepository.saveAndFlush(alias);
        entityManager.clear();

        assertThat(
                adapter.findActiveByNormalizedAliasNames(Set.of("삭제될월드별칭"))
        ).isEmpty();
    }

    @Test
    void 빈이름집합은조회하지않고빈결과를반환한다() {
        assertThat(adapter.findActiveByNormalizedAliasNames(Set.of()))
                .isEmpty();
    }

    private WorldEntity createWorld(
            String worldName,
            String worldSlug,
            WorldEntity.Status status
    ) {
        WorldEntity world = worldRepository.saveAndFlush(
                WorldEntity.create(
                        worldName,
                        worldSlug,
                        null,
                        900,
                        status
                )
        );

        entityManager.clear();

        return worldRepository.findById(world.getId()).orElseThrow();
    }
}
