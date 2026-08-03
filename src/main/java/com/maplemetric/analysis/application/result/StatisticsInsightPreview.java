package com.maplemetric.analysis.application.result;

import java.util.List;

/**
 * 통계 Fact를 사람이 눈으로 확인하는 형태다.
 *
 * {@link InsightResult}를 쓰지 않는다. 그쪽은 {@code InsightSentiment}를 반드시
 * 요구하는데, "비중이 늘었다"가 긍정인지 부정인지는 관점의 문제라 통계 수치만으로
 * 판정할 수 없다. 없는 감정을 지어내지 않으려고 반환 타입을 나눴다.
 */
public record StatisticsInsightPreview(
        String headline,
        List<String> lines
) {

    public StatisticsInsightPreview {
        if (headline == null || headline.isBlank()) {
            throw new IllegalArgumentException(
                    "통계 인사이트 제목은 비어 있을 수 없습니다."
            );
        }

        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
