package com.maplemetric.analysis.infrastructure.openai;

import com.maplemetric.analysis.domain.model.InsightFacts;
import com.maplemetric.analysis.domain.model.InsightSentiment;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

record OpenAiResponsesRequest(
        String model,
        String instructions,
        String input,
        Text text
) {

    private static final String INSTRUCTIONS = """
            주어진 데이터만 사용해 간결한 한국어 인사이트를 작성하세요.
            sentiment와 evidence는 입력값을 그대로 반환하세요.
            headline과 summary에는 숫자를 추가하지 마세요.
            """;

    static OpenAiResponsesRequest from(
            InsightFacts facts,
            String model
    ) {
        return new OpenAiResponsesRequest(
                model,
                INSTRUCTIONS,
                createInput(facts),
                new Text(Format.insight())
        );
    }

    private static String createInput(InsightFacts facts) {
        String evidence = facts.evidence()
                .stream()
                .map(item -> item.label() + ": " + item.value())
                .collect(Collectors.joining("\n"));

        return """
                subject: %s
                sentiment: %s
                evidence:
                %s
                """.formatted(
                facts.subject(),
                facts.sentiment(),
                evidence
        );
    }

    record Text(
            Format format
    ) {
    }

    record Format(
            String type,
            String name,
            boolean strict,
            Map<String, Object> schema
    ) {

        private static Format insight() {
            return new Format(
                    "json_schema",
                    "maplemetric_insight",
                    true,
                    createSchema()
            );
        }

        private static Map<String, Object> createSchema() {
            return Map.of(
                    "type",
                    "object",
                    "properties",
                    Map.of(
                            "sentiment",
                            Map.of(
                                    "type",
                                    "string",
                                    "enum",
                                    List.of(InsightSentiment.values())
                            ),
                            "headline",
                            Map.of("type", "string"),
                            "summary",
                            Map.of("type", "string"),
                            "evidence",
                            Map.of(
                                    "type",
                                    "array",
                                    "items",
                                    Map.of("type", "string")
                            )
                    ),
                    "required",
                    List.of(
                            "sentiment",
                            "headline",
                            "summary",
                            "evidence"
                    ),
                    "additionalProperties",
                    false
            );
        }
    }
}
