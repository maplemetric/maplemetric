package com.maplemetric.ranking.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maplemetric.ranking.api.OverallRankingRetentionRequest;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.OverallRankingCollection;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.RankingRow;
import com.maplemetric.ranking.application.service.OverallRankingRetentionService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 만료가 실제 Transaction 경계에서 원자적인지 확인한다.
 *
 * Snapshot 삭제와 Collection 삭제가 나뉘면 Snapshot만 사라지고 Collection이 남아
 * 조회에 빈 기준일이 생긴다. {@code @DataJpaTest}는 테스트 전체를 한 Transaction으로
 * 묶고 롤백하므로 그 조건에서는 Rollback 여부를 확인할 수 없어 여기서는 실제로
 * 커밋되는 Transaction을 쓴다.
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
// 테스트를 Transaction으로 감싸지 않아야 Service의 Transaction이 실제로 커밋된다.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OverallRankingRetentionTransactionTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final LocalDate OLDEST = LocalDate.of(2026, 7, 1);

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
    private OverallRankingRetentionPersistenceAdapter retentionAdapter;

    @Autowired
    private OverallRankingCollectionJpaRepository collectionRepository;

    @Autowired
    private OverallRankingSnapshotJpaRepository snapshotRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM p_overall_ranking_snapshot");
        jdbcTemplate.update("DELETE FROM p_overall_ranking_collection");
    }

    @Test
    void 만료가롤백되면Snapshot과Collection이함께되살아난다() {
        given(OLDEST, LATEST);

        inNewTransaction(status -> {
            retentionService.expire(
                    new OverallRankingRetentionRequest(LATEST, 30)
            );

            // 삭제 자체는 이 Transaction 안에서 이미 반영됐다.
            assertThat(collectionRepository.count()).isEqualTo(1L);

            status.setRollbackOnly();

            return null;
        });

        // 두 삭제가 한 Transaction이 아니었다면 한쪽만 남는다.
        assertThat(collectionRepository.count()).isEqualTo(2L);
        assertThat(snapshotRepository.count()).isEqualTo(4L);
    }

    /**
     * Transaction 밖 호출은 아무것도 지우기 전에 막는다.
     *
     * 통과시키면 Snapshot 삭제와 Collection 삭제가 각자 커밋돼, 뒤가 실패했을 때
     * Snapshot만 사라지고 Collection이 남는다.
     */
    @Test
    void Transaction없는삭제호출은거부한다() {
        given(OLDEST, LATEST);

        assertThatThrownBy(
                () -> retentionAdapter.deleteBySnapshotDates(List.of(OLDEST))
        ).isInstanceOf(IllegalTransactionStateException.class);

        assertThat(collectionRepository.count()).isEqualTo(2L);
        assertThat(snapshotRepository.count()).isEqualTo(4L);
    }

    private <T> T inNewTransaction(
            org.springframework.transaction.support.TransactionCallback<T> action
    ) {
        TransactionTemplate template =
                new TransactionTemplate(transactionManager);

        template.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );

        return template.execute(action);
    }

    private void given(LocalDate... snapshotDates) {
        for (LocalDate snapshotDate : snapshotDates) {
            inNewTransaction(status -> {
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

                return null;
            });
        }
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
