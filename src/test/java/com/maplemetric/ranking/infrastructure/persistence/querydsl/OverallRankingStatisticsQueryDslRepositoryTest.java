package com.maplemetric.ranking.infrastructure.persistence.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maplemetric.common.config.QuerydslConfig;
import com.maplemetric.ranking.infrastructure.persistence.OverallRankingCollectionEntity;
import com.maplemetric.ranking.infrastructure.persistence.OverallRankingCollectionJpaRepository;
import com.maplemetric.ranking.infrastructure.persistence.OverallRankingSnapshotEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
@Import({
        QuerydslConfig.class,
        OverallRankingStatisticsQueryDslRepositoryImpl.class
})
class OverallRankingStatisticsQueryDslRepositoryTest {

    private static final String POSTGRES_IMAGE =
            "postgres:17-alpine";

    private static final Instant COLLECTED_AT =
            Instant.parse("2026-07-24T01:00:00Z");

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private OverallRankingStatisticsQueryDslRepository repository;

    @Autowired
    private OverallRankingCollectionJpaRepository collectionRepository;

    @Test
    void 전체조건중가장최신기준일의Collection을선택한다() {
        saveAllConditionCollection(
                LocalDate.of(2026, 7, 22),
                new Row[] {row(1, "히어로", null, 200)}
        );

        saveAllConditionCollection(
                LocalDate.of(2026, 7, 24),
                new Row[] {row(1, "히어로", null, 210)}
        );

        saveAllConditionCollection(
                LocalDate.of(2026, 7, 23),
                new Row[] {row(1, "히어로", null, 205)}
        );

        Optional<OverallRankingCollectionEntity> latest =
                repository.findLatestAllConditionCollection();

        assertThat(latest).isPresent();
        assertThat(latest.get().getSnapshotDate())
                .isEqualTo(LocalDate.of(2026, 7, 24));
    }

    @Test
    void 필터가있는Collection은전체조건선택대상에서제외한다() {
        OverallRankingCollectionEntity collection =
                OverallRankingCollectionEntity.create(
                        LocalDate.of(2026, 7, 25),
                        "루나",
                        0,
                        "팬텀",
                        "NEXON_OPEN_API",
                        1,
                        100,
                        false,
                        1,
                        COLLECTED_AT
                );

        collection.addSnapshot(
                OverallRankingSnapshotEntity.create(
                        collection,
                        1,
                        "감점",
                        "루나",
                        "팬텀",
                        null,
                        200,
                        0L,
                        0,
                        null
                )
        );

        collectionRepository.saveAndFlush(collection);

        Optional<OverallRankingCollectionEntity> latest =
                repository.findLatestAllConditionCollection();

        assertThat(latest).isEmpty();
    }

    @Test
    void 기준일보다이전인Collection중가장가까운Collection을선택한다() {
        saveAllConditionCollection(
                LocalDate.of(2026, 7, 21),
                new Row[] {row(1, "히어로", null, 200)}
        );

        saveAllConditionCollection(
                LocalDate.of(2026, 7, 23),
                new Row[] {row(1, "히어로", null, 205)}
        );

        saveAllConditionCollection(
                LocalDate.of(2026, 7, 24),
                new Row[] {row(1, "히어로", null, 210)}
        );

        Optional<OverallRankingCollectionEntity> previous =
                repository.findPreviousAllConditionCollection(
                        LocalDate.of(2026, 7, 24)
                );

        assertThat(previous).isPresent();
        assertThat(previous.get().getSnapshotDate())
                .isEqualTo(LocalDate.of(2026, 7, 23));
    }

    @Test
    void 전일Collection이없으면더이전의가장가까운Collection을선택한다() {
        saveAllConditionCollection(
                LocalDate.of(2026, 7, 18),
                new Row[] {row(1, "히어로", null, 200)}
        );

        saveAllConditionCollection(
                LocalDate.of(2026, 7, 21),
                new Row[] {row(1, "히어로", null, 205)}
        );

        saveAllConditionCollection(
                LocalDate.of(2026, 7, 24),
                new Row[] {row(1, "히어로", null, 210)}
        );

        Optional<OverallRankingCollectionEntity> previous =
                repository.findPreviousAllConditionCollection(
                        LocalDate.of(2026, 7, 24)
                );

        assertThat(previous).isPresent();
        assertThat(previous.get().getSnapshotDate())
                .isEqualTo(LocalDate.of(2026, 7, 21));
    }

