package com.maplemetric.ranking.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.maplemetric.ranking.api.OverallRankingRetentionPlan;
import com.maplemetric.ranking.api.OverallRankingRetentionRequest;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.OverallRankingCollection;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.RankingRow;
import com.maplemetric.ranking.application.service.OverallRankingRetentionService;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 만료가 실제 Schema에서 안전하게 동작하는지 확인한다.
 *
 * 삭제는 되돌릴 수 없으므로 대상 산정 경계와 참조 무결성을 실제 DB로 검증한다.
 */
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
        OverallRankingRetentionService.class,
        OverallRankingRetentionPersistenceAdapter.class,
        OverallRankingSnapshotPersistenceAdapter.class
})
class OverallRankingRetentionPersistenceTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final LocalDate OLDEST = LocalDate.of(2026, 7, 1);

    private static final LocalDate MIDDLE = LocalDate.of(2026, 7, 2);

    private static final LocalDate LATEST = LocalDate.of(2026, 7, 3);

    private static final Instant COLLECTED_AT =
            Instant.parse("2026-07-01T01:00:00Z");

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private OverallRankingRetentionService retentionService;

    @Autowired
    private OverallRankingSnapshotPersistenceAdapter saveAdapter;

    @Autowired
    private OverallRankingCollectionJpaRepository collectionRepository;

    @Autowired
    private OverallRankingSnapshotJpaRepository snapshotRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 기준일이전수집만지우고Snapshot도함께지운다() {
        given(OLDEST, MIDDLE, LATEST);

        OverallRankingRetentionPlan expired =
                retentionService.expire(request(MIDDLE, 30));

        // 만료 기준일 자체는 대상이 아니다.
        assertThat(expired.snapshotDates()).containsExactly(OLDEST);
        assertThat(expired.collectionCount()).isEqualTo(1L);
        assertThat(expired.snapshotCount()).isEqualTo(2L);

        entityManager.clear();

        assertThat(collectionRepository.findAll())
                .extracting(collection -> collection.getSnapshotDate())
                .containsExactlyInAnyOrder(MIDDLE, LATEST);

        // 자식을 먼저 지우지 않으면 Foreign Key 제약에 걸려 삭제가 실패한다.
        assertThat(snapshotRepository.findAll()).hasSize(4);
    }

    @Test
    void 최신기준일은보존기간이지나도남긴다() {
        given(OLDEST, LATEST);

        // 보존 일수를 잘못 넣어 전부 대상이 된 상황이다.
        OverallRankingRetentionPlan expired =
                retentionService.expire(request(LATEST.plusYears(1), 30));

        assertThat(expired.snapshotDates()).containsExactly(OLDEST);
        assertThat(expired.retainedLatestDate()).isEqualTo(LATEST);

        entityManager.clear();

        assertThat(collectionRepository.findAll())
                .extracting(collection -> collection.getSnapshotDate())
                .containsExactly(LATEST);
    }

    @Test
    void 한번에지우는기준일수를묶는다() {
        given(OLDEST, MIDDLE, LATEST);

        OverallRankingRetentionPlan expired =
                retentionService.expire(request(LATEST, 1));

        assertThat(expired.snapshotDates()).containsExactly(OLDEST);

        entityManager.clear();

        // 남은 기준일은 다음 실행이 이어받는다.
        assertThat(collectionRepository.findAll())
                .extracting(collection -> collection.getSnapshotDate())
                .containsExactlyInAnyOrder(MIDDLE, LATEST);
    }

    @Test
    void 산정은아무것도지우지않는다() {
        given(OLDEST, MIDDLE, LATEST);

        OverallRankingRetentionPlan planned =
                retentionService.plan(request(LATEST, 30));

        assertThat(planned.snapshotDates())
                .containsExactly(OLDEST, MIDDLE);
        assertThat(planned.collectionCount()).isEqualTo(2L);
        assertThat(planned.snapshotCount()).isEqualTo(4L);
        assertThat(planned.retainedLatestDate()).isEqualTo(LATEST);

        entityManager.clear();

        assertThat(collectionRepository.findAll()).hasSize(3);
        assertThat(snapshotRepository.findAll()).hasSize(6);
    }

    @Test
    void 대상이없으면보존한최신기준일만돌려준다() {
        given(LATEST);

        OverallRankingRetentionPlan planned =
                retentionService.plan(request(LATEST, 30));

        assertThat(planned.isEmpty()).isTrue();
        assertThat(planned.retainedLatestDate()).isEqualTo(LATEST);
    }

    @Test
    void 수집이하나도없으면아무것도하지않는다() {
        OverallRankingRetentionPlan expired =
                retentionService.expire(request(LATEST, 30));

        assertThat(expired.isEmpty()).isTrue();
        assertThat(expired.retainedLatestDate()).isNull();
    }

    private OverallRankingRetentionRequest request(
            LocalDate expireBefore,
            int maxDatesPerRun
    ) {
        return new OverallRankingRetentionRequest(
                expireBefore,
                maxDatesPerRun
        );
    }

    private void given(LocalDate... snapshotDates) {
        for (LocalDate snapshotDate : snapshotDates) {
            saveAdapter.saveOverallRankingSnapshot(
                    new OverallRankingCollection(
                            snapshotDate,
                            null,
                            null,
                            null,
                            "NEXON_OPEN_API",
                            1,
                            1,
                            false,
                            COLLECTED_AT,
                            List.of(
                                    createRow(1, "첫째"),
                                    createRow(2, "둘째")
                            )
                    )
            );
        }

        entityManager.flush();
        entityManager.clear();
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
