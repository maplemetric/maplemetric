package com.maplemetric.analysis.infrastructure.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * {@code model}은 요청한 이름이 아니라 실제로 응답한 모델이다.
 *
 * 별칭은 스냅샷으로 해석되므로 요청값과 다를 수 있다. 저장할 때 요청값을 쓰면
 * 별칭이 새 스냅샷으로 옮겨가 문장이 달라져도 기록은 그대로여서, 나중에 변화의
 * 원인을 찾을 수 없다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiResponsesResponse(
        String status,
        String model,
        List<Output> output
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Output(
            String type,
            List<Content> content
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(
            String type,
            String text,
            String refusal
    ) {
    }
}
