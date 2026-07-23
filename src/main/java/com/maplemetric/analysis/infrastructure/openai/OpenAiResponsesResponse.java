package com.maplemetric.analysis.infrastructure.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiResponsesResponse(
        String status,
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
