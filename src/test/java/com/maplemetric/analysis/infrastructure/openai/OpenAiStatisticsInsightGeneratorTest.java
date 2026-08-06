package com.maplemetric.analysis.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.analysis.application.result.StatisticsInsightPreview;
import com.maplemetric.analysis.application.service.TemplateInsightGenerator;
import com.maplemetric.analysis.domain.model.InsightEvidence;
import com.maplemetric.analysis.domain.model.StatisticsInsightFact;
import com.maplemetric.analysis.domain.model.StatisticsInsightFactType;
import com.maplemetric.analysis.domain.model.StatisticsInsightFacts;
import com.maplemetric.analysis.domain.model.StatisticsInsightUnit;
import com.maplemetric.statistics.api.StatisticsSubjectType;
import com.maplemetric.statistics.api.StatisticsTrend;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@ExtendWith({
        MockitoExtension.class,
        OutputCaptureExtension.class
})
class OpenAiStatisticsInsightGeneratorTest {

    private static final String API_KEY = "test-openai-secret-key";
    private static final String MODEL = "test-model";
    private static final String SUBJECT_NAME = "히어로";
    private static final String LIMITATION =
            "전체 이용자 모집단이 아니라 수집한 종합 랭킹 표본입니다.";
    private static final String EVIDENCE_LINE = "비중 변화: 2.34";

    @Mock
    private OpenAiResponsesClient responsesClient;

    private TemplateInsightGenerator fallbackGenerator;
    private OpenAiStatisticsInsightGenerator insightGenerator;

    @BeforeEach
    void setUp() {
        fallbackGenerator = new TemplateInsightGenerator();
        insightGenerator = createGenerator(
                createProperties(true, API_KEY, MODEL)
        );
    }

    @Test
    void 검증된응답을설명으로반환하고한계를함께담는다() {
        given(responsesClient.create(any()))
                .willReturn(createResponse(
                        "completed",
                        createOutputText(createJson(
                                "UP",
                                "히어로 비중이 늘었습니다",
                                "만렙 상위권에서 비중이 완만하게 올랐습니다."
                        ))
                ));

        StatisticsInsightPreview preview =
                insightGenerator.generate(createFacts(StatisticsTrend.UP));

        assertThat(preview.headline()).isEqualTo("히어로 비중이 늘었습니다");

        // 한계는 항상 문장과 함께 나가야 표본을 전체로 읽지 않는다.
        assertThat(preview.lines()).containsExactly(
                "만렙 상위권에서 비중이 완만하게 올랐습니다.",
                LIMITATION
        );
    }

    static Stream<String> 버려야하는응답() {
        return Stream.of(
                // 방향을 바꿨다. Statistics가 UP으로 판정했는데 DOWN이다.
                createJson("DOWN", "비중이 줄었습니다", "하락했습니다."),

                // 문장에 숫자가 있다. 수치는 Fact가 가지며 문장이 다시 적지 않는다.
                createJson("UP", "비중 2%p 상승", "2%p 올랐습니다."),

                // 근거를 지어냈다.
                """
                {
                  "trend": "UP",
                  "headline": "비중이 늘었습니다",
                  "summary": "완만하게 올랐습니다.",
                  "evidence": ["출처 없음: 지어낸 값"]
                }
                """,

                "{malformed-json"
        );
    }

    @ParameterizedTest
    @MethodSource("버려야하는응답")
    void 지어낸응답은버리고템플릿으로대체한다(String json) {
        given(responsesClient.create(any()))
                .willReturn(createResponse(
                        "completed",
                        createOutputText(json)
                ));

        StatisticsInsightPreview preview =
                insightGenerator.generate(createFacts(StatisticsTrend.UP));

        // 템플릿은 Fact를 그대로 펼치므로 지어낸 문장이 화면에 남지 않는다.
        assertThat(preview.headline()).isEqualTo(SUBJECT_NAME + " 요약");
    }

    @Test
    void 거절응답도템플릿으로대체한다() {
        given(responsesClient.create(any()))
                .willReturn(createResponse(
                        "completed",
                        new OpenAiResponsesResponse.Output(
                                "message",
                                List.of(new OpenAiResponsesResponse.Content(
                                        "refusal",
                                        null,
                                        "요청 거절"
                                ))
                        )
                ));

        assertThat(
                insightGenerator.generate(createFacts(StatisticsTrend.UP))
                        .headline()
        ).isEqualTo(SUBJECT_NAME + " 요약");
    }

