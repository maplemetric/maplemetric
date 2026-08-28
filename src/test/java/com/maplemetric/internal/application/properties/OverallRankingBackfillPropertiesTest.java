package com.maplemetric.internal.application.properties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Backfill 설정이 서로 어긋난 채로 뜨지 않는지 확인한다.
 *
 * 잘못된 조합은 뜬 뒤에 알면 늦다. 값을 받는 자리에서 막는다.
 */
class OverallRankingBackfillPropertiesTest {

    private static final int MAX_PAGES = 10;

    private static final int MAX_ATTEMPTS = 3;

    private static final int MAX_DATES_PER_RUN = 7;

    /**
     * 회수 임계가 호출 간격보다 짧으면 시작하지 못한다.
     *
     * 정상 실행이 다음 호출을 기다리는 동안 자기 점유가 회수돼 다른 실행기에
     * 넘어간다. 같은 기준일을 둘이 수집하고 외부 호출을 헛되이 쓴다.
     */
    @Test
    void 회수임계가호출간격보다짧으면시작하지못한다() {
        assertThatThrownBy(() -> propertiesOf(
                Duration.ofMinutes(10),
                Duration.ofMinutes(5)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("호출 간격보다 길어야");
    }

    /** 같아도 막는다. 기다리는 사이 경계에 걸리면 결과가 갈린다. */
    @Test
    void 회수임계가호출간격과같아도시작하지못한다() {
        assertThatThrownBy(() -> propertiesOf(
                Duration.ofMinutes(5),
                Duration.ofMinutes(5)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("호출 간격보다 길어야");
    }

    @Test
    void 회수임계가호출간격보다길면받아들인다() {
        assertThatCode(() -> propertiesOf(
                Duration.ofSeconds(2),
                Duration.ofMinutes(10)
        ))
                .doesNotThrowAnyException();
    }

    private OverallRankingBackfillProperties propertiesOf(
            Duration requestInterval,
            Duration staleClaimTimeout
    ) {
        return new OverallRankingBackfillProperties(
                MAX_PAGES,
                MAX_ATTEMPTS,
                MAX_DATES_PER_RUN,
                requestInterval,
                staleClaimTimeout
        );
    }
}
