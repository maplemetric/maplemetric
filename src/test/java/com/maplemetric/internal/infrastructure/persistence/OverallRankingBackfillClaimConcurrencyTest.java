package com.maplemetric.internal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillDate;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillErrorType;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillJob;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillStatus;
import com.maplemetric.internal.application.service.OverallRankingBackfillStateService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 실제 Transaction 경계에서 점유가 원자적인지 확인한다.
 *
 * {@code @DataJpaTest}는 테스트 메서드 전체를 한 Transaction으로 묶고 롤백하므로
 * Service의 {@code @Transactional}이 새 Transaction을 열지 않는다. 그 조건에서는
 * {@code FOR UPDATE SKIP LOCKED}가 남의 점유를 건너뛰는지 확인할 수 없어 여기서는
 * 실제로 커밋되는 Transaction을 쓴다.
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
        OverallRankingBackfillStateService.class,
        OverallRankingBackfillStatePersistenceAdapter.class
})
// 테스트를 Transaction으로 감싸지 않아야 Service의 Transaction이 실제로 커밋된다.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OverallRankingBackfillClaimConcurrencyTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final LocalDate FROM = LocalDate.of(2026, 7, 1);

    private static final LocalDate TO = LocalDate.of(2026, 7, 2);

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private OverallRankingBackfillStateService service;

    @Autowired
    private OverallRankingBackfillDateRepository dateRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM p_overall_ranking_backfill_date");
        jdbcTemplate.update("DELETE FROM p_overall_ranking_backfill_job");
    }

    @Test
    void 다른Transaction이잠근기준일은건너뛰고다음기준일을가져간다() {
        BackfillJob job = service.createJob(FROM, TO);

        // 바깥 Transaction이 첫 기준일 행만 잠근 채로 유지한다.
        // Job 행은 건드리지 않는다. 건드리면 안쪽 Transaction이 Job 갱신에서
        // 막혀 이 테스트가 잠금 대기로 멈춘다.
        LocalDate innerSnapshotDate = inNewTransaction(() -> {
            assertThat(dateRepository.lockNextPending(job.id()))
                    .get()
                    .extracting(date -> date.getSnapshotDate())
                    .isEqualTo(FROM);

            return inNewTransaction(() -> claim(job.id()).snapshotDate());
        });

        // 잠긴 첫 기준일을 건너뛰고 두 번째를 가져갔다.
        assertThat(innerSnapshotDate).isEqualTo(TO);

        assertThat(service.findDates(job.id()))
                .extracting(
                        date -> date.snapshotDate(),
                        date -> date.status()
                )
                .containsExactly(
                        tuple(FROM, BackfillStatus.PENDING),
                        tuple(TO, BackfillStatus.RUNNING)
                );
    }

    @Test
    void 같은기준일의결과기록은잠금을기다려한번만집계된다() throws Exception {
        BackfillJob job = service.createJob(FROM, TO);
        BackfillDate claimed = inNewTransaction(() -> claim(job.id()));
        UUID dateId = claimed.id();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<?>> failures = inNewTransaction(() -> {
                // 바깥 Transaction이 기준일 행을 잠근 채로 유지한다.
                dateRepository.findByIdForUpdate(dateId);

                List<Future<?>> submitted = List.of(
                        executor.submit(() -> fail(claimed)),
                        executor.submit(() -> fail(claimed))
                );

                // 행을 잠그지 않고 읽으면 둘 다 이 시간 안에 끝나고, 각자 RUNNING을
                // 본 채로 실패를 기록해 집계가 두 번 올라간다.
                submitted.forEach(future -> assertThatThrownBy(
                        () -> future.get(1, TimeUnit.SECONDS)
                ).isInstanceOf(TimeoutException.class));

                return submitted;
            });

            for (Future<?> failure : failures) {
                failure.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(service.findJob(job.id()).orElseThrow())
                .extracting(finished -> finished.failedDateCount())
                .isEqualTo(1);

        assertThat(service.findDates(job.id()))
                .extracting(date -> date.status(), date -> date.attemptCount())
                .containsExactly(
                        tuple(BackfillStatus.FAILED, 1),
                        tuple(BackfillStatus.PENDING, 0)
                );
    }

    @Test
    void 커밋된상태전이는이후Transaction에서그대로보인다() {
        BackfillJob job = service.createJob(FROM, TO);

        inNewTransaction(() -> succeed(claim(job.id())));
        inNewTransaction(() -> succeed(claim(job.id())));

        BackfillJob finished = service.findJob(job.id()).orElseThrow();

        assertThat(finished.status()).isEqualTo(BackfillStatus.SUCCEEDED);
        assertThat(finished.succeededDateCount()).isEqualTo(2);
        assertThat(finished.startedAt()).isNotNull();
        assertThat(finished.finishedAt()).isNotNull();

        List<BackfillDate> dates = service.findDates(job.id());

        assertThat(dates)
                .extracting(date -> date.status())
                .containsOnly(BackfillStatus.SUCCEEDED);
    }

    private void fail(BackfillDate date) {
        service.failDate(
                date.id(),
                date.claimToken(),
                BackfillErrorType.UNKNOWN,
                false
        );
    }

    private void succeed(BackfillDate date) {
        service.succeedDate(date.id(), date.claimToken());
    }

    private BackfillDate claim(UUID backfillJobId) {
        Optional<BackfillDate> claimed =
                service.claimNextPendingDate(backfillJobId);

        assertThat(claimed).isPresent();

        return claimed.get();
    }

    private <T> T inNewTransaction(java.util.function.Supplier<T> action) {
        TransactionTemplate template =
                new TransactionTemplate(transactionManager);

        template.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );

        return template.execute(status -> action.get());
    }

    private void inNewTransaction(Runnable action) {
        inNewTransaction(() -> {
            action.run();
            return null;
        });
    }
}
