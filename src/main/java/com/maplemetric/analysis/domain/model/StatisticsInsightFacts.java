package com.maplemetric.analysis.domain.model;

import com.maplemetric.statistics.api.StatisticsSubjectType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 한 대상에 대해 설명을 만들 재료다.
 *
 * {@code facts}는 종류당 하나이며 우선순위 순으로 정렬돼 있다. 같은 입력이면 언제나
 * 같은 목록과 같은 순서가 나온다.
 *
 * {@code limitations}는 이 수치로 말할 수 없는 것을 적는다. 표본이 잘렸거나 기간에
 * 빈 날이 있으면 여기 남고, 이것을 잃으면 표본을 전체 사용자로 표현하게 된다.
 */
public record StatisticsInsightFacts(
        Subject subject,
        Period period,
        List<StatisticsInsightFact> facts,
        Source source,
        List<String> limitations
) {

    public StatisticsInsightFacts {
        if (subject == null) {
            throw new IllegalArgumentException(
                    "통계 인사이트 대상은 비어 있을 수 없습니다."
            );
        }

        facts = normalize(facts);
        limitations = limitations == null
                ? List.of()
                : List.copyOf(limitations);
    }

    /**
     * 종류당 하나만 남기고 우선순위로 정렬한다.
     *
     * 규칙이 늘어 같은 종류를 두 번 만들어도 앞선 것만 남는다. 정렬을 종류에만
     * 의존시켜 입력 순서가 달라도 결과가 흔들리지 않게 한다.
     */
    private static List<StatisticsInsightFact> normalize(
            List<StatisticsInsightFact> facts
    ) {
        if (facts == null) {
            return List.of();
        }

        Map<StatisticsInsightFactType, StatisticsInsightFact> byType =
                new LinkedHashMap<>();

        facts.stream()
                .filter(fact -> fact != null)
                .forEach(fact -> byType.putIfAbsent(fact.type(), fact));

        return byType.values()
                .stream()
                .sorted(Comparator.comparing(fact -> fact.type()))
                .toList();
    }

    public record Subject(
            StatisticsSubjectType type,
            String slug,
            String name
    ) {
    }

    /**
     * 수치가 가리키는 기간이다.
     *
     * {@code missingDateCount}는 요청 기간에서 수집이 없던 날 수다. 이 값 없이
     * "꾸준히 늘었다"를 말하면 빈 날까지 추세로 만들게 된다.
     */
    public record Period(
            String preset,
            LocalDate from,
            LocalDate to,
            int pointCount,
            int missingDateCount
    ) {
    }

    /**
     * 수치의 출처다.
     *
     * {@code truncated}가 참이면 표본이 잘린 것이라 전체 모집단이 아니다.
     */
    public record Source(
            String source,
            Instant collectedAt,
            Integer sampleSize,
            boolean truncated
    ) {
    }
}
