package com.maplemetric.ranking.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.maplemetric.ranking.application.port.out.LoadCollectedSnapshotDatePort;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.OverallRankingCollection;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.RankingRow;
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
 * 어떤 기준일을 "채워졌다"고 볼지 확인한다.
 *
 * 이 판단이 곧 메우기의 대상을 정한다. 좁은 조건으로 받은 수집을 채워진 것으로 세면
 * 그 기준일의 전체 조건 저장본은 아무도 만들지 않는다. 통계가 보는 것은 전체 조건이라
 * 그 날은 통계에서 계속 비어 있게 된다.
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
        OverallRankingCollectedDateAdapter.class,
        OverallRankingSnapshotPersistenceAdapter.class
})
class OverallRankingCollectedDatePersistenceTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final LocalDate FIRST = LocalDate.of(2026, 7, 1);

    private static final LocalDate SECOND = LocalDate.of(2026, 7, 2);

    private static final Instant COLLECTED_AT =
            Instant.parse("2026-07-03T00:00:00Z");

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private LoadCollectedSnapshotDatePort loadCollectedDatePort;

    @Autowired
    private SaveOverallRankingSnapshotPort saveSnapshotPort;

    @Test
    void 전체조건으로수집한기준일을오름차순으로돌려준다() {
        saveAllCondition(SECOND);
        saveAllCondition(FIRST);

        assertThat(loadCollectedDatePort.loadCollectedDates(FIRST, SECOND))
                .containsExactly(FIRST, SECOND);
    }

    /**
     * 월드를 좁혀 받은 수집만 있는 날은 채워진 것이 아니다.
     *
     * 통계는 전체 조건 저장본을 본다. 좁은 조건만 있는 날을 채워졌다고 세면 그 날의
     * 전체 조건 저장본은 영영 만들어지지 않는다.
     */
    @Test
    void 좁은조건만있는기준일은채워진것이아니다() {
        saveWorldCondition(FIRST, "스카니아");

        assertThat(loadCollectedDatePort.loadCollectedDates(FIRST, SECOND))
                .isEmpty();
    }

    /** 직업을 좁혀 받은 수집도 마찬가지다. */
    @Test
    void 직업을좁힌수집도채워진것이아니다() {
        saveClassCondition(FIRST, "히어로");

        assertThat(loadCollectedDatePort.loadCollectedDates(FIRST, SECOND))
                .isEmpty();
    }

    @Test
    void 기간밖의기준일은돌려주지않는다() {
        saveAllCondition(FIRST.minusDays(1));
        saveAllCondition(SECOND.plusDays(1));

        assertThat(loadCollectedDatePort.loadCollectedDates(FIRST, SECOND))
                .isEmpty();
    }

    @Test
    void 전체조건으로수집한가장오래된기준일을돌려준다() {
        saveAllCondition(SECOND);
        saveAllCondition(FIRST);

        assertThat(loadCollectedDatePort.loadEarliestCollectedDate())
                .contains(FIRST);
    }

    /** 좁은 조건만 있으면 아직 수집을 시작한 것이 아니다. */
    @Test
    void 좁은조건만있으면가장오래된기준일이없다() {
        saveWorldCondition(FIRST, "스카니아");

        assertThat(loadCollectedDatePort.loadEarliestCollectedDate())
                .isEmpty();
    }

    @Test
    void 수집이없으면가장오래된기준일이없다() {
        assertThat(loadCollectedDatePort.loadEarliestCollectedDate())
                .isEmpty();
    }

    private void saveAllCondition(LocalDate snapshotDate) {
        save(snapshotDate, null, null, null);
    }

    private void saveWorldCondition(
            LocalDate snapshotDate,
            String worldName
    ) {
        save(snapshotDate, worldName, null, null);
    }

    private void saveClassCondition(
            LocalDate snapshotDate,
            String className
    ) {
        save(snapshotDate, null, null, className);
    }

    private void save(
            LocalDate snapshotDate,
            String worldName,
            Integer worldType,
            String className
    ) {
        saveSnapshotPort.saveOverallRankingSnapshot(
                new OverallRankingCollection(
                        snapshotDate,
                        worldName,
                        worldType,
                        className,
                        "NEXON_OPEN_API",
                        1,
                        1,
                        false,
                        COLLECTED_AT,
                        List.of(createRow(1, "첫째"))
                )
        );
    }

    private RankingRow createRow(int ranking, String characterName) {
        return new RankingRow(
                ranking,
                characterName,
                "스카니아",
                "히어로",
                "히어로",
                290,
                1L,
                0,
                null
        );
    }
}
