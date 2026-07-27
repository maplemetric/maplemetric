package com.maplemetric.ranking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.OverallRankingCollection;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.RankingRow;
import com.maplemetric.world.api.WorldAliasMatchingQuery;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
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
        OverallRankingSnapshotStoreService.class,
        OverallRankingReferenceObserver.class,
        OverallRankingSnapshotStoredEventListener.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ExtendWith(OutputCaptureExtension.class)
class OverallRankingSnapshotTransactionIntegrationTest {

    private static final String POSTGRES_IMAGE =
            "postgres:17-alpine";

    private static final LocalDate SNAPSHOT_DATE =
            LocalDate.of(2026, 7, 27);

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private OverallRankingSnapshotStoreService storeService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private SaveOverallRankingSnapshotPort saveOverallRankingSnapshotPort;

    @MockitoBean
    private JobAliasMatchingService jobAliasMatchingService;

    @MockitoBean
    private WorldAliasMatchingQuery worldAliasMatchingQuery;

    @Test
    void Commit전에는관측하지않고Commit후새읽기Transaction에서관측한다() {
        AtomicBoolean transactionActive = new AtomicBoolean(false);
        AtomicBoolean transactionReadOnly = new AtomicBoolean(false);

        given(jobAliasMatchingService.matches(anyString()))
                .willAnswer(invocation -> {
                    transactionActive.set(
                            TransactionSynchronizationManager
                                    .isActualTransactionActive()
                    );
                    transactionReadOnly.set(
                            TransactionSynchronizationManager
                                    .isCurrentTransactionReadOnly()
                    );
                    return true;
                });
        given(worldAliasMatchingQuery.matches(anyString()))
                .willReturn(true);

        executeStoreTransaction(false);

        verify(jobAliasMatchingService).matches("팬텀");
        verify(worldAliasMatchingQuery).matches("루나");
        assertThat(transactionActive).isTrue();
        assertThat(transactionReadOnly).isTrue();
    }

    @Test
    void Transaction이Rollback되면관측하지않는다() {
        executeStoreTransaction(true);

        verifyNoInteractions(
                jobAliasMatchingService,
                worldAliasMatchingQuery
        );
    }

    @Test
    void Observer실패는저장Transaction결과에영향을주지않는다(
            CapturedOutput output
    ) {
        given(jobAliasMatchingService.matches("팬텀"))
                .willThrow(new IllegalStateException("관측 실패"));

        assertThatCode(() -> executeStoreTransaction(false))
                .doesNotThrowAnyException();

        verify(saveOverallRankingSnapshotPort)
                .saveOverallRankingSnapshot(any());
        verify(jobAliasMatchingService).matches("팬텀");
        assertThat(output.getOut())
                .contains("종합 랭킹 기준정보 매핑 관측에 실패했습니다.")
                .contains("기준일=2026-07-27");
    }

    private void executeStoreTransaction(boolean rollback) {
        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );

        transactionTemplate.executeWithoutResult(status -> {
            storeService.store(createCollection());

            verifyNoInteractions(
                    jobAliasMatchingService,
                    worldAliasMatchingQuery
            );

            if (rollback) {
                status.setRollbackOnly();
            }
        });
    }

    private OverallRankingCollection createCollection() {
        return new OverallRankingCollection(
                SNAPSHOT_DATE,
                null,
                null,
                null,
                "NEXON_OPEN_API",
                1,
                10,
                false,
                Instant.parse("2026-07-27T00:30:00Z"),
                List.of(new RankingRow(
                        1,
                        "캐릭터",
                        "루나",
                        "팬텀",
                        null,
                        300,
                        0L,
                        0,
                        null
                ))
        );
    }
}
