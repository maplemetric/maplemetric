package com.maplemetric.analysis.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.analysis.application.result.InsightResult;
import com.maplemetric.analysis.application.service.TemplateInsightGenerator;
import com.maplemetric.analysis.domain.model.InsightFacts;
import com.maplemetric.analysis.domain.model.InsightSentiment;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@ExtendWith({
        MockitoExtension.class,
        OutputCaptureExtension.class
})
class OpenAiInsightGeneratorTest {

    private static final String API_KEY = "test-openai-secret-key";
    private static final String MODEL = "test-model";
    private static final String SUBJECT = "히어로 검색 비중";
    private static final String EVIDENCE_VALUE = "완만한 증가";
    private static final String FORMATTED_EVIDENCE =
            "검색 비중: " + EVIDENCE_VALUE;

    @Mock
    private OpenAiResponsesClient responsesClient;

    private TemplateInsightGenerator fallbackGenerator;
    private OpenAiInsightGenerator insightGenerator;

    @BeforeEach
    void setUp() {
        fallbackGenerator = new TemplateInsightGenerator();
        insightGenerator = createGenerator(
                createProperties(
                        true,
                        API_KEY,
                        MODEL
                )
        );
    }

    @Test
    void 검증된StructuredOutput을인사이트로반환한다() {
        given(responsesClient.create(
                org.mockito.ArgumentMatchers.any(
                        OpenAiResponsesRequest.class
                )
        )).willReturn(
                createResponse(
                        "completed",
                        createOutputText(
                                createInsightJson(
                                        "POSITIVE",
                                        "검색 흐름 상승",
                                        "긍정적인 변화가 이어지고 있습니다.",
                                        FORMATTED_EVIDENCE
                                )
                        )
                )
        );

        InsightResult result =
                insightGenerator.generate(createFacts());

        assertThat(result)
                .isEqualTo(
                        new InsightResult(
                                InsightSentiment.POSITIVE,
                                "검색 흐름 상승",
                                "긍정적인 변화가 이어지고 있습니다.",
                                List.of(FORMATTED_EVIDENCE)
                        )
                );
    }

    @Test
    void 첫번째출력이아니어도메시지의OutputText를찾는다() {
        OpenAiResponsesResponse.Output unrelatedOutput =
                new OpenAiResponsesResponse.Output(
                        "reasoning",
                        List.of()
                );

        given(responsesClient.create(
                org.mockito.ArgumentMatchers.any(
                        OpenAiResponsesRequest.class
                )
        )).willReturn(
                createResponse(
                        "completed",
                        unrelatedOutput,
                        createOutputText(
                                createInsightJson(
                                        "POSITIVE",
                                        "검색 흐름 상승",
                                        "긍정적인 변화가 이어지고 있습니다.",
                                        FORMATTED_EVIDENCE
                                )
                        )
                )
        );

        InsightResult result =
                insightGenerator.generate(createFacts());

        assertThat(result.headline())
                .isEqualTo("검색 흐름 상승");
    }

    @ParameterizedTest
    @MethodSource("unavailableProperties")
    void 연동을사용할수없으면외부요청없이템플릿을반환한다(
            OpenAiProperties properties
    ) {
        OpenAiInsightGenerator unavailableGenerator =
                createGenerator(properties);

        InsightFacts facts = createFacts();

        assertThat(unavailableGenerator.generate(facts))
                .isEqualTo(fallbackGenerator.generate(facts));

        verify(responsesClient, never())
                .create(
                        org.mockito.ArgumentMatchers.any(
                                OpenAiResponsesRequest.class
                        )
                );
    }

