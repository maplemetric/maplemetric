package com.maplemetric.analysis.application.service;

import com.maplemetric.analysis.api.GenerateStatisticsInsightOutcome;
import com.maplemetric.analysis.api.GenerateStatisticsInsightUseCase;
import com.maplemetric.analysis.application.port.out.SaveStatisticsInsightPort;
import com.maplemetric.analysis.application.result.StatisticsInsightPreview;
import com.maplemetric.analysis.domain.model.StatisticsInsightFacts;
import com.maplemetric.analysis.infrastructure.properties.StatisticsInsightProperties;
import com.maplemetric.statistics.api.StatisticsFactQuery;
import com.maplemetric.statistics.api.StatisticsHistoryFact;
import com.maplemetric.statistics.api.StatisticsSubject;
import com.maplemetric.statistics.api.StatisticsSubjectType;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 대상을 돌며 아직 없는 설명을 만들어 저장한다.
 *
 * 이미 있는 대상은 건너뛴다. 재실행이 비용을 늘리지 않아야 유료 호출을 안심하고
 * 다시 돌릴 수 있다.
 *
 * 한 대상의 실패가 나머지를 막지 않는다. 61개 중 하나가 실패했다고 전체가 멈추면
 * 매번 같은 지점에서 끊긴다.
 */
@Slf4j
@Service
public class StatisticsInsightGenerationService
        implements GenerateStatisticsInsightUseCase {

    private final StatisticsFactQuery statisticsFactQuery;
    private final StatisticsInsightFactsAssembler assembler;
    private final StatisticsInsightGenerator insightGenerator;
    private final SaveStatisticsInsightPort saveStatisticsInsightPort;
    private final StatisticsInsightProperties properties;

    public StatisticsInsightGenerationService(
            StatisticsFactQuery statisticsFactQuery,
            StatisticsInsightFactsAssembler assembler,
            StatisticsInsightGenerator insightGenerator,
            SaveStatisticsInsightPort saveStatisticsInsightPort,
            StatisticsInsightProperties properties
    ) {
        this.statisticsFactQuery = statisticsFactQuery;
        this.assembler = assembler;
        this.insightGenerator = insightGenerator;
        this.saveStatisticsInsightPort = saveStatisticsInsightPort;
        this.properties = properties;
    }

    @Override
    public GenerateStatisticsInsightOutcome generate() {
        int generated = 0;
        int skipped = 0;
        int failed = 0;

        for (StatisticsSubject subject : statisticsFactQuery.listSubjects()) {
            if (generated >= properties.maxPerRun()) {
                break;
            }

            try {
                if (generateOne(subject)) {
                    generated++;
                } else {
                    skipped++;
                }
            } catch (RuntimeException exception) {
                failed++;

                log.error(
                        "통계 인사이트 생성에 실패했습니다. subject={}",
                        subject.slug(),
                        exception
                );
            }
        }

        log.info(
                "통계 인사이트 생성을 마쳤습니다. "
                        + "생성={}, 건너뜀={}, 실패={}",
                generated,
                skipped,
                failed
        );

        return new GenerateStatisticsInsightOutcome(
                generated,
                skipped,
                failed
        );
    }

    /**
     * @return 실제로 생성했으면 {@code true}, 건너뛰었으면 {@code false}
     */
    private boolean generateOne(StatisticsSubject subject) {
        StatisticsHistoryFact history = loadHistory(subject);

        LocalDate asOf = history.range() == null
                ? null
                : history.range().lastAsOf();

        // 수집된 기준일이 없으면 설명할 대상 자체가 없다.
        if (asOf == null) {
            return false;
        }

        if (saveStatisticsInsightPort.exists(
                subject.type(),
                subject.slug(),
                properties.rangePreset(),
                asOf
        )) {
            return false;
        }

        StatisticsInsightFacts facts = assembler.assemble(history);
        StatisticsInsightPreview preview = insightGenerator.generate(facts);

        saveStatisticsInsightPort.save(
                properties.rangePreset(),
                asOf,
                preview.headline(),
                String.join("\n", preview.lines()),
                facts,
                null
        );

        return true;
    }

    private StatisticsHistoryFact loadHistory(StatisticsSubject subject) {
        if (subject.type() == StatisticsSubjectType.JOB) {
            return statisticsFactQuery.getJobHistoryFact(
                    subject.slug(),
                    properties.rangePreset(),
                    null,
                    null
            );
        }

        return statisticsFactQuery.getWorldHistoryFact(
                subject.slug(),
                properties.rangePreset(),
                null,
                null
        );
    }
}
