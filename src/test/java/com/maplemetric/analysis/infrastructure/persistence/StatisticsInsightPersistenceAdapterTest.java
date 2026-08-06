package com.maplemetric.analysis.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maplemetric.analysis.domain.model.InsightEvidence;
import com.maplemetric.analysis.domain.model.StatisticsInsightFact;
import com.maplemetric.analysis.domain.model.StatisticsInsightFactType;
import com.maplemetric.analysis.domain.model.StatisticsInsightFacts;
import com.maplemetric.analysis.domain.model.StatisticsInsightUnit;
import com.maplemetric.statistics.api.StatisticsSubjectType;
import com.maplemetric.statistics.api.StatisticsTrend;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
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
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import(StatisticsInsightPersistenceAdapter.class)
class StatisticsInsightPersistenceAdapterTest {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final String PRESET = "90D";

    private static final LocalDate AS_OF = LocalDate.of(2026, 8, 3);

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private StatisticsInsightPersistenceAdapter adapter;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 생성한설명과근거를함께저장한다() {
        adapter.save(PRESET, AS_OF, "제목", "요약", facts(), "test-model");

        entityManager.flush();
        entityManager.clear();

        StatisticsInsightEntity saved = entityManager
                .createQuery(
                        "select insight from StatisticsInsightEntity insight",
                        StatisticsInsightEntity.class
                )
                .getSingleResult();

        assertThat(saved.getSubjectType())
                .isEqualTo(StatisticsSubjectType.JOB);
        assertThat(saved.getSubjectSlug()).isEqualTo("hero");
        assertThat(saved.getRangePreset()).isEqualTo(PRESET);
        assertThat(saved.getAsOf()).isEqualTo(AS_OF);
        assertThat(saved.getHeadline()).isEqualTo("제목");

        // 근거를 남겨야 이 문장이 어떤 수치에서 나왔는지 추적할 수 있다.
        assertThat(saved.getFactsPayload())
                .contains("SHARE_CHANGE")
                .contains("hero");
    }

    @Test
    void 같은대상기간기준일이면존재로판정한다() {
        assertThat(adapter.exists(
                StatisticsSubjectType.JOB, "hero", PRESET, AS_OF
        )).isFalse();

        adapter.save(PRESET, AS_OF, "제목", "요약", facts(), "test-model");
        entityManager.flush();

        assertThat(adapter.exists(
                StatisticsSubjectType.JOB, "hero", PRESET, AS_OF
        )).isTrue();

        // 기간이나 기준일이 다르면 다른 설명이다.
        assertThat(adapter.exists(
                StatisticsSubjectType.JOB, "hero", "1Y", AS_OF
        )).isFalse();
        assertThat(adapter.exists(
                StatisticsSubjectType.JOB, "hero", PRESET, AS_OF.minusDays(1)
        )).isFalse();
    }

    /**
     * 재실행이 비용을 늘리지 않도록 같은 키는 두 번 저장되지 않는다.
     */
    @Test
    void 같은키를두번저장하면제약으로거부한다() {
        adapter.save(PRESET, AS_OF, "제목", "요약", facts(), "test-model");
        entityManager.flush();

        assertThatThrownBy(() -> {
            adapter.save(PRESET, AS_OF, "다른 제목", "다른 요약", facts(), null);
            entityManager.flush();
        }).hasMessageContaining(
                "uk_p_statistics_insight_subject_range_as_of"
        );
    }

    private StatisticsInsightFacts facts() {
        return new StatisticsInsightFacts(
                new StatisticsInsightFacts.Subject(
                        StatisticsSubjectType.JOB,
                        "hero",
                        "히어로"
                ),
                new StatisticsInsightFacts.Period(
                        PRESET,
                        LocalDate.of(2026, 5, 6),
                        AS_OF,
                        13,
                        77
                ),
                List.of(new StatisticsInsightFact(
                        StatisticsInsightFactType.SHARE_CHANGE,
                        new BigDecimal("2.34"),
                        StatisticsInsightUnit.PERCENTAGE_POINT,
                        StatisticsTrend.UP,
                        List.of(new InsightEvidence("비중 변화", "2.34"))
                )),
                new StatisticsInsightFacts.Source(
                        "NEXON_OPEN_API",
                        Instant.parse("2026-08-03T00:40:00Z"),
                        2000,
                        true
                ),
                List.of("표본입니다.")
        );
    }
}
