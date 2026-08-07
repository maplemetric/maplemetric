package com.maplemetric.analysis.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.maplemetric.analysis.api.GenerateStatisticsInsightOutcome;
import com.maplemetric.analysis.application.port.out.SaveStatisticsInsightPort;
import com.maplemetric.analysis.application.result.StatisticsInsightPreview;
import com.maplemetric.analysis.infrastructure.properties.StatisticsInsightProperties;
import com.maplemetric.statistics.api.StatisticsDataAvailability;
import com.maplemetric.statistics.api.StatisticsFactQuery;
import com.maplemetric.statistics.api.StatisticsHistoryFact;
import com.maplemetric.statistics.api.StatisticsSubject;
import com.maplemetric.statistics.api.StatisticsSubjectType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatisticsInsightGenerationServiceTest {

    private static final String PRESET = "90D";
    private static final LocalDate AS_OF = LocalDate.of(2026, 8, 3);
    private static final String MODEL = "test-model";

    @Mock
    private StatisticsFactQuery statisticsFactQuery;

    @Mock
    private StatisticsInsightGenerator insightGenerator;

    @Mock
    private SaveStatisticsInsightPort saveStatisticsInsightPort;

    private StatisticsInsightGenerationService service;

    @BeforeEach
    void setUp() {
        service = createService(70);
    }

    @Test
    void 설명이없는대상만생성해저장한다() {
        givenSubjects(job("hero"), world("scania"));
        givenHistory("hero", AS_OF);
        givenHistory("scania", AS_OF);
        givenPreview();

        given(saveStatisticsInsightPort.exists(
                StatisticsSubjectType.JOB, "hero", PRESET, AS_OF
        )).willReturn(false);
        given(saveStatisticsInsightPort.exists(
                StatisticsSubjectType.WORLD, "scania", PRESET, AS_OF
        )).willReturn(true);

        GenerateStatisticsInsightOutcome outcome = service.generate();

        assertThat(outcome).isEqualTo(
                new GenerateStatisticsInsightOutcome(1, 1, 0)
        );

        // 이미 있는 대상은 생성 자체를 하지 않아 비용이 들지 않는다.
        verify(insightGenerator, times(1)).generate(any());
        verify(saveStatisticsInsightPort, times(1))
                .save(eq(PRESET), eq(AS_OF), any(), any(), any(), any());
    }

    /**
     * 어떤 생성기가 만든 문장인지 함께 저장한다.
     *
     * 남기지 않으면 외부 호출이 실패해 템플릿으로 대체된 문장과 정상 생성된
     * 문장이 데이터상 같아 보인다. 실제로 Key가 만료된 채 전량이 템플릿으로
     * 저장됐고 로그를 뒤지기 전까지 알 수 없었다.
     */
    @Test
    void 생성출처를함께저장한다() {
        givenSubjects(job("hero"));
        givenHistory("hero", AS_OF);
        givenPreview();

        service.generate();

        verify(saveStatisticsInsightPort).save(
                eq(PRESET),
                eq(AS_OF),
                any(),
                any(),
                any(),
                eq(MODEL)
        );
    }

    /**
     * 61개 중 하나가 실패했다고 전체가 멈추면 매번 같은 지점에서 끊긴다.
     */
    @Test
    void 한대상이실패해도나머지를계속생성한다() {
        givenSubjects(job("hero"), job("bishop"));
        givenHistory("hero", AS_OF);
        givenHistory("bishop", AS_OF);
        givenPreview();

        willThrow(new IllegalStateException("생성 실패"))
                .given(insightGenerator)
                .generate(argThatSubject("hero"));

        GenerateStatisticsInsightOutcome outcome = service.generate();

        assertThat(outcome.generated()).isEqualTo(1);
        assertThat(outcome.failed()).isEqualTo(1);
    }

    @Test
    void 실행한도까지만생성한다() {
        service = createService(1);

        givenSubjects(job("hero"), job("bishop"));
        givenHistory("hero", AS_OF);
        givenPreview();

        GenerateStatisticsInsightOutcome outcome = service.generate();

        assertThat(outcome.generated()).isEqualTo(1);
        verify(insightGenerator, times(1)).generate(any());
    }

    /**
     * 수집된 기준일이 없으면 설명할 대상 자체가 없다.
     */
    @Test
    void 수집기준일이없으면생성하지않는다() {
        givenSubjects(job("hero"));
        given(statisticsFactQuery.getJobHistoryFact(
                "hero", PRESET, null, null
        )).willReturn(history(job("hero"), null));

        GenerateStatisticsInsightOutcome outcome = service.generate();

        assertThat(outcome).isEqualTo(
                new GenerateStatisticsInsightOutcome(0, 1, 0)
        );

        verify(insightGenerator, never()).generate(any());
        verify(saveStatisticsInsightPort, never())
                .exists(any(), anyString(), anyString(), any());
    }

    private StatisticsInsightGenerationService createService(int maxPerRun) {
        return new StatisticsInsightGenerationService(
                statisticsFactQuery,
                new StatisticsInsightFactsAssembler(),
                insightGenerator,
                saveStatisticsInsightPort,
                new StatisticsInsightProperties(maxPerRun, PRESET)
        );
    }

    private void givenSubjects(StatisticsSubject... subjects) {
        given(statisticsFactQuery.listSubjects())
                .willReturn(List.of(subjects));
    }

    private void givenHistory(String slug, LocalDate lastAsOf) {
        if (slug.equals("scania")) {
            given(statisticsFactQuery.getWorldHistoryFact(
                    slug, PRESET, null, null
            )).willReturn(history(world(slug), lastAsOf));

            return;
        }

        given(statisticsFactQuery.getJobHistoryFact(
                slug, PRESET, null, null
        )).willReturn(history(job(slug), lastAsOf));
    }

    private void givenPreview() {
        given(insightGenerator.generate(any()))
                .willReturn(new StatisticsInsightPreview(
                        "제목",
                        List.of("요약"),
                        MODEL
                ));
    }

    private com.maplemetric.analysis.domain.model.StatisticsInsightFacts
            argThatSubject(String slug) {
        return org.mockito.ArgumentMatchers.argThat(
                facts -> facts != null
                        && slug.equals(facts.subject().slug())
        );
    }

    private StatisticsSubject job(String slug) {
        return new StatisticsSubject(
                StatisticsSubjectType.JOB,
                slug,
                slug
        );
    }

    private StatisticsSubject world(String slug) {
        return new StatisticsSubject(
                StatisticsSubjectType.WORLD,
                slug,
                slug
        );
    }

    private StatisticsHistoryFact history(
            StatisticsSubject subject,
            LocalDate lastAsOf
    ) {
        return new StatisticsHistoryFact(
                subject,
                StatisticsDataAvailability.AVAILABLE,
                new StatisticsHistoryFact.RangeFact(
                        PRESET,
                        LocalDate.of(2026, 5, 6),
                        AS_OF,
                        LocalDate.of(2026, 5, 6),
                        lastAsOf,
                        13,
                        77
                ),
                null,
                List.of(),
                List.of()
        );
    }
}
