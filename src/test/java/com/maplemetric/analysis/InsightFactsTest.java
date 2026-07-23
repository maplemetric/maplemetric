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

class InsightFactsTest {

    @Test
    void 한개의근거로Fact를생성한다() {
        InsightFacts facts = new InsightFacts(
                "히어로 검색 비중",
                InsightSentiment.POSITIVE,
                List.of(
                        new InsightFacts.Evidence(
                                "검색 비중",
                                "11.6% → 12.4% (+0.8%p)"
                        )
                )
        );

        assertThat(facts.evidence()).hasSize(1);
    }

    @Test
    void 근거목록을불변으로보존한다() {
        List<InsightFacts.Evidence> evidence =
                new ArrayList<>(
                        List.of(
                                new InsightFacts.Evidence(
                                        "검색 비중",
                                        "12.4%"
                                )
                        )
                );

        InsightFacts facts = new InsightFacts(
                "히어로 검색 비중",
                InsightSentiment.POSITIVE,
                evidence
        );

        evidence.clear();

        assertThat(facts.evidence()).hasSize(1);
        assertThatThrownBy(
                        () -> facts.evidence().add(
                                new InsightFacts.Evidence(
                                        "변화율",
                                        "+0.8%p"
                                )
                        )
                ).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @MethodSource("invalidTextValues")
    void 대상이비어있으면생성할수없다(String subject) {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> new InsightFacts(
                                subject,
                                InsightSentiment.NEUTRAL,
                                createEvidence()
                        )
                );
    }

    @Test
    void 감정이비어있으면생성할수없다() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> new InsightFacts(
                                "히어로 검색 비중",
                                null,
                                createEvidence()
                        )
                );
    }

    @Test
    void 근거목록이비어있으면생성할수없다() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> new InsightFacts(
                                "히어로 검색 비중",
                                InsightSentiment.NEUTRAL,
                                List.of()
                        )
                );
    }

    @Test
    void 근거목록이null이면생성할수없다() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> new InsightFacts(
                                "히어로 검색 비중",
                                InsightSentiment.NEUTRAL,
                                null
                        )
                );
    }

    @Test
    void null근거가포함되면생성할수없다() {
        List<InsightFacts.Evidence> evidence =
                new ArrayList<>();
        evidence.add(null);

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> new InsightFacts(
                                "히어로 검색 비중",
                                InsightSentiment.NEUTRAL,
                                evidence
                        )
                );
    }

    @ParameterizedTest
    @MethodSource("invalidTextValues")
    void 근거이름이비어있으면생성할수없다(String label) {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> new InsightFacts.Evidence(
                                label,
                                "12.4%"
                        )
                );
    }

    @ParameterizedTest
    @MethodSource("invalidTextValues")
    void 근거값이비어있으면생성할수없다(String value) {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> new InsightFacts.Evidence(
                                "검색 비중",
                                value
                        )
                );
    }

    private List<InsightFacts.Evidence> createEvidence() {
        return List.of(
                new InsightFacts.Evidence(
                        "검색 비중",
                        "12.4%"
                )
        );
    }

    private static Stream<String> invalidTextValues() {
        return Stream.of(null, "", " ");
    }
}
