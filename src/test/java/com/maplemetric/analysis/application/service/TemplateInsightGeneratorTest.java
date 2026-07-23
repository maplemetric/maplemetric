package com.maplemetric.analysis.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.maplemetric.analysis.application.result.InsightResult;
import com.maplemetric.analysis.domain.model.InsightFacts;
import com.maplemetric.analysis.domain.model.InsightSentiment;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TemplateInsightGeneratorTest {

    private TemplateInsightGenerator insightGenerator;

    @BeforeEach
    void setUp() {
        insightGenerator = new TemplateInsightGenerator();
    }

    @ParameterizedTest
    @MethodSource("sentimentSummaries")
    void 서버가확정한감정을요약에반영한다(
            InsightSentiment sentiment,
            String expectedSummary
    ) {
        InsightFacts facts = createFacts(sentiment);

        InsightResult result =
                insightGenerator.generate(facts);

        assertThat(result.sentiment()).isEqualTo(sentiment);
        assertThat(result.headline())
                .isEqualTo("히어로 검색 비중 요약");
        assertThat(result.summary())
                .isEqualTo(expectedSummary);
    }

    @Test
    void 근거이름과값을변경하지않고출력한다() {
        InsightFacts facts =
                createFacts(InsightSentiment.POSITIVE);

        InsightResult result =
                insightGenerator.generate(facts);

        assertThat(result.evidence())
                .containsExactly(
                        "검색 비중: 11.6% → 12.4% (+0.8%p)"
                );
    }

    @Test
    void 동일한Fact는동일한결과를반환한다() {
        InsightFacts facts =
                createFacts(InsightSentiment.MIXED);

        assertThat(insightGenerator.generate(facts))
                .isEqualTo(insightGenerator.generate(facts));
    }

    @Test
    void Fact가null이면생성할수없다() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> insightGenerator.generate(null)
                );
    }

    private InsightFacts createFacts(
            InsightSentiment sentiment
    ) {
        return new InsightFacts(
                "히어로 검색 비중",
                sentiment,
                List.of(
                        new InsightFacts.Evidence(
                                "검색 비중",
                                "11.6% → 12.4% (+0.8%p)"
                        )
                )
        );
    }

    private static Stream<Arguments> sentimentSummaries() {
        return Stream.of(
                Arguments.of(
                        InsightSentiment.POSITIVE,
                        "히어로 검색 비중에서 긍정적인 흐름이 "
                                + "확인되었습니다."
                ),
                Arguments.of(
                        InsightSentiment.NEGATIVE,
                        "히어로 검색 비중에서 부정적인 흐름이 "
                                + "확인되었습니다."
                ),
                Arguments.of(
                        InsightSentiment.NEUTRAL,
                        "히어로 검색 비중에서 뚜렷한 변화가 "
                                + "확인되지 않았습니다."
                ),
                Arguments.of(
                        InsightSentiment.MIXED,
                        "히어로 검색 비중에서 서로 다른 흐름이 "
                                + "함께 확인되었습니다."
                )
        );
    }
}
