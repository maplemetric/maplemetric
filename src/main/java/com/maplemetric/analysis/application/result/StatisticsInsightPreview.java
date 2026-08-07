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
        List<String> lines,
        String model
) {

    /**
     * 템플릿이 만든 문장임을 나타내는 값이다.
     *
     * 외부 호출이 실패하면 템플릿이 대신 문장을 만드는데, 그 사실이 어디에도
     * 남지 않으면 연동이 죽어도 정상 생성과 구분되지 않는다. 실제로 API Key가
     * 만료된 채 62건이 생성됐고 로그를 뒤지기 전까지 알 수 없었다.
     */
    public static final String TEMPLATE_MODEL = "TEMPLATE";

    public StatisticsInsightPreview {
        if (headline == null || headline.isBlank()) {
            throw new IllegalArgumentException(
                    "통계 인사이트 제목은 비어 있을 수 없습니다."
            );
        }

        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException(
                    "통계 인사이트 생성 출처는 비어 있을 수 없습니다."
            );
        }

        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
