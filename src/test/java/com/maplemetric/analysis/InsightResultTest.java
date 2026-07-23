package com.maplemetric.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class InsightResultTest {

    @Test
    void 근거목록을불변으로보존한다() {
        List<String> evidence =
                new ArrayList<>(List.of("검색 비중: 12.4%"));

        InsightResult result = new InsightResult(
                InsightSentiment.POSITIVE,
                "히어로 검색 비중 요약",
                "히어로 검색 비중에서 긍정적인 흐름이 확인되었습니다.",
                evidence
        );

        evidence.clear();

        assertThat(result.evidence())
                .containsExactly("검색 비중: 12.4%");
        assertThatThrownBy(
                        () -> result.evidence().add(
                                "변화율: +0.8%p"
                        )
                ).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void 감정이비어있으면생성할수없다() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> new InsightResult(
                                null,
                                "히어로 검색 비중 요약",
                                "검증된 요약",
                                List.of("검색 비중: 12.4%")
                        )
                );
    }

    @ParameterizedTest
    @MethodSource("invalidTextValues")
    void 제목이비어있으면생성할수없다(String headline) {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> new InsightResult(
                                InsightSentiment.NEUTRAL,
                                headline,
                                "검증된 요약",
                                List.of("검색 비중: 12.4%")
                        )
                );
    }

    @ParameterizedTest
    @MethodSource("invalidTextValues")
    void 요약이비어있으면생성할수없다(String summary) {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> new InsightResult(
                                InsightSentiment.NEUTRAL,
                                "히어로 검색 비중 요약",
                                summary,
                                List.of("검색 비중: 12.4%")
                        )
                );
    }

    @Test
    void 근거가비어있으면생성할수없다() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> new InsightResult(
                                InsightSentiment.NEUTRAL,
                                "히어로 검색 비중 요약",
                                "검증된 요약",
                                List.of()
                        )
                );
    }

    @Test
    void 근거목록이null이면생성할수없다() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> new InsightResult(
                                InsightSentiment.NEUTRAL,
                                "히어로 검색 비중 요약",
                                "검증된 요약",
                                null
                        )
                );
    }

    private static Stream<String> invalidTextValues() {
        return Stream.of(null, "", " ");
    }
}
