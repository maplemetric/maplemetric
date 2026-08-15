package com.maplemetric.character.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.maplemetric.character.application.port.out.SaveCharacterSectionSnapshotPort;
import com.maplemetric.character.application.port.out.SaveCharacterSectionSnapshotPort.CharacterSection;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
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
 * 같은 이름을 동시에 차지하려 할 때 최신 수집이 남는지 확인한다.
 *
 * 회수와 저장은 두 문장이라 서로의 행을 아직 보지 못한 채 진행할 수 있다. 그 경우
 * 회수 조건의 시각 비교가 아무것도 비교하지 못하고, 먼저 커밋한 쪽이 이름을 차지한다.
 * 유일 제약은 소유자를 하나로 만들 뿐 최신을 고르지 않는다.
 *
 * {@code @DataJpaTest}는 테스트 메서드를 한 Transaction으로 묶고 롤백하므로 그 조건에서는
 * 서로 다른 Transaction의 가시성을 확인할 수 없다. 여기서는 실제로 커밋되는 Transaction을
 * 쓴다.
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
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import(CharacterSectionSnapshotPersistenceAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CharacterSectionSnapshotNameClaimConcurrencyTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final String CHARACTER_NAME = "겹치는이름";

    private static final String NEWER_OCID = "ocid-최신";

    private static final String OLDER_OCID = "ocid-오래된";

    private static final Instant OLDER_FETCHED_AT =
            Instant.parse("2026-08-01T00:00:00Z");

    private static final Instant NEWER_FETCHED_AT =
            OLDER_FETCHED_AT.plusSeconds(3600);

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private SaveCharacterSectionSnapshotPort port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private CharacterSectionSnapshotRepository repository;

    record TestPayload(String value) {
    }

    @AfterEach
    void 저장본을정리한다() {
        jdbcTemplate.update("DELETE FROM p_character_section_snapshot");
    }

    /**
     * 최신 수집이 회수와 저장 사이에 멈춘 동안 오래된 수집이 끼어들어도 이름을
     * 빼앗지 못한다.
     *
     * 이 순서가 문제의 핵심이다. 잠금이 없으면 두 Transaction이 서로의 행을 보지 못한
     * 채 각자 회수를 마치고, 먼저 커밋한 오래된 쪽이 이름을 차지한다. 뒤늦게 삽입하는
     * 최신 데이터는 유일 제약에 걸려 버려진다. 회수 조건의 시각 비교는 상대 행이
     * 보일 때만 동작하므로 이 경로를 막지 못한다.
     *
     * 그 순서를 만들려면 최신 쪽이 회수와 저장 사이에서 멈춰야 한다. 저장 경로가 한
     * 메서드라 테스트가 같은 순서를 직접 몬다.
     */
    @Test
    void 회수와저장사이에끼어든오래된수집은이름을빼앗지못한다() throws Exception {
        CountDownLatch newerReleased = new CountDownLatch(1);
        CountDownLatch olderFinished = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> newer = executor.submit(() -> inNewTransaction(() -> {
                repository.lockCharacterName(lockKey());
                repository.releaseCharacterName(
                        CHARACTER_NAME,
                        CharacterSection.PROFILE.name(),
                        NEWER_OCID,
                        NEWER_FETCHED_AT
                );

                newerReleased.countDown();

                // 잠금이 있으면 오래된 쪽은 여기서 진입하지 못해 신호가 오지 않는다.
                // 신호를 기다리지 않고 넘어가는 것이 정상이다.
                awaitAtMost(olderFinished);

                repository.upsert(
                        NEWER_OCID,
                        CHARACTER_NAME,
                        CharacterSection.PROFILE.name(),
                        "{\"value\":\"최신\"}",
                        NEWER_FETCHED_AT
                );
            }));

            Future<?> older = executor.submit(() -> {
                try {
                    await(newerReleased);
                    inNewTransaction(() -> save(OLDER_OCID, OLDER_FETCHED_AT));
                } finally {
                    olderFinished.countDown();
                }
            });

            // 오래된 쪽은 유일 제약에 걸려 실패할 수 있다. 실패 자체는 정상이며
            // 호출부가 삼킨다. 여기서 확인할 것은 남은 소유자다.
            newer.get(60, TimeUnit.SECONDS);
            awaitQuietly(older);
        } finally {
            executor.shutdownNow();
        }

        assertThat(ownerOf(CHARACTER_NAME)).isEqualTo(NEWER_OCID);
    }

    private String lockKey() {
        return CHARACTER_NAME + ":" + CharacterSection.PROFILE.name();
    }

    /**
     * 이름을 잠가도 다른 이름의 저장은 기다리지 않는다.
     */
    @Test
    void 다른이름의저장은서로기다리지않는다() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> inNewTransaction(() ->
                    save("ocid-1", "이름1", NEWER_FETCHED_AT)
            ));
            Future<?> second = executor.submit(() -> inNewTransaction(() ->
                    save("ocid-2", "이름2", NEWER_FETCHED_AT)
            ));

            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(ownerOf("이름1")).isEqualTo("ocid-1");
        assertThat(ownerOf("이름2")).isEqualTo("ocid-2");
    }

    private void save(String ocid, Instant fetchedAt) {
        save(ocid, CHARACTER_NAME, fetchedAt);
    }

    private void save(
            String ocid,
            String characterName,
            Instant fetchedAt
    ) {
        port.save(
                ocid,
                characterName,
                CharacterSection.PROFILE,
                new TestPayload(ocid),
                fetchedAt
        );
    }

    private String ownerOf(String characterName) {
        return jdbcTemplate.queryForObject(
                """
                SELECT ocid
                  FROM p_character_section_snapshot
                 WHERE character_name = ?
                   AND section = 'PROFILE'
                """,
                String.class,
                characterName
        );
    }

    private void inNewTransaction(Runnable work) {
        TransactionTemplate template =
                new TransactionTemplate(transactionManager);

        template.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );

        template.executeWithoutResult(status -> work.run());
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("상대 Transaction이 시작되지 않았습니다.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    /**
     * 상대가 끼어들 기회를 주되 끝까지 기다리지는 않는다.
     *
     * 잠금이 동작하면 상대는 진입하지 못하므로 신호가 오지 않는다. 그때는 시간이 지나면
     * 그대로 진행해야 한다.
     */
    private void awaitAtMost(CountDownLatch latch) {
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private void awaitQuietly(Future<?> future) {
        try {
            future.get(30, TimeUnit.SECONDS);
        } catch (Exception exception) {
            // 유일 제약 위반으로 끝나는 것이 정상 경로다.
        }
    }
}
