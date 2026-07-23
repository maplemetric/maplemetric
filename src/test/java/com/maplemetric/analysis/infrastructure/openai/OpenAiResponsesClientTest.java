package com.maplemetric.analysis.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.maplemetric.analysis.domain.model.InsightFacts;
import com.maplemetric.analysis.domain.model.InsightSentiment;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiResponsesClientTest {

    private static final String BASE_URL = "https://api.openai.test";
    private static final String API_KEY = "test-openai-api-key";
    private static final String MODEL = "test-model";

    private MockRestServiceServer mockServer;
    private OpenAiResponsesClient responsesClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeaders(
                        headers -> headers.setBearerAuth(API_KEY)
                );

        mockServer = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();

        RestClient restClient = restClientBuilder.build();

        responsesClient = new OpenAiResponsesClient(restClient);
    }

    @Test
    void ResponsesAPI에엄격한JSON스키마로요청한다() {
        String responseBody = """
                {
                  "status": "completed",
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "{}"
                        }
                      ]
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + API_KEY
                        )
                )
                .andExpect(jsonPath("$.model").value(MODEL))
                .andExpect(
                        jsonPath("$.input")
                                .value(containsString(
                                        "subject: 히어로 검색 비중"
                                ))
                )
                .andExpect(
                        jsonPath("$.input")
                                .value(containsString(
                                        "sentiment: POSITIVE"
                                ))
                )
                .andExpect(
                        jsonPath("$.input")
                                .value(containsString(
                                        "검색 비중: 증가"
                                ))
                )
                .andExpect(
                        jsonPath("$.text.format.type")
                                .value("json_schema")
                )
                .andExpect(
                        jsonPath("$.text.format.name")
                                .value("maplemetric_insight")
                )
                .andExpect(
                        jsonPath("$.text.format.strict")
                                .value(true)
                )
                .andExpect(
                        jsonPath(
                                "$.text.format.schema"
                                        + ".additionalProperties"
                        ).value(false)
                )
                .andExpect(
                        jsonPath(
                                "$.text.format.schema.required.length()"
                        ).value(4)
                )
                .andExpect(
                        jsonPath(
                                "$.text.format.schema.properties"
                                        + ".sentiment.enum[0]"
                        ).value("POSITIVE")
                )
                .andRespond(
                        withSuccess(
                                responseBody,
                                MediaType.APPLICATION_JSON
                        )
                );

        OpenAiResponsesResponse response =
                responsesClient.create(
                        OpenAiResponsesRequest.from(
                                createFacts(),
                                MODEL
                        )
                );

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.output()).hasSize(1);
        assertThat(response.output().get(0).content().get(0).text())
                .isEqualTo("{}");

        mockServer.verify();
    }

    private InsightFacts createFacts() {
        return new InsightFacts(
                "히어로 검색 비중",
                InsightSentiment.POSITIVE,
                List.of(
                        new InsightFacts.Evidence(
                                "검색 비중",
                                "증가"
                        )
                )
        );
    }
}