    @Test
    void 기준일과같거나이후인Collection은이전선택대상에서제외한다() {
        saveAllConditionCollection(
                LocalDate.of(2026, 7, 24),
                new Row[] {row(1, "히어로", null, 210)}
        );

        saveAllConditionCollection(
                LocalDate.of(2026, 7, 25),
                new Row[] {row(1, "히어로", null, 215)}
        );

        Optional<OverallRankingCollectionEntity> previous =
                repository.findPreviousAllConditionCollection(
                        LocalDate.of(2026, 7, 24)
                );

        assertThat(previous).isEmpty();
    }

    @Test
    void 필터가있는Collection은이전선택대상에서제외한다() {
        OverallRankingCollectionEntity collection =
                OverallRankingCollectionEntity.create(
                        LocalDate.of(2026, 7, 23),
                        "루나",
                        0,
                        "팬텀",
                        "NEXON_OPEN_API",
                        1,
                        100,
                        false,
                        1,
                        COLLECTED_AT
                );

        collection.addSnapshot(
                OverallRankingSnapshotEntity.create(
                        collection,
                        1,
                        "감점",
                        "루나",
                        "팬텀",
                        null,
                        200,
                        0L,
                        0,
                        null
                )
        );

        collectionRepository.saveAndFlush(collection);

        Optional<OverallRankingCollectionEntity> previous =
                repository.findPreviousAllConditionCollection(
                        LocalDate.of(2026, 7, 24)
                );

        assertThat(previous).isEmpty();
    }

    @Test
    void 이전Collection이없으면빈결과를반환한다() {
        saveAllConditionCollection(
                LocalDate.of(2026, 7, 24),
                new Row[] {row(1, "히어로", null, 210)}
        );

        Optional<OverallRankingCollectionEntity> previous =
                repository.findPreviousAllConditionCollection(
                        LocalDate.of(2026, 7, 24)
                );

        assertThat(previous).isEmpty();
    }

