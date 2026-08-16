package com.maplemetric.ranking.infrastructure.client.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.common.nexon.NexonRateLimitProperties;
import com.maplemetric.common.nexon.NexonRequestRateGate;
import com.maplemetric.common.nexon.NexonApiFailure;
import com.maplemetric.ranking.domain.exception.RankingException;
import com.maplemetric.ranking.infrastructure.client.nexon.response.DojangRankingResponse;
import com.maplemetric.ranking.infrastructure.client.nexon.response.OverallRankingResponse;
import com.maplemetric.ranking.infrastructure.client.nexon.response.UnionRankingResponse;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

@ExtendWith(OutputCaptureExtension.class)
class RankingClientImplTest {

    private static final String BASE_URL =
            "https://open.api.nexon.com";

    private static final String NEXON_API_KEY =
            "test-nexon-api-key";

    private static final String OCID =
            "1234567890abcdefghijklmnopqrstuv";

    private static final String MASKED_OCID =
            "1234...stuv";

    private static final LocalDate RANKING_DATE =
            LocalDate.of(2026, 7, 19);

    private static final String OVERALL_RANKING_PATH =
            "/maplestory/v1/ranking/overall";

    private static final String UNION_RANKING_PATH =
            "/maplestory/v1/ranking/union";

    private static final String DOJANG_RANKING_PATH =
            "/maplestory/v1/ranking/dojang";

    private ObjectMapper objectMapper;
    private MockRestServiceServer mockServer;
    private RankingClientImpl rankingClient;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        RestClient.Builder restClientBuilder =
                RestClient.builder()
                        .baseUrl(BASE_URL)
                        .defaultHeader(
                                "x-nxopen-api-key",
                                NEXON_API_KEY
                        );

        mockServer = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();