    static Stream<OpenAiProperties> 사용할수없는설정() {
        return Stream.of(
                createProperties(false, API_KEY, MODEL),
                createProperties(true, "", MODEL),
                createProperties(true, API_KEY, "")
        );
    }

    @ParameterizedTest
    @MethodSource("사용할수없는설정")
    void 연동을사용할수없으면외부요청없이템플릿을반환한다(
            OpenAiProperties properties
    ) {
        assertThat(
                createGenerator(properties)
                        .generate(createFacts(StatisticsTrend.UP))
                        .headline()
        ).isEqualTo(SUBJECT_NAME + " 요약");

        verify(responsesClient, never()).create(any());
    }

    static Stream<RestClientException> 외부오류() {
        return Stream.of(
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
                )
        );
    }

    /**
     * 로그에 Key·Prompt·Fact 값을 남기지 않는다.
     */
    @ParameterizedTest
    @MethodSource("외부오류")
    void 외부오류는비밀값을남기지않고템플릿으로대체한다(
            RestClientException exception,
            CapturedOutput output
    ) {
        given(responsesClient.create(any())).willThrow(exception);

        assertThat(
                insightGenerator.generate(createFacts(StatisticsTrend.UP))
                        .headline()
        ).isEqualTo(SUBJECT_NAME + " 요약");

        assertThat(output)
                .contains("failureType=")
                .doesNotContain(API_KEY)
                .doesNotContain(LIMITATION)
                .doesNotContain("Authorization");
    }

    /**
     * 검증하는 값은 반드시 요청에도 담겨야 한다.
     *
     * evidence를 보내지 않으면 모델이 되돌려줄 수 없고, 그러면 모든 응답이 거부돼
     * 연동이 조용히 죽는다. Stub 테스트는 정답을 직접 넣어주므로 이 어긋남을
     * 잡지 못한다. 요청 본문 자체를 확인한다.
     */
    @Test
    void 검증에쓰는evidence를요청에담는다() {
        StatisticsInsightFacts facts = createFacts(StatisticsTrend.UP);

        OpenAiResponsesRequest request =
                OpenAiResponsesRequest.fromStatistics(facts, MODEL);

        assertThat(request.input()).contains(EVIDENCE_LINE);
    }

    @Test
    void Fact가null이면외부요청없이템플릿규칙을따른다() {
        org.assertj.core.api.Assertions
                .assertThatIllegalArgumentException()
                .isThrownBy(() -> insightGenerator.generate(null));

        verify(responsesClient, never()).create(any());
    }

    private OpenAiStatisticsInsightGenerator createGenerator(
            OpenAiProperties properties
    ) {
        return new OpenAiStatisticsInsightGenerator(
                responsesClient,
                properties,
                new ObjectMapper(),
                fallbackGenerator
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

    private StatisticsInsightFacts createFacts(StatisticsTrend trend) {
        return new StatisticsInsightFacts(
                new StatisticsInsightFacts.Subject(
                        StatisticsSubjectType.JOB,
                        "hero",
                        SUBJECT_NAME
                ),
                new StatisticsInsightFacts.Period(
                        "90D",
                        LocalDate.of(2026, 5, 6),
                        LocalDate.of(2026, 8, 3),
                        13,
                        77
                ),
                List.of(new StatisticsInsightFact(
                        StatisticsInsightFactType.SHARE_CHANGE,
                        new BigDecimal("2.34"),
                        StatisticsInsightUnit.PERCENTAGE_POINT,
                        trend,
                        List.of(new InsightEvidence("비중 변화", "2.34"))
                )),
                new StatisticsInsightFacts.Source(
                        "NEXON_OPEN_API",
                        java.time.Instant.parse("2026-08-03T00:40:00Z"),
                        2000,
                        true
                ),
                List.of(LIMITATION)
        );
    }

    private static OpenAiResponsesResponse createResponse(
            String status,
            OpenAiResponsesResponse.Output... output
    ) {
        return new OpenAiResponsesResponse(status, List.of(output));
    }

    private static OpenAiResponsesResponse.Output createOutputText(
            String text
    ) {
        return new OpenAiResponsesResponse.Output(
                "message",
                List.of(new OpenAiResponsesResponse.Content(
                        "output_text",
                        text,
                        null
                ))
        );
    }

    private static String createJson(
            String trend,
            String headline,
            String summary
    ) {
        return """
                {
                  "trend": "%s",
                  "headline": "%s",
                  "summary": "%s",
                  "evidence": ["%s"]
                }
                """.formatted(trend, headline, summary, EVIDENCE_LINE);
    }
}
