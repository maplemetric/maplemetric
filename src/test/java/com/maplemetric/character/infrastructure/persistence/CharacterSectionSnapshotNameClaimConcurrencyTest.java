package com.maplemetric.character.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maplemetric.character.application.port.out.SaveCharacterSectionSnapshotPort;
import com.maplemetric.character.application.port.out.SaveCharacterSectionSnapshotPort.CharacterSection;
import java.sql.SQLException;
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
        CountDownLatch olderEnteringSave = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<?> older;
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

                // 오래된 쪽이 저장 경로에 실제로 진입한 것을 확인하고 넘어간다.
                // 기다리지 않으면 상대가 시작조차 못한 채 이 테스트가 통과할 수 있다.
                await(olderEnteringSave, "오래된 수집이 저장 경로에 진입하지 않았습니다.");
                sleepUntilOlderBlocks();

                repository.upsert(
                        NEWER_OCID,
                        CHARACTER_NAME,
                        CharacterSection.PROFILE.name(),
                        "{\"value\":\"최신\"}",
                        NEWER_FETCHED_AT
                );
            }));

            older = executor.submit(() -> inNewTransaction(() -> {
                await(newerReleased, "최신 수집이 회수를 마치지 않았습니다.");

                olderEnteringSave.countDown();

                save(OLDER_OCID, OLDER_FETCHED_AT);
            }));

            newer.get(60, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(ownerOf(CHARACTER_NAME)).isEqualTo(NEWER_OCID);

        // 오래된 쪽은 유일 제약에 걸려 끝나야 한다. 다른 이유로 끝났다면 이 테스트가
        // 의도한 순서를 재현하지 못한 것이다.
        assertThatThrownBy(() -> older.get(60, TimeUnit.SECONDS))
                .hasRootCauseInstanceOf(SQLException.class)
                .rootCause()
                .hasMessageContaining(
                        "uk_p_character_section_snapshot_name_section"
                );
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

    private void await(CountDownLatch latch, String message) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException(message);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    /**
     * 상대가 잠금 대기에 들어갈 시간을 준다.
     *
     * 신호는 저장 경로 진입 직전에 오므로 그 자체로는 아직 잠금을 잡으러 가기 전이다.
     * 이 대기가 짧아 상대가 아직 도달하지 못했더라도 결과는 같다. 잠금이 있으면
     * 어차피 기다리게 되고, 없으면 이 테스트가 실패해야 하기 때문이다.
     */
    private void sleepUntilOlderBlocks() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
