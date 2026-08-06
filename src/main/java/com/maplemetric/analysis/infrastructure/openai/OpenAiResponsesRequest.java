package com.maplemetric.analysis.infrastructure.openai;

import com.maplemetric.analysis.domain.model.InsightFacts;
import com.maplemetric.analysis.domain.model.InsightSentiment;
import com.maplemetric.analysis.domain.model.StatisticsInsightFacts;
import com.maplemetric.statistics.api.StatisticsTrend;
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

    /**
     * 한계 문구를 담는 입력 항목 이름이다.
     *
     * 지시문과 입력이 같은 이름을 써야 "이 항목을 반복하지 말라"는 지시가 가리킬
     * 대상이 생긴다. 한쪽만 바뀌면 지시가 조용히 무의미해지므로 상수로 묶는다.
     */
    private static final String ALREADY_SHOWN_LABEL = "already_shown";

    /**
     * 통계 설명 지시문이다.
     *
     * 숫자를 쓰지 못하게 막는다. 수치는 Fact가 이미 구조화된 값으로 갖고 있고
     * 화면이 그것을 그린다. 문장이 수치를 다시 적으면 둘이 어긋날 수 있고, 그
     * 어긋남은 검증하기 어렵다. 모델은 해석만 맡는다.
     *
     * 한계 문구는 설명과 별도로 화면에 나가므로 문장에서 반복하지 못하게 막는다.
     * 막지 않으면 두 문장 중 하나를 바로 아래 줄과 같은 말에 쓴다.
     *
     * 어떻게 쓸지도 함께 지시한다. 금지 조항만 주면 모델이 가장 안전한 공시문체로
     * 물러나고, 그 문장은 정확하지만 사용자가 읽지 않는다.
     */
    private static final String STATISTICS_INSTRUCTIONS = """
            주어진 통계 Fact만 사용해 한국어 설명을 작성하세요.
            서비스 화면에서 사용자가 읽을 문장입니다.
            보고서체 대신 담백한 존댓말로 쓰세요.
            headline은 무엇이 달라졌는지 한 줄로 말하세요.
            summary는 두 문장을 넘기지 마세요.
            %s 항목은 화면에 이미 따로 나갑니다. 그 내용을 다시 쓰지 마세요.
            trend와 evidence는 입력값을 그대로 반환하세요.
            headline과 summary에는 숫자를 쓰지 마세요.
            입력에 없는 사실, 원인, 전망을 추측하지 마세요.
            표본이 전체 이용자가 아니라는 점을 왜곡하지 마세요.
            """.formatted(ALREADY_SHOWN_LABEL);

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

    static OpenAiResponsesRequest fromStatistics(
            StatisticsInsightFacts facts,
            String model
    ) {
        return new OpenAiResponsesRequest(
                model,
                STATISTICS_INSTRUCTIONS,
                createStatisticsInput(facts),
                new Text(Format.statisticsInsight())
        );
    }

    /**
     * Prompt에는 Fact와 한계만 담는다.
     *
     * Entity·내부 오류·Secret·원본 대량 데이터는 넣지 않는다.
     */
    private static String createStatisticsInput(
            StatisticsInsightFacts facts
    ) {
        String factLines = facts.facts()
                .stream()
                .map(fact -> fact.type() + ": "
                        + (fact.value() == null
                                ? "없음"
                                : fact.value().toPlainString()
                                        + " " + fact.unit()))
                .collect(Collectors.joining("\n"));

        // evidence는 응답에서 그대로 돌려받아 검증하므로 반드시 요청에 담는다.
        // 검증과 같은 생성기를 써야 형식이 어긋나지 않는다.
        String evidenceLines = String.join(
                "\n",
                StatisticsInsightEvidences.lines(facts)
        );

        return """
                subject: %s (%s)
                period: %s ~ %s
                trend: %s
                facts:
                %s
                evidence:
                %s
                %s:
                %s
                """.formatted(
                facts.subject().name(),
                facts.subject().type(),
                facts.period() == null ? "없음" : facts.period().from(),
                facts.period() == null ? "없음" : facts.period().to(),
                StatisticsInsightTrends.of(facts),
                factLines,
                evidenceLines,
                ALREADY_SHOWN_LABEL,
                String.join("\n", facts.limitations())
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

        private static Format statisticsInsight() {
            return new Format(
                    "json_schema",
                    "maplemetric_statistics_insight",
                    true,
                    createStatisticsSchema()
            );
        }

        /**
         * {@code trend}와 {@code evidence}는 입력을 되돌려 받는 자리다.
         *
         * 모델이 다른 값을 넣으면 방향이나 근거를 지어낸 것이므로 응답을 버린다.
         */
        private static Map<String, Object> createStatisticsSchema() {
            return Map.of(
                    "type",
                    "object",
                    "properties",
                    Map.of(
                            "trend",
                            Map.of(
                                    "type",
                                    "string",
                                    "enum",
                                    List.of(StatisticsTrend.values())
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
                            "trend",
                            "headline",
                            "summary",
                            "evidence"
                    ),
                    "additionalProperties",
                    false
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