        rankingClient = new RankingClientImpl(
                restClientBuilder.build(),
                objectMapper,
                new NexonRequestRateGate(new NexonRateLimitProperties(1000, java.time.Duration.ofMinutes(1)))
        );
    }

    @Test
    void 종합랭킹을공식쿼리로조회하고역직렬화한다() {
        expectGetRequest(
                OVERALL_RANKING_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "world_type", "1",
                        "class", "팬텀-전체 전직",
                        "page", "2"
                ),
                withSuccess(
                        """
                        {
                          "ranking": [
                            {
                              "date": "2026-07-19",
                              "ranking": 1,
                              "character_name": "감점",
                              "world_name": "루나",
                              "class_name": "팬텀",
                              "sub_class_name": "",
                              "character_level": 290,
                              "character_exp": 1234567890123,
                              "character_popularity": 321,
                              "character_guildname": "메이플"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        OverallRankingResponse result =
                rankingClient.getOverallRanking(
                        RANKING_DATE,
                        null,
                        1,
                        "팬텀-전체 전직",
                        2
                );

        assertThat(result.ranking())
                .singleElement()
                .satisfies(ranking -> {
                    assertThat(ranking.ranking()).isEqualTo(1);
                    assertThat(ranking.characterName())
                            .isEqualTo("감점");
                    assertThat(ranking.characterExp())
                            .isEqualTo(1_234_567_890_123L);
                    assertThat(ranking.characterGuildName())
                            .isEqualTo("메이플");
                });

        mockServer.verify();
    }

    @Test
    void 캐릭터랭킹네종을공식쿼리로조회하고역직렬화한다() {
        ResponseCreator responseCreator = characterRankingResponse();

        expectGetRequest(
                OVERALL_RANKING_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "ocid", OCID
                ),
                responseCreator
        );

        expectGetRequest(
                OVERALL_RANKING_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "world_name", "루나",
                        "ocid", OCID
                ),
                responseCreator
        );

        expectGetRequest(
                OVERALL_RANKING_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "class", "팬텀-전체 전직",
                        "ocid", OCID
                ),
                responseCreator
        );

        expectGetRequest(
                OVERALL_RANKING_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "world_name", "루나",
                        "class", "팬텀-전체 전직",
                        "ocid", OCID
                ),
                responseCreator
        );

        OverallRankingResponse overallResult =
                rankingClient.getCharacterOverallRanking(
                        OCID,
                        RANKING_DATE
                );

        OverallRankingResponse worldResult =
                rankingClient.getCharacterWorldRanking(
                        OCID,
                        "루나",
                        RANKING_DATE
                );

        OverallRankingResponse classResult =
                rankingClient.getCharacterClassRanking(
                        OCID,
                        "팬텀-전체 전직",
                        RANKING_DATE
                );

        OverallRankingResponse worldClassResult =
                rankingClient.getCharacterWorldClassRanking(
                        OCID,
                        "루나",
                        "팬텀-전체 전직",
                        RANKING_DATE
                );

        assertThat(overallResult.ranking().get(0).characterName())
                .isEqualTo("감점");

        assertThat(worldResult.ranking().get(0).ranking())
                .isEqualTo(58333);

        assertThat(classResult.ranking().get(0).className())
                .isEqualTo("팬텀");

        assertThat(worldClassResult.ranking().get(0).subClassName())
                .isNull();

        mockServer.verify();
    }

    @Test
    void 캐릭터직업랭킹사엑스엑스로그는OCID와API키를노출하지않는다(
            CapturedOutput output
    ) {
        expectGetRequest(
                OVERALL_RANKING_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "class", "팬텀-전체 전직",
                        "ocid", OCID
                ),
                withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                """
                                {
                                  "error": {
                                    "name": "OPENAPI00004",
                                    "message": "Please input valid parameter"
                                  }
                                }
                                """
                        )
        );

        RankingException exception = catchThrowableOfType(
                () -> rankingClient.getCharacterClassRanking(
                        OCID,
                        "팬텀-전체 전직",
                        RANKING_DATE
                ),
                RankingException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(NexonApiFailure.CLIENT_ERROR);

        assertThat(output)
                .contains("캐릭터 직업 랭킹 정보")
                .contains("OPENAPI00004")
                .contains("ocid=" + MASKED_OCID)
                .doesNotContain(OCID)
                .doesNotContain(NEXON_API_KEY)
                .doesNotContain("x-nxopen-api-key");

        mockServer.verify();
    }

    @Test
    void 캐릭터랭킹응답본문이비어있으면잘못된응답오류를반환한다(
            CapturedOutput output
    ) {
        expectGetRequest(
                OVERALL_RANKING_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "ocid", OCID
                ),
                withStatus(HttpStatus.OK)
        );

        RankingException exception = catchThrowableOfType(
                () -> rankingClient.getCharacterOverallRanking(
                        OCID,
                        RANKING_DATE
                ),
                RankingException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(NexonApiFailure.RESPONSE_INVALID);

        assertSensitiveValuesAreNotLogged(output);

        mockServer.verify();
    }

    @Test
    void 캐릭터랭킹사공사응답은빈목록으로변환하지않는다(
            CapturedOutput output
    ) {
        expectGetRequest(
                OVERALL_RANKING_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "ocid", OCID
                ),
                withStatus(HttpStatus.NOT_FOUND)
        );

        RankingException exception = catchThrowableOfType(
                () -> rankingClient.getCharacterOverallRanking(
                        OCID,
                        RANKING_DATE
                ),
                RankingException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(NexonApiFailure.NOT_FOUND);

        assertSensitiveValuesAreNotLogged(output);

        mockServer.verify();
    }

    @Test
    void 캐릭터랭킹요청제한응답이면재시도후성공한다(
            CapturedOutput output
    ) {
        Map<String, String> queryParameters = Map.of(
                "date", "2026-07-19",
                "class", "팬텀-전체 전직",
                "ocid", OCID
        );

        expectGetRequest(
                OVERALL_RANKING_PATH,
                queryParameters,
                createRateLimitResponse()
        );

        expectGetRequest(
                OVERALL_RANKING_PATH,
                queryParameters,
                characterRankingResponse()
        );

        OverallRankingResponse result =
                rankingClient.getCharacterClassRanking(
                        OCID,
                        "팬텀-전체 전직",
                        RANKING_DATE
                );

        assertThat(result.ranking().get(0).ranking())
                .isEqualTo(58333);

        assertThat(output)
                .contains("OPENAPI00007")
                .contains("retryCount=1");

        assertSensitiveValuesAreNotLogged(output);

        mockServer.verify();
    }

    @Test
    void 캐릭터랭킹요청제한재시도횟수를소진하면클라이언트오류다(
            CapturedOutput output
    ) {
        Map<String, String> queryParameters = Map.of(
                "date", "2026-07-19",
                "class", "팬텀-전체 전직",
                "ocid", OCID
        );

        for (int requestCount = 0;
             requestCount < 4;
             requestCount++) {
            expectGetRequest(
                    OVERALL_RANKING_PATH,
                    queryParameters,
                    createRateLimitResponse()
            );
        }

        RankingException exception = catchThrowableOfType(
                () -> rankingClient.getCharacterClassRanking(
                        OCID,
                        "팬텀-전체 전직",
                        RANKING_DATE
                ),
                RankingException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(NexonApiFailure.CLIENT_ERROR);

        assertThat(output)
                .contains("retryCount=1")
                .contains("retryCount=2")
                .contains("retryCount=3");

        assertSensitiveValuesAreNotLogged(output);

        mockServer.verify();
    }

    @Test
    void 캐릭터랭킹타임아웃은타임아웃오류로변환한다(
            CapturedOutput output
    ) {
        RankingClientImpl timeoutClient = createFailingClient(
                new SocketTimeoutException("read timeout")
        );

        RankingException exception = catchThrowableOfType(
                () -> timeoutClient.getCharacterOverallRanking(
                        OCID,
                        RANKING_DATE
                ),
                RankingException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(NexonApiFailure.TIMEOUT);

        assertSensitiveValuesAreNotLogged(output);
    }

    @Test
    void 월드명이있으면종합랭킹의월드타입을전송하지않는다() {
        expectGetRequest(
                OVERALL_RANKING_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "world_name", "루나",
                        "page", "1"
                ),
                emptyRankingResponse()
        );

        rankingClient.getOverallRanking(
                RANKING_DATE,
                "루나",
                1,
                null,
                1
        );

        mockServer.verify();
    }

    @Test
    void 유니온랭킹을공식쿼리로조회하고역직렬화한다() {
        expectGetRequest(
                UNION_RANKING_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "world_name", "루나",
                        "page", "3"
                ),
                withSuccess(
                        """
                        {
                          "ranking": [
                            {
                              "date": "2026-07-19",
                              "ranking": 10,
                              "character_name": "감점",
                              "world_name": "루나",
                              "class_name": "팬텀",
                              "sub_class_name": "",
                              "union_level": 9000,
                              "union_power": 123456789012
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        UnionRankingResponse result =
                rankingClient.getUnionRanking(
                        RANKING_DATE,
                        "루나",
                        3
                );

        assertThat(result.ranking())
                .singleElement()
                .satisfies(ranking -> {
                    assertThat(ranking.unionLevel())
                            .isEqualTo(9000);
                    assertThat(ranking.unionPower())
                            .isEqualTo(123_456_789_012L);
                });

        mockServer.verify();
    }

    @Test
    void 무릉도장랭킹을공식쿼리로조회하고역직렬화한다() {
        expectGetRequest(
                DOJANG_RANKING_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "world_name", "루나",
                        "difficulty", "1",
                        "class", "팬텀-전체 전직",
                        "page", "4"
                ),
                withSuccess(
                        """
                        {
                          "ranking": [
                            {
                              "date": "2026-07-19",
                              "ranking": 20,
                              "character_name": "감점",
                              "world_name": "루나",
                              "class_name": "팬텀",
                              "sub_class_name": "",
                              "character_level": 290,
                              "dojang_floor": 80,
                              "dojang_time_record": 600
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        DojangRankingResponse result =
                rankingClient.getDojangRanking(
                        RANKING_DATE,
                        "루나",
                        1,
                        "팬텀-전체 전직",
                        4
                );

        assertThat(result.ranking())
                .singleElement()
                .satisfies(ranking -> {
                    assertThat(ranking.dojangFloor())
                            .isEqualTo(80);
                    assertThat(ranking.dojangTimeRecord())
                            .isEqualTo(600);
                });

        mockServer.verify();
    }

    @Test
    void 이백응답의빈랭킹목록은그대로반환한다() {
        expectGetRequest(
                UNION_RANKING_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "page", "1"
                ),
                emptyRankingResponse()
        );

        UnionRankingResponse result =
                rankingClient.getUnionRanking(
                        RANKING_DATE,
                        null,
                        1
                );

        assertThat(result.ranking()).isEmpty();

        mockServer.verify();
    }

    @Test
    void 랭킹목록이누락되면잘못된응답오류를반환한다() {
        expectGetRequest(
                UNION_RANKING_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "page", "1"
                ),
                withSuccess(
                        "{}",
                        MediaType.APPLICATION_JSON
                )
        );

        RankingException exception = catchThrowableOfType(
                () -> rankingClient.getUnionRanking(
                        RANKING_DATE,
                        null,
                        1
                ),
                RankingException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(NexonApiFailure.RESPONSE_INVALID);

        mockServer.verify();
    }

    @Test
    void 사공사응답을빈랭킹으로변환하지않는다() {
        expectGetRequest(
                OVERALL_RANKING_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "page", "1"
                ),
                withStatus(HttpStatus.NOT_FOUND)
        );

        RankingException exception = catchThrowableOfType(
                () -> rankingClient.getOverallRanking(
                        RANKING_DATE,
                        null,
                        null,
                        null,
                        1
                ),
                RankingException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(NexonApiFailure.NOT_FOUND);

        mockServer.verify();
    }

    @Test
    void 일반사엑스엑스응답은클라이언트오류로변환한다(
            CapturedOutput output
    ) {
        expectGetRequest(
                DOJANG_RANKING_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "difficulty", "0",
                        "page", "1"
                ),
                withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                """
                                {
                                  "error": {
                                    "name": "OPENAPI00004",
                                    "message": "Please input valid parameter"
                                  }
                                }
                                """
                        )
        );

        RankingException exception = catchThrowableOfType(
                () -> rankingClient.getDojangRanking(
                        RANKING_DATE,
                        null,
                        0,
                        null,
                        1
                ),
                RankingException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(NexonApiFailure.CLIENT_ERROR);

        assertThat(output)
                .contains("무릉도장 랭킹 목록")
                .contains("OPENAPI00004")
                .doesNotContain(NEXON_API_KEY)
                .doesNotContain("x-nxopen-api-key");

        mockServer.verify();
    }

    @Test
    void 오엑스엑스응답은서버오류로변환한다() {
        expectGetRequest(
                UNION_RANKING_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "page", "1"
                ),
                withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        );

        RankingException exception = catchThrowableOfType(
                () -> rankingClient.getUnionRanking(
                        RANKING_DATE,
                        null,
                        1
                ),
                RankingException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(NexonApiFailure.SERVER_ERROR);

        mockServer.verify();
    }

    @Test
    void 타임아웃은타임아웃오류로변환한다() {
        RankingClientImpl timeoutClient = createFailingClient(
                new SocketTimeoutException("read timeout")
        );

        RankingException exception = catchThrowableOfType(
                () -> timeoutClient.getOverallRanking(
                        RANKING_DATE,
                        null,
                        null,
                        null,
                        1
                ),
                RankingException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(NexonApiFailure.TIMEOUT);
    }

    private ResponseCreator emptyRankingResponse() {
        return withSuccess(
                "{\"ranking\": []}",
                MediaType.APPLICATION_JSON
        );
    }

    private ResponseCreator characterRankingResponse() {
        return withSuccess(
                """
                {
                  "ranking": [
                    {
                      "date": "2026-07-19",
                      "ranking": 58333,
                      "character_name": "감점",
                      "world_name": "루나",
                      "class_name": "팬텀",
                      "sub_class_name": null,
                      "character_level": 290,
                      "character_exp": 123456789,
                      "character_popularity": 321,
                      "character_guildname": "메이플"
                    }
                  ]
                }
                """,
                MediaType.APPLICATION_JSON
        );
    }

    private ResponseCreator createRateLimitResponse() {
        return withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        """
                        {
                          "error": {
                            "name": "OPENAPI00007",
                            "message": "Please try again later"
                          }
                        }
                        """
                );
    }

    private void assertSensitiveValuesAreNotLogged(
            CapturedOutput output
    ) {
        assertThat(output)
                .contains("ocid=" + MASKED_OCID)
                .doesNotContain(OCID)
                .doesNotContain(NEXON_API_KEY)
                .doesNotContain("x-nxopen-api-key");
    }

    private void expectGetRequest(
            String path,
            Map<String, String> queryParameters,
            ResponseCreator responseCreator
    ) {
        mockServer.expect(request -> {
                    assertThat(request.getMethod())
                            .isEqualTo(HttpMethod.GET);

                    assertThat(request.getURI().getPath())
                            .isEqualTo(path);

                    String query = URLDecoder.decode(
                            request.getURI().getRawQuery(),
                            StandardCharsets.UTF_8
                    );

                    assertThat(query.split("&"))
                            .containsExactlyInAnyOrderElementsOf(
                                    queryParameters.entrySet()
                                            .stream()
                                            .map(entry -> entry.getKey()
                                                    + "="
                                                    + entry.getValue()
                                            )
                                            .toList()
                            );
                })
                .andRespond(responseCreator);
    }

    private RankingClientImpl createFailingClient(
            IOException exception
    ) {
        ClientHttpRequestFactory requestFactory =
                (uri, httpMethod) -> {
                    throw exception;
                };

        RestClient restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader(
                        "x-nxopen-api-key",
                        NEXON_API_KEY
                )
                .requestFactory(requestFactory)
                .build();

        return new RankingClientImpl(
                restClient,
                objectMapper,
                new NexonRequestRateGate(new NexonRateLimitProperties(1000, java.time.Duration.ofMinutes(1)))
        );
    }
}
