package com.maplemetric.internal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillDate;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillErrorType;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillJob;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillStatus;
import com.maplemetric.internal.application.service.OverallRankingBackfillStateService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
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
        OverallRankingBackfillStateService.class,
        OverallRankingBackfillStatePersistenceAdapter.class
})
class OverallRankingBackfillStatePersistenceTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final LocalDate FROM = LocalDate.of(2026, 7, 1);

    private static final LocalDate TO = LocalDate.of(2026, 7, 3);

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private OverallRankingBackfillStateService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 요청기간의모든기준일을PENDING으로만든다() {
        BackfillJob job = service.createJob(FROM, TO);

        assertThat(job.status()).isEqualTo(BackfillStatus.PENDING);
        assertThat(job.requestedFrom()).isEqualTo(FROM);
        assertThat(job.requestedTo()).isEqualTo(TO);
        assertThat(job.createdAt()).isNotNull();
        assertThat(job.startedAt()).isNull();

        assertThat(service.findDates(job.id()))
                .extracting(
                        date -> date.snapshotDate(),
                        date -> date.status(),
                        date -> date.attemptCount()
                )
                .containsExactly(
                        tuple(FROM, BackfillStatus.PENDING, 0),
                        tuple(FROM.plusDays(1), BackfillStatus.PENDING, 0),
                        tuple(TO, BackfillStatus.PENDING, 0)
                );
    }

    @Test
    void 같은Job에같은기준일을두번만들지못한다() {
        BackfillJob job = service.createJob(FROM, TO);
        entityManager.flush();

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO p_overall_ranking_backfill_date
                    (backfill_job_id, snapshot_date)
                VALUES (?, ?)
                """,
                job.id(),
                FROM
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 시작일이종료일보다뒤면Job을만들지않는다() {
        assertThatThrownBy(() -> service.createJob(TO, FROM))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 점유는기준일을오름차순으로하나씩가져가고Job을실행중으로바꾼다() {
        BackfillJob job = service.createJob(FROM, TO);

        BackfillDate claimed = claim(job.id());

        assertThat(claimed.snapshotDate()).isEqualTo(FROM);
        assertThat(claimed.status()).isEqualTo(BackfillStatus.RUNNING);
        assertThat(claimed.attemptCount()).isEqualTo(1);
        assertThat(claimed.startedAt()).isNotNull();

        BackfillJob running = findJob(job.id());

        assertThat(running.status()).isEqualTo(BackfillStatus.RUNNING);
        assertThat(running.startedAt()).isNotNull();

        assertThat(claim(job.id()).snapshotDate())
                .isEqualTo(FROM.plusDays(1));
    }

    @Test
    void 이미점유한기준일은다시점유되지않는다() {
        BackfillJob job = service.createJob(FROM, FROM);

        claim(job.id());

        assertThat(service.claimNextPendingDate(job.id())).isEmpty();
    }

    @Test
    void 성공과Skip은집계를올리고모두끝나면Job을성공으로닫는다() {
        BackfillJob job = service.createJob(FROM, TO);

        service.succeedDate(claim(job.id()).id());
        service.skipDate(claim(job.id()).id());

        assertThat(findJob(job.id()).status())
                .isEqualTo(BackfillStatus.RUNNING);

        service.succeedDate(claim(job.id()).id());

        BackfillJob finished = findJob(job.id());

        assertThat(finished.status()).isEqualTo(BackfillStatus.SUCCEEDED);
        assertThat(finished.succeededDateCount()).isEqualTo(2);
        assertThat(finished.skippedDateCount()).isEqualTo(1);
        assertThat(finished.failedDateCount()).isZero();
        assertThat(finished.finishedAt()).isNotNull();
    }

    @Test
    void 재시도가능한실패는PENDING으로되돌리고시도횟수를유지한다() {
        BackfillJob job = service.createJob(FROM, FROM);

        service.failDate(
                claim(job.id()).id(),
                BackfillErrorType.EXTERNAL_TIMEOUT,
                true
        );

        BackfillDate retried = onlyDate(job.id());

        assertThat(retried.status()).isEqualTo(BackfillStatus.PENDING);
        assertThat(retried.attemptCount()).isEqualTo(1);
        assertThat(retried.lastErrorType())
                .isEqualTo(BackfillErrorType.EXTERNAL_TIMEOUT);
        assertThat(retried.finishedAt()).isNull();

        assertThat(findJob(job.id()).failedDateCount()).isZero();

        // 중단 후 재개하면 같은 기준일을 다시 점유한다.
        assertThat(claim(job.id()).attemptCount()).isEqualTo(2);
    }

    @Test
    void 재시도불가능한실패는기준일과Job을실패로닫는다() {
        BackfillJob job = service.createJob(FROM, FROM);

        service.failDate(
                claim(job.id()).id(),
                BackfillErrorType.RESPONSE_INVALID,
                false
        );

        BackfillDate failed = onlyDate(job.id());

        assertThat(failed.status()).isEqualTo(BackfillStatus.FAILED);
        assertThat(failed.finishedAt()).isNotNull();

        BackfillJob finished = findJob(job.id());

        assertThat(finished.status()).isEqualTo(BackfillStatus.FAILED);
        assertThat(finished.failedDateCount()).isEqualTo(1);
    }

    @Test
    void 실패한기준일이하나라도있으면Job은성공으로닫히지않는다() {
        BackfillJob job = service.createJob(FROM, FROM.plusDays(1));

        service.failDate(
                claim(job.id()).id(),
                BackfillErrorType.STORE_FAILED,
                false
        );
        service.succeedDate(claim(job.id()).id());

        assertThat(findJob(job.id()).status())
                .isEqualTo(BackfillStatus.FAILED);
    }

    @Test
    void 취소는남은기준일과Job을CANCELLED로닫고끝난기준일은건드리지않는다() {
        BackfillJob job = service.createJob(FROM, TO);

        service.succeedDate(claim(job.id()).id());
        service.cancelJob(job.id());

        assertThat(service.findDates(job.id()))
                .extracting(date -> date.status())
                .containsExactly(
                        BackfillStatus.SUCCEEDED,
                        BackfillStatus.CANCELLED,
                        BackfillStatus.CANCELLED
                );

        BackfillJob cancelled = findJob(job.id());

        assertThat(cancelled.status()).isEqualTo(BackfillStatus.CANCELLED);
        assertThat(cancelled.finishedAt()).isNotNull();
        assertThat(service.claimNextPendingDate(job.id())).isEmpty();
    }

    @Test
    void 취소된기준일은뒤늦은성공기록으로되살아나지않는다() {
        BackfillJob job = service.createJob(FROM, TO);

        // 실행기가 외부 호출을 하는 사이 관리자가 Job을 취소한 상황이다.
        UUID claimedDateId = claim(job.id()).id();
        service.cancelJob(job.id());

        service.succeedDate(claimedDateId);

        assertThat(service.findDates(job.id()))
                .extracting(date -> date.status())
                .containsOnly(BackfillStatus.CANCELLED);

        BackfillJob cancelled = findJob(job.id());

        assertThat(cancelled.status()).isEqualTo(BackfillStatus.CANCELLED);
        assertThat(cancelled.succeededDateCount()).isZero();
    }

    @Test
    void 종료된기준일에Skip과실패를기록해도집계가바뀌지않는다() {
        BackfillJob job = service.createJob(FROM, FROM);

        UUID claimedDateId = claim(job.id()).id();
        service.succeedDate(claimedDateId);

        service.skipDate(claimedDateId);
        service.failDate(
                claimedDateId,
                BackfillErrorType.UNKNOWN,
                false
        );

        BackfillJob finished = findJob(job.id());

        assertThat(finished.succeededDateCount()).isEqualTo(1);
        assertThat(finished.skippedDateCount()).isZero();
        assertThat(finished.failedDateCount()).isZero();
        assertThat(onlyDate(job.id()).status())
                .isEqualTo(BackfillStatus.SUCCEEDED);
    }

    @Test
    void 임계시각보다오래된점유만회수대상이다() {
        BackfillJob job = service.createJob(FROM, TO);

        BackfillDate claimed = claim(job.id());
        entityManager.flush();

        // 방금 점유한 기준일은 정상 실행 중일 수 있어 건드리지 않는다.
        assertThat(service.findStaleClaims(
                job.id(),
                Instant.now().minusSeconds(600)
        )).isEmpty();

        // 임계 시각이 점유 시각보다 뒤면 회수 대상이다.
        assertThat(service.findStaleClaims(
                job.id(),
                Instant.now().plusSeconds(600)
        ))
                .extracting(date -> date.id())
                .containsExactly(claimed.id());
    }

    @Test
    void 회수한기준일은다시점유된다() {
        BackfillJob job = service.createJob(FROM, FROM);

        BackfillDate claimed = claim(job.id());
        entityManager.flush();

        // 실행기가 강제 종료돼 RUNNING으로 남은 상황이다.
        assertThat(service.claimNextPendingDate(job.id())).isEmpty();

        BackfillDate stale = service.findStaleClaims(
                job.id(),
                Instant.now().plusSeconds(600)
        ).get(0);

        service.failDate(
                stale.id(),
                BackfillErrorType.UNKNOWN,
                true
        );

        BackfillDate reclaimed = claim(job.id());

        assertThat(reclaimed.id()).isEqualTo(claimed.id());
        assertThat(reclaimed.attemptCount()).isEqualTo(2);
    }

    @Test
    void 끝난기준일은회수대상이아니다() {
        BackfillJob job = service.createJob(FROM, FROM);

        service.succeedDate(claim(job.id()).id());
        entityManager.flush();

        assertThat(service.findStaleClaims(
                job.id(),
                Instant.now().plusSeconds(600)
        )).isEmpty();
    }

    @Test
    void 없는Job을취소하면예외를던진다() {
        assertThatThrownBy(() -> service.cancelJob(UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 오류분류밖의값은저장하지못한다() {
        BackfillJob job = service.createJob(FROM, FROM);
        entityManager.flush();

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                UPDATE p_overall_ranking_backfill_date
                   SET last_error_type = ?
                 WHERE backfill_job_id = ?
                """,
                "Connection refused: secret-token=abc",
                job.id()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void Job상태에SKIPPED를저장하지못한다() {
        BackfillJob job = service.createJob(FROM, FROM);
        entityManager.flush();

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                UPDATE p_overall_ranking_backfill_job
                   SET status = 'SKIPPED'
                 WHERE backfill_job_id = ?
                """,
                job.id()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private BackfillDate claim(UUID backfillJobId) {
        Optional<BackfillDate> claimed =
                service.claimNextPendingDate(backfillJobId);

        assertThat(claimed).isPresent();

        return claimed.get();
    }

    private BackfillDate onlyDate(UUID backfillJobId) {
        List<BackfillDate> dates = service.findDates(backfillJobId);

        assertThat(dates).hasSize(1);

        return dates.get(0);
    }

    private BackfillJob findJob(UUID backfillJobId) {
        Optional<BackfillJob> job = service.findJob(backfillJobId);

        assertThat(job).isPresent();

        return job.get();
    }
}