    @Test
    void Fact가null이면외부요청없이기존검증예외를유지한다() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> insightGenerator.generate(null));

        verify(responsesClient, never())
                .create(
                        org.mockito.ArgumentMatchers.any(
                                OpenAiResponsesRequest.class
                        )
                );
    }

    @ParameterizedTest
    @MethodSource("invalidResponses")
    void 사용할수없는응답이면템플릿으로대체한다(
            OpenAiResponsesResponse response
    ) {
        given(responsesClient.create(
                org.mockito.ArgumentMatchers.any(
                        OpenAiResponsesRequest.class
                )
        )).willReturn(response);

        InsightFacts facts = createFacts();

        assertThat(insightGenerator.generate(facts))
                .isEqualTo(fallbackGenerator.generate(facts));
    }

    @ParameterizedTest
    @MethodSource("providerExceptions")
    void 외부API오류이면비밀값을로그에남기지않고템플릿으로대체한다(
            RestClientException exception,
            CapturedOutput output
    ) {
        given(responsesClient.create(
                org.mockito.ArgumentMatchers.any(
                        OpenAiResponsesRequest.class
                )
        )).willThrow(exception);

        InsightFacts facts = createFacts();

        assertThat(insightGenerator.generate(facts))
                .isEqualTo(fallbackGenerator.generate(facts));

        assertThat(output)
                .contains("failureType=")
                .doesNotContain(API_KEY)
                .doesNotContain(SUBJECT)
                .doesNotContain(EVIDENCE_VALUE)
                .doesNotContain("Authorization");
    }

    @Test
    void 응답검증실패로그에도입력데이터를남기지않는다(
            CapturedOutput output
    ) {
        given(responsesClient.create(
                org.mockito.ArgumentMatchers.any(
                        OpenAiResponsesRequest.class
                )
        )).willReturn(
                createResponse(
                        "completed",
                        createOutputText("{malformed-json")
                )
        );

        InsightFacts facts = createFacts();

        assertThat(insightGenerator.generate(facts))
                .isEqualTo(fallbackGenerator.generate(facts));

        assertThat(output)
                .contains("failureType=JsonParseException")
                .doesNotContain(API_KEY)
                .doesNotContain(SUBJECT)
                .doesNotContain(EVIDENCE_VALUE)
                .doesNotContain("Authorization");
    }

    private OpenAiInsightGenerator createGenerator(
            OpenAiProperties properties
    ) {
        return new OpenAiInsightGenerator(
                responsesClient,
                properties,
                new ObjectMapper(),
                fallbackGenerator
        );
    }

    private static Stream<OpenAiProperties> unavailableProperties() {
        return Stream.of(
                createProperties(false, API_KEY, MODEL),
                createProperties(true, "", MODEL),
                createProperties(true, API_KEY, "")
        );
    }

    private static Stream<Arguments> invalidResponses() {
        return Stream.of(
                Arguments.of((OpenAiResponsesResponse) null),
                Arguments.of(createResponse("incomplete")),
                Arguments.of(createResponse("completed")),
                Arguments.of(
                        createResponse(
                                "completed",
                                new OpenAiResponsesResponse.Output(
                                        "message",
                                        List.of(
                                                new OpenAiResponsesResponse
                                                        .Content(
                                                        "refusal",
                                                        null,
                                                        "요청 거절"
                                                )
                                        )
                                )
                        )
                ),
                Arguments.of(
                        createResponse(
                                "completed",
                                createOutputText("")
                        )
                ),
                Arguments.of(
                        createResponse(
                                "completed",
                                createOutputText(
                                        createInsightJson(
                                                "NEGATIVE",
                                                "검색 흐름 하락",
                                                "부정적인 변화입니다.",
                                                FORMATTED_EVIDENCE
                                        )
                                )
                        )
                ),
                Arguments.of(
                        createResponse(
                                "completed",
                                createOutputText(
                                        createInsightJson(
                                                "POSITIVE",
                                                "검색 흐름 상승",
                                                "긍정적인 변화입니다.",
                                                "검색 비중: 다른 값"
                                        )
                                )
                        )
                ),
                Arguments.of(
                        createResponse(
                                "completed",
                                createOutputText(
                                        createInsightJson(
                                                "POSITIVE",
                                                "검색 흐름 1단계 상승",
                                                "긍정적인 변화입니다.",
                                                FORMATTED_EVIDENCE
                                        )
                                )
                        )
                ),
                Arguments.of(
                        createResponse(
                                "completed",
                                createOutputText(
                                        createInsightJson(
                                                "POSITIVE",
                                                "검색 흐름 상승",
                                                "긍정적인 변화가 2회 "
                                                        + "이어졌습니다.",
                                                FORMATTED_EVIDENCE
                                        )
                                )
                        )
                ),
                Arguments.of(
                        createResponse(
                                "completed",
                                createOutputText("{malformed-json")
                        )
                )
        );
    }

    private static Stream<RestClientException> providerExceptions() {
        return Stream.of(
                HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                ),
                HttpServerErrorException.create(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Server Error",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                ),
                new ResourceAccessException(
                        "timeout",
                        new SocketTimeoutException("read timeout")
                ),
                new RestClientException("connection failed")
        );
    }

    private static OpenAiProperties createProperties(
            boolean enabled,
            String key,
            String model
    ) {
        return new OpenAiProperties(
                enabled,
                "https://api.openai.test",
                key,
                model,
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)
        );
    }

    private InsightFacts createFacts() {
        return new InsightFacts(
                SUBJECT,
                InsightSentiment.POSITIVE,
                List.of(
                        new InsightFacts.Evidence(
                                "검색 비중",
                                EVIDENCE_VALUE
                        )
                )
        );
    }

    private static OpenAiResponsesResponse createResponse(
            String status,
            OpenAiResponsesResponse.Output... output
    ) {
        return new OpenAiResponsesResponse(
                status,
                List.of(output)
        );
    }

    private static OpenAiResponsesResponse.Output createOutputText(
            String text
    ) {
        return new OpenAiResponsesResponse.Output(
                "message",
                List.of(
                        new OpenAiResponsesResponse.Content(
                                "output_text",
                                text,
                                null
                        )
                )
        );
    }

    private static String createInsightJson(
            String sentiment,
            String headline,
            String summary,
            String evidence
    ) {
        return """
                {
                  "sentiment": "%s",
                  "headline": "%s",
                  "summary": "%s",
                  "evidence": ["%s"]
                }
                """.formatted(
                sentiment,
                headline,
                summary,
                evidence
        );
    }
}
