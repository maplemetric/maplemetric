package com.maplemetric.analysis.application.result;

import com.maplemetric.analysis.domain.model.InsightSentiment;
import java.util.List;

public record InsightResult(
        InsightSentiment sentiment,
        String headline,
        String summary,
        List<String> evidence
) {

    public InsightResult {
        if (sentiment == null) {
            throw new IllegalArgumentException(
                    "인사이트 감정은 비어 있을 수 없습니다."
            );
        }

        requireText(
                headline,
                "인사이트 제목은 비어 있을 수 없습니다."
        );
        requireText(
                summary,
                "인사이트 요약은 비어 있을 수 없습니다."
        );

        if (evidence == null || evidence.isEmpty()) {
            throw new IllegalArgumentException(
                    "인사이트 근거는 한 개 이상이어야 합니다."
            );
        }

        if (evidence.stream()
                .anyMatch(item -> item == null || item.isBlank())) {
            throw new IllegalArgumentException(
                    "인사이트 근거는 비어 있을 수 없습니다."
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
}
