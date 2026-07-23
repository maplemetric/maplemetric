package com.maplemetric.analysis;

import java.util.List;

public record InsightFacts(
        String subject,
        InsightSentiment sentiment,
        List<Evidence> evidence
) {

    public InsightFacts {
        requireText(
                subject,
                "인사이트 대상은 비어 있을 수 없습니다."
        );

        if (sentiment == null) {
            throw new IllegalArgumentException(
                    "인사이트 감정은 비어 있을 수 없습니다."
            );
        }

        if (evidence == null || evidence.isEmpty()) {
            throw new IllegalArgumentException(
                    "인사이트 근거는 한 개 이상이어야 합니다."
            );
        }

        if (evidence.stream().anyMatch(item -> item == null)) {
            throw new IllegalArgumentException(
                    "인사이트 근거는 null일 수 없습니다."
            );
        }

        evidence = List.copyOf(evidence);
    }

    private static void requireText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public record Evidence(
            String label,
            String value
    ) {

        public Evidence {
            requireText(
                    label,
                    "인사이트 근거 이름은 비어 있을 수 없습니다."
            );
            requireText(
                    value,
                    "인사이트 근거 값은 비어 있을 수 없습니다."
            );
        }
    }
}
