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

        succeedDate(claim(job.id()));
        skipDate(claim(job.id()));

        assertThat(findJob(job.id()).status())
                .isEqualTo(BackfillStatus.RUNNING);

        succeedDate(claim(job.id()));

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

        failDate(
                claim(job.id()),
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

    /**
     * 점유를 되돌리면 시도 횟수도 함께 되돌린다.
     *
     * 한도 초과처럼 기준일과 무관한 사정으로 못 받았을 때 쓴다. 시도가 남으면 그
     * 사정이 반복될수록 쌓이고, 나중에 진짜 일시 오류가 왔을 때 이미 소진돼 영구
     * 실패한다.
     */
    @Test
    void 점유를되돌리면시도횟수도되돌린다() {
        BackfillJob job = service.createJob(FROM, FROM);

        BackfillDate claimed = claim(job.id());

        assertThat(claimed.attemptCount()).isEqualTo(1);

        releaseDate(
                claimed,
                BackfillErrorType.EXTERNAL_RATE_LIMITED
        );

        BackfillDate released = onlyDate(job.id());

        assertThat(released.status()).isEqualTo(BackfillStatus.PENDING);
        assertThat(released.attemptCount()).isZero();
        assertThat(released.lastErrorType())
                .isEqualTo(BackfillErrorType.EXTERNAL_RATE_LIMITED);
        assertThat(released.finishedAt()).isNull();

        assertThat(findJob(job.id()).failedDateCount()).isZero();

        // 되돌렸으므로 다시 점유해도 첫 시도다.
        assertThat(claim(job.id()).attemptCount()).isEqualTo(1);
    }

    @Test
    void 재시도불가능한실패는기준일과Job을실패로닫는다() {
        BackfillJob job = service.createJob(FROM, FROM);

        failDate(
                claim(job.id()),
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

        failDate(
                claim(job.id()),
                BackfillErrorType.STORE_FAILED,
                false
        );
        succeedDate(claim(job.id()));

        assertThat(findJob(job.id()).status())
                .isEqualTo(BackfillStatus.FAILED);
    }

    @Test
    void 취소는남은기준일과Job을CANCELLED로닫고끝난기준일은건드리지않는다() {
        BackfillJob job = service.createJob(FROM, TO);

        succeedDate(claim(job.id()));
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
        BackfillDate claimedDate = claim(job.id());
        service.cancelJob(job.id());

        succeedDate(claimedDate);

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

        BackfillDate claimedDate = claim(job.id());
        succeedDate(claimedDate);

        skipDate(claimedDate);
        failDate(
                claimedDate,
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

        failDate(stale, BackfillErrorType.UNKNOWN, true);

        BackfillDate reclaimed = claim(job.id());

        assertThat(reclaimed.id()).isEqualTo(claimed.id());
        assertThat(reclaimed.attemptCount()).isEqualTo(2);
    }

    @Test
    void 끝난기준일은회수대상이아니다() {
        BackfillJob job = service.createJob(FROM, FROM);

        succeedDate(claim(job.id()));
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

    /**
     * 점유할 때마다 새 표를 발급한다.
     *
     * 표가 같으면 앞선 점유의 뒤늦은 결과가 새 점유에 그대로 통한다.
     */
    @Test
    void 점유할때마다새표를발급한다() {
        BackfillJob job = service.createJob(FROM, FROM);

        BackfillDate first = claim(job.id());

        assertThat(first.claimToken()).isNotNull();

        failDate(first, BackfillErrorType.EXTERNAL_TIMEOUT, true);

        BackfillDate second = claim(job.id());

        assertThat(second.claimToken()).isNotNull();
        assertThat(second.claimToken()).isNotEqualTo(first.claimToken());
    }

    /**
     * 점유를 잃은 실행기의 뒤늦은 결과는 아무것도 바꾸지 않는다.
     *
     * 오래 걸려 회수된 실행기가 그 사이 다시 점유한 실행기의 점유를 덮어쓰면, 같은
     * 기준일을 둘이 수집하고 시도 한도도 무너진다.
     */
    @Test
    void 점유를잃은실행기의뒤늦은결과는무시된다() {
        BackfillJob job = service.createJob(FROM, FROM);

        BackfillDate lost = claim(job.id());

        // 회수된 뒤 다른 실행기가 다시 잡았다.
        failDate(lost, BackfillErrorType.UNKNOWN, true);

        BackfillDate current = claim(job.id());

        succeedDate(lost);
        skipDate(lost);
        failDate(lost, BackfillErrorType.UNKNOWN, false);
        releaseDate(lost, BackfillErrorType.EXTERNAL_RATE_LIMITED);

        BackfillDate untouched = onlyDate(job.id());

        assertThat(untouched.status()).isEqualTo(BackfillStatus.RUNNING);
        assertThat(untouched.claimToken()).isEqualTo(current.claimToken());

        BackfillJob unchanged = findJob(job.id());

        assertThat(unchanged.succeededDateCount()).isZero();
        assertThat(unchanged.skippedDateCount()).isZero();
        assertThat(unchanged.failedDateCount()).isZero();
    }

    /**
     * 점유가 끝나면 표를 지운다.
     *
     * 표가 남아 있는 것은 지금 누군가 점유 중이라는 뜻이다. 끝난 기준일에 남겨 두면
     * 표의 의미가 흐려진다.
     */
    @Test
    void 점유가끝나면표를지운다() {
        BackfillJob job = service.createJob(FROM, FROM);

        BackfillDate claimed = claim(job.id());

        releaseDate(claimed, BackfillErrorType.EXTERNAL_RATE_LIMITED);

        assertThat(onlyDate(job.id()).claimToken()).isNull();

        BackfillDate retried = claim(job.id());

        failDate(retried, BackfillErrorType.EXTERNAL_TIMEOUT, true);

        assertThat(onlyDate(job.id()).claimToken()).isNull();

        BackfillDate finished = claim(job.id());

        succeedDate(finished);

        assertThat(onlyDate(job.id()).claimToken()).isNull();
    }

    /** 취소도 표를 남기지 않는다. 점유 중이 아닌 기준일이 되기 때문이다. */
    @Test
    void 취소도표를남기지않는다() {
        BackfillJob job = service.createJob(FROM, FROM);

        claim(job.id());

        service.cancelJob(job.id());

        BackfillDate cancelled = onlyDate(job.id());

        assertThat(cancelled.status()).isEqualTo(BackfillStatus.CANCELLED);
        assertThat(cancelled.claimToken()).isNull();
    }

    /**
     * 회수 조회와 회수 기록 사이에 다시 점유되면 회수가 적용되지 않는다.
     *
     * 회수 조회는 행을 잠그지 않는다. 그 사이 다른 실행기가 잡으면 표가 달라진다.
     */
    @Test
    void 회수전에다시점유되면회수가적용되지않는다() {
        BackfillJob job = service.createJob(FROM, FROM);

        BackfillDate claimed = claim(job.id());

        BackfillDate stale = service.findStaleClaims(
                job.id(),
                Instant.now().plusSeconds(600)
        ).get(0);

        // 회수를 기록하기 전에 원래 실행기가 스스로 끝내고 다른 실행기가 다시 잡았다.
        failDate(claimed, BackfillErrorType.UNKNOWN, true);

        BackfillDate reclaimed = claim(job.id());

        failDate(stale, BackfillErrorType.UNKNOWN, true);

        BackfillDate current = onlyDate(job.id());

        assertThat(current.status()).isEqualTo(BackfillStatus.RUNNING);
        assertThat(current.claimToken()).isEqualTo(reclaimed.claimToken());
    }

    /**
     * 점유한 실행기로서 결과를 기록한다.
     *
     * 점유할 때 받은 표를 함께 넘겨야 상태가 바뀐다. 시험마다 표를 꺼내 넘기면
     * 검증하려는 것이 무엇인지가 가려진다.
     */
    private void succeedDate(BackfillDate date) {
        service.succeedDate(date.id(), date.claimToken());
    }

    private void skipDate(BackfillDate date) {
        service.skipDate(date.id(), date.claimToken());
    }

    private void failDate(
            BackfillDate date,
            BackfillErrorType errorType,
            boolean retryable
    ) {
        service.failDate(
                date.id(),
                date.claimToken(),
                errorType,
                retryable
        );
    }

    private void releaseDate(
            BackfillDate date,
            BackfillErrorType errorType
    ) {
        service.releaseDate(date.id(), date.claimToken(), errorType);
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
