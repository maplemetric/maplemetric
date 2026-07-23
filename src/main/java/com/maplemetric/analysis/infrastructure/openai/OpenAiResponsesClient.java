package com.maplemetric.analysis.infrastructure.openai;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class OpenAiResponsesClient {

    private static final String RESPONSES_PATH = "/v1/responses";

    private final RestClient restClient;

    OpenAiResponsesClient(
            @Qualifier("openAiRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    OpenAiResponsesResponse create(OpenAiResponsesRequest request) {
        return restClient.post()
                .uri(RESPONSES_PATH)
                .body(request)
                .retrieve()
                .body(OpenAiResponsesResponse.class);
    }
}