    @Test
    void 같은기준일의전체조건Collection중복저장은Unique제약으로실패한다() {
        saveAllConditionCollection(
                LocalDate.of(2026, 7, 24),
                new Row[] {row(1, "히어로", null, 210)}
        );

        assertThatThrownBy(() -> saveAllConditionCollection(
                LocalDate.of(2026, 7, 24),
                new Row[] {row(1, "팬텀", null, 215)}
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void subClassName이유효하면우선하고없으면className으로집계한다() {
        OverallRankingCollectionEntity collection =
                saveAllConditionCollection(
                        LocalDate.of(2026, 7, 24),
                        new Row[] {
                                row(1, "마법사", "비숍", 200),
                                row(2, "마법사", "비숍", 210),
                                row(3, "비숍", null, 220),
                                row(4, "히어로", "", 230),
                                row(5, "팔라딘", "   ", 240),
                                row(6, "나이트로드", "\t", 250),
                                row(7, "아크메이지(불,독)", "\n", 260)
                        }
                );

        var aggregates =
                repository.aggregateByClassName(collection.getId());

        assertThat(aggregates)
                .extracting(aggregate -> aggregate.className())
                .containsExactlyInAnyOrder(
                        "비숍",
                        "히어로",
                        "팔라딘",
                        "나이트로드",
                        "아크메이지(불,독)"
                );

        var bishop = aggregates.stream()
                .filter(aggregate -> aggregate.className().equals("비숍"))
                .findFirst()
                .orElseThrow();

        assertThat(bishop.count()).isEqualTo(3);
        assertThat(bishop.averageLevel())
                .isEqualByComparingTo(BigDecimal.valueOf(210));
    }

    @Test
    void count내림차순동률은className오름차순으로정렬한다() {
        OverallRankingCollectionEntity collection =
                saveAllConditionCollection(
                        LocalDate.of(2026, 7, 24),
                        new Row[] {
                                row(1, "히어로", null, 200),
                                row(2, "히어로", null, 205),
                                row(3, "히어로", null, 210),
                                row(4, "데몬슬레이어", null, 220),
                                row(5, "데몬슬레이어", null, 225),
                                row(6, "나이트로드", null, 230),
                                row(7, "나이트로드", null, 235)
                        }
                );

        var aggregates =
                repository.aggregateByClassName(collection.getId());

        assertThat(aggregates)
                .extracting(aggregate -> aggregate.className())
                .containsExactly("히어로", "나이트로드", "데몬슬레이어");
    }

    @Test
    void 평균레벨을BigDecimal로반환한다() {
        OverallRankingCollectionEntity collection =
                saveAllConditionCollection(
                        LocalDate.of(2026, 7, 24),
                        new Row[] {
                                row(1, "히어로", null, 200),
                                row(2, "히어로", null, 201)
                        }
                );

        var aggregates =
                repository.aggregateByClassName(collection.getId());

        assertThat(aggregates).hasSize(1);
        assertThat(aggregates.get(0).averageLevel())
                .isEqualByComparingTo(new BigDecimal("200.5"));
    }

    @Test
    void worldName기준으로집계한다() {
        OverallRankingCollectionEntity collection =
                saveAllConditionCollectionByWorld(
                        LocalDate.of(2026, 7, 24),
                        new WorldRow[] {
                                worldRow(1, "루나", 200),
                                worldRow(2, "루나", 210),
                                worldRow(3, "베라", 220)
                        }
                );

        var aggregates =
                repository.aggregateByWorldName(collection.getId());

        assertThat(aggregates)
                .extracting(aggregate -> aggregate.worldName())
                .containsExactlyInAnyOrder("루나", "베라");

        var luna = aggregates.stream()
                .filter(aggregate -> aggregate.worldName().equals("루나"))
                .findFirst()
                .orElseThrow();

        assertThat(luna.count()).isEqualTo(2);
        assertThat(luna.averageLevel())
                .isEqualByComparingTo(BigDecimal.valueOf(205));
    }

    @Test
    void count내림차순동률은worldName오름차순으로정렬한다() {
        OverallRankingCollectionEntity collection =
                saveAllConditionCollectionByWorld(
                        LocalDate.of(2026, 7, 24),
                        new WorldRow[] {
                                worldRow(1, "루나", 200),
                                worldRow(2, "루나", 205),
                                worldRow(3, "루나", 210),
                                worldRow(4, "베라", 220),
                                worldRow(5, "베라", 225),
                                worldRow(6, "이노시스", 230),
                                worldRow(7, "이노시스", 235)
                        }
                );

        var aggregates =
                repository.aggregateByWorldName(collection.getId());

        assertThat(aggregates)
                .extracting(aggregate -> aggregate.worldName())
                .containsExactly("루나", "베라", "이노시스");
    }

    @Test
    void 월드별평균레벨을BigDecimal로반환한다() {
        OverallRankingCollectionEntity collection =
                saveAllConditionCollectionByWorld(
                        LocalDate.of(2026, 7, 24),
                        new WorldRow[] {
                                worldRow(1, "루나", 200),
                                worldRow(2, "루나", 201)
                        }
                );

        var aggregates =
                repository.aggregateByWorldName(collection.getId());

        assertThat(aggregates).hasSize(1);
        assertThat(aggregates.get(0).averageLevel())
                .isEqualByComparingTo(new BigDecimal("200.5"));
    }

    @Test
    void 양끝날짜를포함한전체조건Collection만조회한다() {
        saveAllConditionCollection(
                LocalDate.of(2026, 7, 22),
                new Row[] {row(1, "히어로", null, 200)}
        );

        saveAllConditionCollection(
                LocalDate.of(2026, 7, 23),
                new Row[] {row(1, "히어로", null, 205)}
        );

        saveAllConditionCollection(
                LocalDate.of(2026, 7, 24),
                new Row[] {row(1, "히어로", null, 210)}
        );

        var collections = repository.findAllConditionCollectionsBetween(
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 24)
        );

        assertThat(collections)
                .extracting(collection -> collection.getSnapshotDate())
                .containsExactly(
                        LocalDate.of(2026, 7, 22),
                        LocalDate.of(2026, 7, 23),
                        LocalDate.of(2026, 7, 24)
                );
    }

    @Test
    void 기간밖의더오래된Collection은제외한다() {
        saveAllConditionCollection(
                LocalDate.of(2026, 7, 20),
                new Row[] {row(1, "히어로", null, 200)}
        );

        saveAllConditionCollection(
                LocalDate.of(2026, 7, 24),
                new Row[] {row(1, "히어로", null, 210)}
        );

        var collections = repository.findAllConditionCollectionsBetween(
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 24)
        );

        assertThat(collections)
                .extracting(collection -> collection.getSnapshotDate())
                .containsExactly(LocalDate.of(2026, 7, 24));
    }

    @Test
    void 기준일보다미래인Collection은제외한다() {
        saveAllConditionCollection(
                LocalDate.of(2026, 7, 24),
                new Row[] {row(1, "히어로", null, 210)}
        );

        saveAllConditionCollection(
                LocalDate.of(2026, 7, 25),
                new Row[] {row(1, "히어로", null, 215)}
        );

        var collections = repository.findAllConditionCollectionsBetween(
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 24)
        );

        assertThat(collections)
                .extracting(collection -> collection.getSnapshotDate())
                .containsExactly(LocalDate.of(2026, 7, 24));
    }

