package com.maplemetric.analysis.application.service;

import com.maplemetric.analysis.application.result.InsightResult;
import com.maplemetric.analysis.application.result.StatisticsInsightPreview;
import com.maplemetric.analysis.domain.model.InsightFacts;
import com.maplemetric.analysis.domain.model.StatisticsInsightFact;
import com.maplemetric.analysis.domain.model.StatisticsInsightFacts;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TemplateInsightGenerator
        implements InsightGenerator, StatisticsInsightGenerator {

    @Override
    public InsightResult generate(InsightFacts facts) {
        if (facts == null) {
            throw new IllegalArgumentException(
                    "인사이트 Fact는 비어 있을 수 없습니다."
            );
        }

        List<String> evidence = facts.evidence()
                .stream()
                .map(item -> item.label() + ": " + item.value())
                .toList();

        return new InsightResult(
                facts.sentiment(),
                createHeadline(facts),
                createSummary(facts),
                evidence
        );
    }

    /**
     * 통계 Fact를 줄 단위로 펼친다.
     *
     * 문장을 만들지 않는다. 수치와 단위를 그대로 늘어놓아 무엇이 재료로 넘어가는지
     * 눈으로 확인하는 용도이며, 문장 생성은 #166이 맡는다.
     */
    @Override
    public StatisticsInsightPreview generate(
            StatisticsInsightFacts facts
    ) {
        if (facts == null) {
            throw new IllegalArgumentException(
                    "통계 인사이트 Fact는 비어 있을 수 없습니다."
            );
        }

        List<String> lines = new ArrayList<>(
                facts.facts()
                        .stream()
                        .map(fact -> createLine(fact))
                        .toList()
        );

        lines.addAll(facts.limitations());

        return new StatisticsInsightPreview(
                facts.subject().name() + " 요약",
                lines
        );
    }

    private String createLine(StatisticsInsightFact fact) {
        if (fact.value() == null) {
            return fact.type().name();
        }

        String line = fact.type().name()
                + ": " + fact.value().toPlainString()
                + " " + fact.unit().name();

        if (fact.trend() == null) {
            return line;
        }

        return line + " (" + fact.trend().name() + ")";
    }

    private String createHeadline(InsightFacts facts) {
        return facts.subject() + " 요약";
    }

    private String createSummary(InsightFacts facts) {
        return switch (facts.sentiment()) {
            case POSITIVE ->
                    facts.subject()
                            + "에서 긍정적인 흐름이 확인되었습니다.";
            case NEGATIVE ->
                    facts.subject()
                            + "에서 부정적인 흐름이 확인되었습니다.";
            case NEUTRAL ->
                    facts.subject()
                            + "에서 뚜렷한 변화가 확인되지 않았습니다.";
            case MIXED ->
                    facts.subject()
                            + "에서 서로 다른 흐름이 함께 확인되었습니다.";
        };
    }
}
