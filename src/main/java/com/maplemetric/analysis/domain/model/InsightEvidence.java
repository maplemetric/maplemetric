package com.maplemetric.analysis.domain.model;

/**
 * 문장이 인용할 수 있는 근거 한 줄이다.
 *
 * 감성 기반 {@link InsightFacts.Evidence}와 모양은 같지만 따로 둔다. 두 도메인을
 * 한 타입으로 묶으면 한쪽 요구가 바뀔 때 다른 쪽이 함께 흔들린다.
 */
public record InsightEvidence(
        String label,
        String value
) {

    public InsightEvidence {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException(
                    "인사이트 근거 이름은 비어 있을 수 없습니다."
            );
        }

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "인사이트 근거 값은 비어 있을 수 없습니다."
            );
        }
    }
}
