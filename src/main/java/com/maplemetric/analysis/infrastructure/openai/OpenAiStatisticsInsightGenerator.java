package com.maplemetric.analysis.infrastructure.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.analysis.application.result.StatisticsInsightPreview;
import com.maplemetric.analysis.application.service.StatisticsInsightGenerator;
import com.maplemetric.analysis.application.service.TemplateInsightGenerator;
import com.maplemetric.analysis.domain.model.StatisticsInsightFacts;
import com.maplemetric.statistics.api.StatisticsTrend;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

/**
 * 통계 Fact를 문장으로 옮긴다.
 *
 * 모델은 해석만 한다. 수치·방향·근거는 Statistics가 이미 확정한 값이며, 응답이
 * 그것을 바꾸거나 새로 만들면 버리고 템플릿으로 되돌아간다.
 *
 * 실패해도 화면이 비지 않는다. 어떤 이유로든 응답을 쓸 수 없으면
 * {@link TemplateInsightGenerator}가 같은 Fact로 결정론적 결과를 만든다.
 */
@Slf4j
@Primary
@Service
class OpenAiStatisticsInsightGenerator implements StatisticsInsightGenerator {

    private static final String COMPLETED_STATUS = "completed";
    private static final String MESSAGE_TYPE = "message";
    private static final String OUTPUT_TEXT_TYPE = "output_text";
    private static final String REFUSAL_TYPE = "refusal";

    private final OpenAiResponsesClient responsesClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;
    private final TemplateInsightGenerator fallbackGenerator;

    OpenAiStatisticsInsightGenerator(
            OpenAiResponsesClient responsesClient,
            OpenAiProperties properties,
            ObjectMapper objectMapper,
            TemplateInsightGenerator fallbackGenerator
    ) {
        this.responsesClient = responsesClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.fallbackGenerator = fallbackGenerator;
    }

    @Override
    public StatisticsInsightPreview generate(StatisticsInsightFacts facts) {
        if (facts == null || !isAvailable()) {
            return fallbackGenerator.generate(facts);
        }

        try {
            OpenAiResponsesResponse response = responsesClient.create(
                    OpenAiResponsesRequest.fromStatistics(
                            facts,
                            properties.model()
                    )
            );

            return convertResponse(facts, response);
        } catch (
                RestClientException
                | JsonProcessingException
                | OpenAiResponseInvalidException
                | IllegalArgumentException exception
        ) {
            // 예외 메시지에 Prompt나 응답 전문이 실릴 수 있어 종류만 남긴다.
            log.warn(
                    "OpenAI 통계 인사이트 생성에 실패하여 템플릿으로 "
                            + "대체합니다. subject={}, failureType={}",
                    facts.subject().slug(),
                    exception.getClass().getSimpleName()
            );

            return fallbackGenerator.generate(facts);
        }
    }

    private boolean isAvailable() {
        return properties.enabled()
                && StringUtils.hasText(properties.key())
                && StringUtils.hasText(properties.model());
    }

    private StatisticsInsightPreview convertResponse(
            StatisticsInsightFacts facts,
            OpenAiResponsesResponse response
    ) throws JsonProcessingException {
        validateResponse(response);

        GeneratedInsight generated = objectMapper.readValue(
                findOutputText(response),
                GeneratedInsight.class
        );

        validateGenerated(facts, generated);

        List<String> lines = new ArrayList<>();
        lines.add(generated.summary());
        lines.addAll(facts.limitations());

        return new StatisticsInsightPreview(
                generated.headline(),
                lines,
                toModel(response)
        );
    }

    /**
     * 실제로 응답한 모델을 생성 출처로 쓴다.
     *
     * 요청한 이름은 별칭일 수 있고 그때는 응답이 해석된 스냅샷을 알려준다. 나중에
     * 문장이 달라진 이유를 찾을 때 필요한 것은 해석된 쪽이다.
     *
     * 응답에 없으면 요청값으로 되돌아간다. 문장 자체는 멀쩡하므로 출처 하나 때문에
     * 버리고 템플릿으로 내려가면 화면이 더 나빠진다.
     */
    private String toModel(OpenAiResponsesResponse response) {
        return StringUtils.hasText(response.model())
                ? response.model()
                : properties.model();
    }

    private void validateResponse(OpenAiResponsesResponse response) {
        if (response == null
                || !COMPLETED_STATUS.equals(response.status())
                || response.output() == null
                || response.output().isEmpty()
                || containsRefusal(response)) {
            throw new OpenAiResponseInvalidException();
        }
    }

    private boolean containsRefusal(OpenAiResponsesResponse response) {
        return response.output()
                .stream()
                .filter(item -> item != null && item.content() != null)
                .flatMap(item -> item.content().stream())
                .anyMatch(content -> content != null
                        && (REFUSAL_TYPE.equals(content.type())
                        || StringUtils.hasText(content.refusal())));
    }

    private String findOutputText(OpenAiResponsesResponse response) {
        return response.output()
                .stream()
                .filter(item -> item != null
                        && MESSAGE_TYPE.equals(item.type())
                        && item.content() != null)
                .flatMap(item -> item.content().stream())
                .filter(content -> content != null
                        && OUTPUT_TEXT_TYPE.equals(content.type())
                        && StringUtils.hasText(content.text()))
                .map(content -> content.text())
                .findFirst()
                .orElseThrow(() -> new OpenAiResponseInvalidException());
    }

    /**
     * 지어낸 응답을 버린다.
     *
     * 방향과 근거는 입력을 그대로 되돌려 받아야 한다. 다른 값이면 모델이 판단을
     * 만들어낸 것이다.
     *
     * 문장에 숫자가 있으면 버린다. 수치는 Fact가 구조화된 값으로 갖고 있으므로
     * 문장이 숫자를 다시 적을 이유가 없고, 적는 순간 화면의 수치와 어긋날 수 있다.
     */
    private void validateGenerated(
            StatisticsInsightFacts facts,
            GeneratedInsight generated
    ) {
        if (generated == null
                || generated.trend() != StatisticsInsightTrends.of(facts)
                || !StatisticsInsightEvidences.lines(facts)
                        .equals(generated.evidence())
                || containsDigit(generated.headline())
                || containsDigit(generated.summary())) {
            throw new OpenAiResponseInvalidException();
        }
    }

    private boolean containsDigit(String value) {
        return value == null
                || value.chars()
                .anyMatch(character -> Character.isDigit(character));
    }

    private record GeneratedInsight(
            StatisticsTrend trend,
            String headline,
            String summary,
            List<String> evidence
    ) {
    }
}
