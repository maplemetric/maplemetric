package com.maplemetric.analysis.infrastructure.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.analysis.application.result.InsightResult;
import com.maplemetric.analysis.application.service.InsightGenerator;
import com.maplemetric.analysis.application.service.TemplateInsightGenerator;
import com.maplemetric.analysis.domain.model.InsightFacts;
import com.maplemetric.analysis.domain.model.InsightSentiment;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

@Slf4j
@Primary
@Service
class OpenAiInsightGenerator implements InsightGenerator {

    private static final String COMPLETED_STATUS = "completed";
    private static final String MESSAGE_TYPE = "message";
    private static final String OUTPUT_TEXT_TYPE = "output_text";
    private static final String REFUSAL_TYPE = "refusal";

    private final OpenAiResponsesClient responsesClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;
    private final TemplateInsightGenerator fallbackGenerator;

    OpenAiInsightGenerator(
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
    public InsightResult generate(InsightFacts facts) {
        if (facts == null) {
            // 통계 Overload가 생겨 리터럴 null은 어느 쪽인지 정해지지 않는다.
            return fallbackGenerator.generate(facts);
        }

        if (!isAvailable()) {
            return fallbackGenerator.generate(facts);
        }

        try {
            OpenAiResponsesRequest request =
                    OpenAiResponsesRequest.from(
                            facts,
                            properties.model()
                    );

            OpenAiResponsesResponse response =
                    responsesClient.create(request);

            return convertResponse(facts, response);
        } catch (
                RestClientException
                | JsonProcessingException
                | OpenAiResponseInvalidException
                | IllegalArgumentException exception
        ) {
            log.warn(
                    "OpenAI 인사이트 생성에 실패하여 템플릿으로 "
                            + "대체합니다. failureType={}",
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

    private InsightResult convertResponse(
            InsightFacts facts,
            OpenAiResponsesResponse response
    ) throws JsonProcessingException {
        validateResponse(response);

        String outputText = findOutputText(response);
        GeneratedInsight generatedInsight =
                objectMapper.readValue(
                        outputText,
                        GeneratedInsight.class
                );

        validateGeneratedInsight(facts, generatedInsight);

        return new InsightResult(
                generatedInsight.sentiment(),
                generatedInsight.headline(),
                generatedInsight.summary(),
                generatedInsight.evidence()
        );
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

    private void validateGeneratedInsight(
            InsightFacts facts,
            GeneratedInsight generatedInsight
    ) {
        if (generatedInsight == null
                || generatedInsight.sentiment() != facts.sentiment()
                || !createEvidence(facts).equals(
                generatedInsight.evidence()
        )
                || containsDigit(generatedInsight.headline())
                || containsDigit(generatedInsight.summary())) {
            throw new OpenAiResponseInvalidException();
        }
    }

    private List<String> createEvidence(InsightFacts facts) {
        return facts.evidence()
                .stream()
                .map(item -> item.label() + ": " + item.value())
                .toList();
    }

    private boolean containsDigit(String value) {
        return value != null
                && value.chars()
                .anyMatch(character -> Character.isDigit(character));
    }

    private record GeneratedInsight(
            InsightSentiment sentiment,
            String headline,
            String summary,
            List<String> evidence
    ) {
    }
}