    @Test
    void 월드직업필터가적용된Collection은기간조회대상에서제외한다() {
        saveAllConditionCollection(
                LocalDate.of(2026, 7, 24),
                new Row[] {row(1, "히어로", null, 210)}
        );

        OverallRankingCollectionEntity filtered =
                OverallRankingCollectionEntity.create(
                        LocalDate.of(2026, 7, 23),
                        "루나",
                        0,
                        "팬텀",
                        "NEXON_OPEN_API",
                        1,
                        100,
                        false,
                        1,
                        COLLECTED_AT
                );

        filtered.addSnapshot(
                OverallRankingSnapshotEntity.create(
                        filtered,
                        1,
                        "감점",
                        "루나",
                        "팬텀",
                        null,
                        200,
                        0L,
                        0,
                        null
                )
        );

        collectionRepository.saveAndFlush(filtered);

        var collections = repository.findAllConditionCollectionsBetween(
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 24)
        );

        assertThat(collections)
                .extracting(collection -> collection.getSnapshotDate())
                .containsExactly(LocalDate.of(2026, 7, 24));
    }

    @Test
    void 수집누락일이있으면존재하는Collection만반환한다() {
        saveAllConditionCollection(
                LocalDate.of(2026, 7, 20),
                new Row[] {row(1, "히어로", null, 200)}
        );

        saveAllConditionCollection(
                LocalDate.of(2026, 7, 24),
                new Row[] {row(1, "히어로", null, 210)}
        );

        var collections = repository.findAllConditionCollectionsBetween(
                LocalDate.of(2026, 7, 18),
                LocalDate.of(2026, 7, 24)
        );

        assertThat(collections)
                .extracting(collection -> collection.getSnapshotDate())
                .containsExactly(
                        LocalDate.of(2026, 7, 20),
                        LocalDate.of(2026, 7, 24)
                );
    }

    @Test
    void 기간내대상Collection이없으면빈결과를반환한다() {
        saveAllConditionCollection(
                LocalDate.of(2026, 6, 1),
                new Row[] {row(1, "히어로", null, 200)}
        );

        var collections = repository.findAllConditionCollectionsBetween(
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 24)
        );

        assertThat(collections).isEmpty();
    }

    @Test
    void 여러collectionId의직업집계를한번의조회로반환한다() {
        OverallRankingCollectionEntity first =
                saveAllConditionCollection(
                        LocalDate.of(2026, 7, 23),
                        new Row[] {row(1, "전사", "히어로", 200)}
                );

        OverallRankingCollectionEntity second =
                saveAllConditionCollection(
                        LocalDate.of(2026, 7, 24),
                        new Row[] {
                                row(1, "전사", "히어로", 210),
                                row(2, "팬텀", null, 220)
                        }
                );

        var aggregates = repository.aggregateByClassName(
                List.of(first.getId(), second.getId())
        );

        assertThat(aggregates)
                .filteredOn(aggregate ->
                        aggregate.collectionId().equals(first.getId()))
                .extracting(aggregate -> aggregate.className())
                .containsExactly("히어로");

        assertThat(aggregates)
                .filteredOn(aggregate ->
                        aggregate.collectionId().equals(second.getId()))
                .extracting(aggregate -> aggregate.className())
                .containsExactlyInAnyOrder("히어로", "팬텀");
    }

    @Test
    void 여러collectionId의직업집계도count내림차순className오름차순으로정렬한다() {
        OverallRankingCollectionEntity first =
                saveAllConditionCollection(
                        LocalDate.of(2026, 7, 23),
                        new Row[] {
                                row(1, "데몬슬레이어", null, 200),
                                row(2, "히어로", null, 205),
                                row(3, "히어로", null, 210)
                        }
                );

        OverallRankingCollectionEntity second =
                saveAllConditionCollection(
                        LocalDate.of(2026, 7, 24),
                        new Row[] {
                                row(1, "나이트로드", null, 200),
                                row(2, "나이트로드", null, 205),
                                row(3, "팬텀", null, 210),
                                row(4, "팬텀", null, 215),
                                row(5, "팬텀", null, 220)
                        }
                );

        var aggregates = repository.aggregateByClassName(
                List.of(first.getId(), second.getId())
        );

        assertThat(aggregates)
                .filteredOn(aggregate ->
                        aggregate.collectionId().equals(first.getId()))
                .extracting(aggregate -> aggregate.className())
                .containsExactly("히어로", "데몬슬레이어");

        assertThat(aggregates)
                .filteredOn(aggregate ->
                        aggregate.collectionId().equals(second.getId()))
                .extracting(aggregate -> aggregate.className())
                .containsExactly("팬텀", "나이트로드");
    }

    private OverallRankingCollectionEntity saveAllConditionCollection(
            LocalDate snapshotDate,
            Row[] rows
    ) {
        OverallRankingCollectionEntity collection =
                OverallRankingCollectionEntity.create(
                        snapshotDate,
                        null,
                        null,
                        null,
                        "NEXON_OPEN_API",
                        1,
                        100,
                        false,
                        rows.length,
                        COLLECTED_AT
                );

        for (Row row : rows) {
            collection.addSnapshot(
                    OverallRankingSnapshotEntity.create(
                            collection,
                            row.ranking(),
                            "캐릭터" + row.ranking(),
                            "루나",
                            row.className(),
                            row.subClassName(),
                            row.characterLevel(),
                            0L,
                            0,
                            null
                    )
            );
        }

        return collectionRepository.saveAndFlush(collection);
    }

    private Row row(
            int ranking,
            String className,
            String subClassName,
            int characterLevel
    ) {
        return new Row(ranking, className, subClassName, characterLevel);
    }

    private record Row(
            int ranking,
            String className,
            String subClassName,
            int characterLevel
    ) {
    }

    private OverallRankingCollectionEntity saveAllConditionCollectionByWorld(
            LocalDate snapshotDate,
            WorldRow[] rows
    ) {
        OverallRankingCollectionEntity collection =
                OverallRankingCollectionEntity.create(
                        snapshotDate,
                        null,
                        null,
                        null,
                        "NEXON_OPEN_API",
                        1,
                        100,
                        false,
                        rows.length,
                        COLLECTED_AT
                );

        for (WorldRow row : rows) {
            collection.addSnapshot(
                    OverallRankingSnapshotEntity.create(
                            collection,
                            row.ranking(),
                            "캐릭터" + row.ranking(),
                            row.worldName(),
                            "히어로",
                            null,
                            row.characterLevel(),
                            0L,
                            0,
                            null
                    )
            );
        }

        return collectionRepository.saveAndFlush(collection);
    }

    private WorldRow worldRow(
            int ranking,
            String worldName,
            int characterLevel
    ) {
        return new WorldRow(ranking, worldName, characterLevel);
    }

    private record WorldRow(
            int ranking,
            String worldName,
            int characterLevel
    ) {
    }
}
