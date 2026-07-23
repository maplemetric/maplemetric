package com.maplemetric.analysis.application.service;

import com.maplemetric.analysis.application.result.InsightResult;
import com.maplemetric.analysis.domain.model.InsightFacts;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TemplateInsightGenerator implements InsightGenerator {

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
