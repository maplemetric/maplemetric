package com.maplemetric.ranking.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.OverallRankingCollection;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.RankingRow;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
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
@Import(OverallRankingSnapshotPersistenceAdapter.class)
class OverallRankingSnapshotPersistenceAdapterTest {

    private static final String POSTGRES_IMAGE =
            "postgres:17-alpine";

    private static final LocalDate SNAPSHOT_DATE =
            LocalDate.of(2026, 7, 24);

    private static final Instant COLLECTED_AT =
            Instant.parse("2026-07-24T01:00:00Z");

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private OverallRankingSnapshotPersistenceAdapter adapter;

    @Autowired
    private OverallRankingCollectionJpaRepository collectionRepository;

    @Autowired
    private OverallRankingSnapshotJpaRepository snapshotRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 종합랭킹스냅샷을저장하고순위순서로조회한다() {
        adapter.saveOverallRankingSnapshot(
                createCollection(
                        null,
                        null,
                        null,
                        List.of(
                                createRow(2, "둘째"),
                                createRow(1, "첫째")
                        )
                )
        );

        entityManager.flush();
        entityManager.clear();

        List<OverallRankingCollectionEntity> collections =
                collectionRepository.findAll();

        assertThat(collections).hasSize(1);

        OverallRankingCollectionEntity collection =
                collections.get(0);

        assertThat(collection.getWorldName()).isEqualTo("ALL");
        assertThat(collection.getWorldType()).isEqualTo(-1);
        assertThat(collection.getClassName()).isEqualTo("ALL");
        assertThat(collection.getPageCount()).isEqualTo(1);
        assertThat(collection.getSampleSize()).isEqualTo(2);
        assertThat(collection.getSource()).isEqualTo("NEXON_OPEN_API");

        List<OverallRankingSnapshotEntity> rows =
                snapshotRepository
                        .findByCollectionIdOrderByRankingAsc(
                                collection.getId()
                        );

        assertThat(rows)
                .extracting(row -> row.getRanking())
                .containsExactly(1, 2);

        assertThat(rows.get(0).getCharacterName())
                .isEqualTo("첫째");
    }

    @Test
    void 필터가있으면원본값그대로저장한다() {
        adapter.saveOverallRankingSnapshot(
                createCollection(
                        "루나",
                        0,
                        "팬텀",
                        List.of(createRow(1, "감점"))
                )
        );

        entityManager.flush();
        entityManager.clear();

        OverallRankingCollectionEntity collection =
                collectionRepository.findAll().get(0);

        assertThat(collection.getWorldName()).isEqualTo("루나");
        assertThat(collection.getWorldType()).isEqualTo(0);
        assertThat(collection.getClassName()).isEqualTo("팬텀");
    }

    @Test
    void 정규화된필터기준으로존재여부를판별한다() {
        adapter.saveOverallRankingSnapshot(
                createCollection(
                        null,
                        null,
                        null,
                        List.of(createRow(1, "감점"))
                )
        );

        entityManager.flush();

        assertThat(
                adapter.existsOverallRankingCollection(
                        SNAPSHOT_DATE,
                        null,
                        null,
                        null
                )
        ).isTrue();

        assertThat(
                adapter.existsOverallRankingCollection(
                        SNAPSHOT_DATE.plusDays(1),
                        null,
                        null,
                        null
                )
        ).isFalse();

        assertThat(
                adapter.existsOverallRankingCollection(
                        SNAPSHOT_DATE,
                        "루나",
                        null,
                        null
                )
        ).isFalse();
    }

    @Test
    void 동일기준일과조건의중복수집은저장에실패한다() {
        adapter.saveOverallRankingSnapshot(
                createCollection(
                        "루나",
                        0,
                        "팬텀",
                        List.of(createRow(1, "감점"))
                )
        );

        entityManager.flush();

        assertThatThrownBy(
                () -> adapter.saveOverallRankingSnapshot(
                        createCollection(
                                "루나",
                                0,
                                "팬텀",
                                List.of(createRow(1, "다른감점"))
                        )
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    private OverallRankingCollection createCollection(
            String worldName,
            Integer worldType,
            String className,
            List<RankingRow> rows
    ) {
        return new OverallRankingCollection(
                SNAPSHOT_DATE,
                worldName,
                worldType,
                className,
                "NEXON_OPEN_API",
                1,
                COLLECTED_AT,
                rows
        );
    }

    private RankingRow createRow(
            int ranking,
            String characterName
    ) {
        return new RankingRow(
                ranking,
                characterName,
                "루나",
                "팬텀",
                null,
                200,
                0L,
                0,
                null
        );
    }
}
