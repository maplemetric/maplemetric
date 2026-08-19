package com.maplemetric.analysis.application.service;

import com.maplemetric.analysis.api.GenerateStatisticsInsightOutcome;
import com.maplemetric.analysis.api.GenerateStatisticsInsightUseCase;
import com.maplemetric.analysis.application.port.out.SaveStatisticsInsightPort;
import com.maplemetric.analysis.application.result.StatisticsInsightPreview;
import com.maplemetric.analysis.domain.model.StatisticsInsightFacts;
import com.maplemetric.analysis.application.properties.StatisticsInsightProperties;
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
        int alreadyExists = 0;
        int noHistory = 0;
        int failed = 0;
        boolean hasMore = false;

        for (StatisticsSubject subject : statisticsFactQuery.listSubjects()) {
            if (generated >= properties.maxPerRun()) {
                // 이 대상을 아직 처리하지 않은 채로 멈춘다. 남은 것이 있다는 사실을
                // 짐작이 아니라 이 지점에서 확정한다.
                hasMore = true;

                break;
            }

            try {
                switch (generateOne(subject)) {
                    case GENERATED -> generated++;
                    case ALREADY_EXISTS -> alreadyExists++;
                    case NO_HISTORY -> noHistory++;
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
                        + "생성={}, 이미있음={}, 기준일없음={}, 실패={}, 남음={}",
                generated,
                alreadyExists,
                noHistory,
                failed,
                hasMore
        );

        return new GenerateStatisticsInsightOutcome(
                generated,
                alreadyExists,
                noHistory,
                failed,
                hasMore
        );
    }

    /** 대상 하나를 처리한 결과다. */
    private enum SubjectOutcome {

        GENERATED,

        /** 이미 만들어 뒀다. 재실행이 비용을 늘리지 않는다는 뜻이라 조치가 없다. */
        ALREADY_EXISTS,

        /** 설명할 기준일이 없다. 수집이 비어 있다는 뜻이라 확인이 필요하다. */
        NO_HISTORY
    }

    private SubjectOutcome generateOne(StatisticsSubject subject) {
        StatisticsHistoryFact history = loadHistory(subject);

        LocalDate asOf = history.range() == null
                ? null
                : history.range().lastAsOf();

        // 수집된 기준일이 없으면 설명할 대상 자체가 없다.
        if (asOf == null) {
            return SubjectOutcome.NO_HISTORY;
        }

        if (saveStatisticsInsightPort.exists(
                subject.type(),
                subject.slug(),
                properties.rangePreset(),
                asOf
        )) {
            return SubjectOutcome.ALREADY_EXISTS;
        }

        StatisticsInsightFacts facts = assembler.assemble(history);
        StatisticsInsightPreview preview = insightGenerator.generate(facts);

        saveStatisticsInsightPort.save(
                properties.rangePreset(),
                asOf,
                preview.headline(),
                String.join("\n", preview.lines()),
                facts,
                preview.model()
        );

        return SubjectOutcome.GENERATED;
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
