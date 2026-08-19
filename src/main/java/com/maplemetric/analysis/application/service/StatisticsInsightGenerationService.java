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
            boolean withinLimit = generated < properties.maxPerRun();

            try {
                switch (generateOne(subject, withinLimit)) {
                    case GENERATED -> generated++;
                    case ALREADY_EXISTS -> alreadyExists++;
                    case NO_HISTORY -> noHistory++;
                    case BLOCKED_BY_LIMIT -> hasMore = true;
                }
            } catch (RuntimeException exception) {
                failed++;

                log.error(
                        "통계 인사이트 생성에 실패했습니다. subject={}",
                        subject.slug(),
                        exception
                );
            }

            if (hasMore) {
                // 만들어야 하는데 한도에 막힌 대상을 만났다. 더 볼 것 없이 확정이다.
                //
                // 한도에 닿았다는 것만으로 멈추지 않는 이유는, 뒤가 전부 건너뛸
                // 대상이면 만들 것이 없는데도 다시 호출하라고 알리기 때문이다.
                // 만들 것이 실제로 남았는지 확인될 때까지 훑는다.
                break;
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
        NO_HISTORY,

        /** 만들어야 하는데 실행 한도에 막혔다. 다음 실행이 필요하다는 뜻이다. */
        BLOCKED_BY_LIMIT
    }

    private SubjectOutcome generateOne(
            StatisticsSubject subject,
            boolean withinLimit
    ) {
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

        // 여기까지 왔으면 만들어야 하는 대상이다. 한도는 이 시점에 본다. 앞에서 보면
        // 건너뛸 대상까지 남은 것으로 세게 된다.
        if (!withinLimit) {
            return SubjectOutcome.BLOCKED_BY_LIMIT;
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
