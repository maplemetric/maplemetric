package com.maplemetric.character.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import com.maplemetric.character.infrastructure.client.dto.CharacterDojangResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterLinkSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterRankingResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSymbolResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterUnionResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterVMatrixResponse;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
class CharacterClientImplTest {

    private static final String BASE_URL =
            "https://open.api.nexon.com";

    private static final String CHARACTER_NAME =
            "test1234";

    private static final String OCID =
            "test-ocid";

    private static final String MASKED_OCID =
            "test...ocid";

    private static final String NEXON_API_KEY =
            "test-nexon-api-key";

    private static final String CHARACTER_OCID_PATH =
            "/maplestory/v1/id";

    private static final String CHARACTER_BASIC_PATH =
            "/maplestory/v1/character/basic";

    private static final String CHARACTER_UNION_PATH =
            "/maplestory/v1/user/union";

    private static final String CHARACTER_SYMBOL_PATH =
            "/maplestory/v1/character/symbol-equipment";

    private static final String CHARACTER_SKILL_PATH =
            "/maplestory/v1/character/skill";

    private static final String CHARACTER_LINK_SKILL_PATH =
            "/maplestory/v1/character/link-skill";

    private static final String CHARACTER_V_MATRIX_PATH =
            "/maplestory/v1/character/vmatrix";

    private static final String CHARACTER_HEXA_MATRIX_PATH =
            "/maplestory/v1/character/hexamatrix";

    private static final String CHARACTER_HEXA_MATRIX_STAT_PATH =
            "/maplestory/v1/character/hexamatrix-stat";

    private static final String RANKING_OVERALL_PATH =
            "/maplestory/v1/ranking/overall";

    private static final String CHARACTER_DOJANG_PATH =
            "/maplestory/v1/character/dojang";

    private static final LocalDate RANKING_DATE =
            LocalDate.of(2026, 7, 19);

    private ObjectMapper objectMapper;
    private MockRestServiceServer mockServer;
    private CharacterClientImpl characterClient;

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

        mockServer =
                MockRestServiceServer
                        .bindTo(restClientBuilder)
                        .build();

        characterClient = new CharacterClientImpl(
                restClientBuilder.build(),
                objectMapper
        );
    }

    @Test
    void OCID를조회한다() {
        expectGetRequest(
                CHARACTER_OCID_PATH,
                "character_name",
                CHARACTER_NAME,
                withSuccess(
                        """
                        {
                          "ocid": "test-ocid"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        String result =
                characterClient.getOcid(CHARACTER_NAME);

        assertThat(result).isEqualTo(OCID);

        mockServer.verify();
    }

    @Test
    void 존재하지않는캐릭터는미조회예외로변환한다() {
        expectGetRequest(
                CHARACTER_OCID_PATH,
                "character_name",
                CHARACTER_NAME,
                withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                """
                                {
                                  "error": {
                                    "name": "OPENAPI00004",
                                    "message": "파라미터가 유효하지 않습니다."
                                  }
                                }
                                """
                        )
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getOcid(
                                CHARACTER_NAME
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.CHARACTER_NOT_FOUND
                );

        mockServer.verify();
    }

    @Test
    void 유효하지않은식별자는미조회예외로변환한다() {
        expectGetRequest(
                CHARACTER_OCID_PATH,
                "character_name",
                CHARACTER_NAME,
                withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                """
                                {
                                  "error": {
                                    "name": "OPENAPI00003",
                                    "message": "유효하지 않은 식별자입니다."
                                  }
                                }
                                """
                        )
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getOcid(
                                CHARACTER_NAME
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.CHARACTER_NOT_FOUND
                );

        mockServer.verify();
    }

    @Test
    void 일반4xx응답은클라이언트오류로변환한다() {
        expectGetRequest(
                CHARACTER_OCID_PATH,
                "character_name",
                CHARACTER_NAME,
                withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                """
                                {
                                  "error": {
                                    "name": "OPENAPI00002",
                                    "message": "권한이 없습니다."
                                  }
                                }
                                """
                        )
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getOcid(
                                CHARACTER_NAME
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_CLIENT_ERROR
                );

        mockServer.verify();
    }

    @Test
    void 유니온정보를조회한다() {
        expectGetRequest(
                CHARACTER_UNION_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "union_level": 9000,
                          "union_artifact_level": 50
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterUnionResponse result =
                characterClient.getCharacterUnion(OCID);

        assertThat(result.unionLevel())
                .isEqualTo(9000);

        assertThat(result.unionArtifactLevel())
                .isEqualTo(50);

        mockServer.verify();
    }

    @Test
    void 장착심볼정보를조회한다() {
        expectGetRequest(
                CHARACTER_SYMBOL_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "date": null,
                          "character_class": "팬텀",
                          "symbol": [
                            {
                              "symbol_name": "아케인심볼 : 소멸의 여로",
                              "symbol_level": 20,
                              "symbol_icon": "arcane-icon"
                            },
                            {
                              "symbol_name": "그랜드 어센틱심볼 : 탈라하트",
                              "symbol_level": 5,
                              "symbol_icon": "authentic-icon"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterSymbolResponse result =
                characterClient.getCharacterSymbol(OCID);

        assertThat(result.characterClass())
                .isEqualTo("팬텀");

        assertThat(result.symbol())
                .hasSize(2);

        assertThat(result.symbol())
                .extracting(
                        symbol -> symbol.symbolName(),
                        symbol -> symbol.symbolLevel(),
                        symbol -> symbol.symbolIcon()
                )
                .containsExactly(
                        tuple(
                                "아케인심볼 : 소멸의 여로",
                                20,
                                "arcane-icon"
                        ),
                        tuple(
                                "그랜드 어센틱심볼 : 탈라하트",
                                5,
                                "authentic-icon"
                        )
                );

        mockServer.verify();
    }

    @Test
    void 캐릭터5차와6차스킬정보를스킬등급으로조회한다() {
        expectGetRequest(
                CHARACTER_SKILL_PATH,
                Map.of(
                        "ocid", OCID,
                        "character_skill_grade", "5"
                ),
                withSuccess(
                        createSkillResponseJson("5", "조커"),
                        MediaType.APPLICATION_JSON
                )
        );

        expectGetRequest(
                CHARACTER_SKILL_PATH,
                Map.of(
                        "ocid", OCID,
                        "character_skill_grade", "6"
                ),
                withSuccess(
                        createSkillResponseJson(
                                "6",
                                "템페스트 오브 카드 VI"
                        ),
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterSkillResponse fifthSkill =
                characterClient.getCharacterSkill(OCID, "5");

        CharacterSkillResponse sixthSkill =
                characterClient.getCharacterSkill(OCID, "6");

        assertThat(fifthSkill.characterSkillGrade())
                .isEqualTo("5");

        assertThat(fifthSkill.characterSkill().get(0).skillName())
                .isEqualTo("조커");

        assertThat(sixthSkill.characterSkillGrade())
                .isEqualTo("6");

        assertThat(sixthSkill.characterSkill().get(0).skillName())
                .isEqualTo("템페스트 오브 카드 VI");

        mockServer.verify();
    }

    @Test
    void 링크스킬프리셋숫자필드를역직렬화한다() {
        expectGetRequest(
                CHARACTER_LINK_SKILL_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "character_link_skill": [],
                          "character_link_skill_preset_1": [
                            {
                              "skill_name": "데들리 인스팅트",
                              "skill_level": 2,
                              "skill_icon": "link-icon"
                            }
                          ],
                          "character_link_skill_preset_2": [],
                          "character_link_skill_preset_3": []
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterLinkSkillResponse result =
                characterClient.getCharacterLinkSkill(OCID);

        assertThat(result.characterLinkSkillPreset1())
                .extracting(
                        skill -> skill.skillName(),
                        skill -> skill.skillLevel(),
                        skill -> skill.skillIcon()
                )
                .containsExactly(
                        tuple(
                                "데들리 인스팅트",
                                2,
                                "link-icon"
                        )
                );

        mockServer.verify();
    }

    @Test
    void V매트릭스정보를조회한다() {
        expectGetRequest(
                CHARACTER_V_MATRIX_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "character_class": "팬텀",
                          "character_v_core_equipment": [
                            {
                              "v_core_name": "조커",
                              "v_core_type": "직업 코어",
                              "v_core_level": 30
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterVMatrixResponse result =
                characterClient.getCharacterVMatrix(OCID);

        assertThat(result.characterVCoreEquipment())
                .extracting(
                        core -> core.vCoreName(),
                        core -> core.vCoreType(),
                        core -> core.vCoreLevel()
                )
                .containsExactly(
                        tuple("조커", "직업 코어", 30)
                );

        mockServer.verify();
    }

    @Test
    void HEXA코어정보를조회한다() {
        expectGetRequest(
                CHARACTER_HEXA_MATRIX_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "character_hexa_core_equipment": [
                            {
                              "hexa_core_name": "템페스트 오브 카드 VI",
                              "hexa_core_level": 18,
                              "hexa_core_type": "마스터리 코어",
                              "linked_skill": [
                                {
                                  "hexa_skill_id": "템페스트 오브 카드 VI"
                                }
                              ]
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterHexaMatrixResponse result =
                characterClient.getCharacterHexaMatrix(OCID);

        assertThat(result.characterHexaCoreEquipment().get(0).linkedSkill())
                .extracting(skill -> skill.hexaSkillId())
                .containsExactly("템페스트 오브 카드 VI");

        mockServer.verify();
    }

    @Test
    void HEXA스탯숫자필드를역직렬화한다() {
        expectGetRequest(
                CHARACTER_HEXA_MATRIX_STAT_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "character_hexa_stat_core": [],
                          "character_hexa_stat_core_2": [
                            {
                              "slot_id": "0",
                              "main_stat_name": "공격력 증가",
                              "sub_stat_name_1": "크리티컬 데미지 증가",
                              "sub_stat_name_2": "주력 스탯 증가",
                              "main_stat_level": 6,
                              "sub_stat_level_1": 6,
                              "sub_stat_level_2": 8
                            }
                          ],
                          "character_hexa_stat_core_3": []
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterHexaMatrixStatResponse result =
                characterClient.getCharacterHexaMatrixStat(OCID);

        assertThat(result.characterHexaStatCore2())
                .extracting(
                        stat -> stat.slotId(),
                        stat -> stat.subStatName1(),
                        stat -> stat.subStatLevel1()
                )
                .containsExactly(
                        tuple("0", "크리티컬 데미지 증가", 6)
                );

        mockServer.verify();
    }

    @Test
    void 종합랭킹정보를조회한다() {
        LinkedHashMap<String, String> queryParameters =
                new LinkedHashMap<>();

        queryParameters.put("date", "2026-07-19");
        queryParameters.put("ocid", OCID);

        expectGetRequest(
                RANKING_OVERALL_PATH,
                queryParameters,
                withSuccess(
                        """
                        {
                          "ranking": [
                            {
                              "ranking": 58333,
                              "character_name": "감점",
                              "world_name": "루나",
                              "class_name": "팬텀",
                              "sub_class_name": ""
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterRankingResponse result =
                characterClient.getOverallRanking(
                        OCID,
                        RANKING_DATE
                );

        assertThat(result.ranking())
                .hasSize(1);

        assertThat(result.ranking().get(0).ranking())
                .isEqualTo(58333);

        assertThat(result.ranking().get(0).characterName())
                .isEqualTo("감점");

        assertThat(result.ranking().get(0).subClassName())
                .isEmpty();

        mockServer.verify();
    }

    @Test
    void 월드랭킹정보는worldName파라미터로조회한다() {
        LinkedHashMap<String, String> queryParameters =
                new LinkedHashMap<>();

        queryParameters.put("date", "2026-07-19");
        queryParameters.put("world_name", "루나");
        queryParameters.put("ocid", OCID);

        expectGetRequest(
                RANKING_OVERALL_PATH,
                queryParameters,
                withSuccess(
                        """
                        {
                          "ranking": []
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterRankingResponse result =
                characterClient.getWorldRanking(
                        OCID,
                        "루나",
                        RANKING_DATE
                );

        assertThat(result.ranking())
                .isEmpty();

        mockServer.verify();
    }

    @Test
    void 직업랭킹정보는class파라미터로조회한다() {
        LinkedHashMap<String, String> queryParameters =
                new LinkedHashMap<>();

        queryParameters.put("date", "2026-07-19");
        queryParameters.put("class", "팬텀-전체 전직");
        queryParameters.put("ocid", OCID);

        expectGetRequest(
                RANKING_OVERALL_PATH,
                queryParameters,
                withSuccess(
                        """
                        {
                          "ranking": [
                            {
                              "ranking": 1588,
                              "character_name": "감점",
                              "world_name": "루나",
                              "class_name": "팬텀",
                              "sub_class_name": ""
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterRankingResponse result =
                characterClient.getClassRanking(
                        OCID,
                        "팬텀-전체 전직",
                        RANKING_DATE
                );

        assertThat(result.ranking().get(0).ranking())
                .isEqualTo(1588);

        assertThat(result.ranking().get(0).className())
                .isEqualTo("팬텀");

        assertThat(result.ranking().get(0).subClassName())
                .isEmpty();

        mockServer.verify();
    }

    @Test
    void 월드내직업랭킹정보는worldName과class파라미터로조회한다() {
        LinkedHashMap<String, String> queryParameters =
                new LinkedHashMap<>();

        queryParameters.put("date", "2026-07-19");
        queryParameters.put("world_name", "루나");
        queryParameters.put("class", "팬텀-전체 전직");
        queryParameters.put("ocid", OCID);

        expectGetRequest(
                RANKING_OVERALL_PATH,
                queryParameters,
                withSuccess(
                        """
                        {
                          "ranking": [
                            {
                              "ranking": 321,
                              "character_name": "감점",
                              "world_name": "루나",
                              "class_name": "팬텀",
                              "sub_class_name": ""
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterRankingResponse result =
                characterClient.getWorldClassRanking(
                        OCID,
                        "루나",
                        "팬텀-전체 전직",
                        RANKING_DATE
                );

        assertThat(result.ranking())
                .hasSize(1);

        assertThat(result.ranking().get(0).ranking())
                .isEqualTo(321);

        assertThat(result.ranking().get(0).characterName())
                .isEqualTo("감점");

        mockServer.verify();
    }

    @Test
    void 무릉최고기록정보는date없이ocid로조회한다() {
        expectGetRequest(
                CHARACTER_DOJANG_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "dojang_best_floor": 57
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterDojangResponse result =
                characterClient.getCharacterDojang(OCID);

        assertThat(result.dojangBestFloor())
                .isEqualTo(57);

        mockServer.verify();
    }

    @Test
    void 랭킹조회응답본문이비어있으면잘못된응답오류를반환한다(
            CapturedOutput output
    ) {
        expectGetRequest(
                RANKING_OVERALL_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "ocid", OCID
                ),
                withStatus(HttpStatus.OK)
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getOverallRanking(
                                OCID,
                                RANKING_DATE
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_RESPONSE_INVALID
                );

        assertSensitiveValuesAreNotLogged(output);

        mockServer.verify();
    }

    @Test
    void 랭킹조회404응답은빈목록으로변환하지않는다(
            CapturedOutput output
    ) {
        expectGetRequest(
                RANKING_OVERALL_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "ocid", OCID
                ),
                withStatus(HttpStatus.NOT_FOUND)
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getOverallRanking(
                                OCID,
                                RANKING_DATE
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.CHARACTER_NOT_FOUND
                );

        assertSensitiveValuesAreNotLogged(output);

        mockServer.verify();
    }

    @Test
    void 랭킹조회일반4xx응답은클라이언트오류로변환한다(
            CapturedOutput output
    ) {
        expectGetRequest(
                RANKING_OVERALL_PATH,
                Map.of(
                        "date", "2026-07-19",
                        "ocid", OCID
                ),
                withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                """
                                {
                                  "error": {
                                    "name": "OPENAPI00002",
                                    "message": "권한이 없습니다."
                                  }
                                }
                                """
                        )
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getOverallRanking(
                                OCID,
                                RANKING_DATE
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_CLIENT_ERROR
                );

        assertSensitiveValuesAreNotLogged(output);

        mockServer.verify();
    }

    @Test
    void 직업랭킹조회4xx로그에요청정보와넥슨오류내용을남긴다(
            CapturedOutput output
    ) {
        LinkedHashMap<String, String> queryParameters =
                new LinkedHashMap<>();

        queryParameters.put("date", "2026-07-19");
        queryParameters.put("class", "팬텀-전체 전직");
        queryParameters.put("ocid", OCID);

        expectGetRequest(
                RANKING_OVERALL_PATH,
                queryParameters,
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

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getClassRanking(
                                OCID,
                                "팬텀-전체 전직",
                                RANKING_DATE
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_CLIENT_ERROR
                );

        assertThat(output)
                .contains("캐릭터 직업 랭킹 정보")
                .contains("400 BAD_REQUEST")
                .contains("OPENAPI00004")
                .contains("Please input valid parameter")
                .contains("ocid=" + MASKED_OCID);

        assertSensitiveValuesAreNotLogged(output);

        mockServer.verify();
    }

    @Test
    void 요청제한응답이면재시도후성공응답을반환한다(
            CapturedOutput output
    ) {
        LinkedHashMap<String, String> queryParameters =
                new LinkedHashMap<>();

        queryParameters.put("date", "2026-07-19");
        queryParameters.put("class", "팬텀-전체 전직");
        queryParameters.put("ocid", OCID);

        expectGetRequest(
                RANKING_OVERALL_PATH,
                queryParameters,
                withStatus(HttpStatus.TOO_MANY_REQUESTS)
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
                        )
        );

        expectGetRequest(
                RANKING_OVERALL_PATH,
                queryParameters,
                withSuccess(
                        """
                        {
                          "ranking": [
                            {
                              "ranking": 1588,
                              "character_name": "감점",
                              "world_name": "루나",
                              "class_name": "팬텀",
                              "sub_class_name": ""
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterRankingResponse result =
                characterClient.getClassRanking(
                        OCID,
                        "팬텀-전체 전직",
                        RANKING_DATE
                );

        assertThat(result.ranking().get(0).ranking())
                .isEqualTo(1588);

        assertThat(output)
                .contains("OPENAPI00007")
                .contains("retryCount=1");

        assertSensitiveValuesAreNotLogged(output);

        mockServer.verify();
    }

    @Test
    void 요청제한재시도횟수를소진하면클라이언트오류로변환한다(
            CapturedOutput output
    ) {
        LinkedHashMap<String, String> queryParameters =
                new LinkedHashMap<>();

        queryParameters.put("date", "2026-07-19");
        queryParameters.put("class", "팬텀-전체 전직");
        queryParameters.put("ocid", OCID);

        for (int requestCount = 0;
             requestCount < 4;
             requestCount++) {
            expectGetRequest(
                    RANKING_OVERALL_PATH,
                    queryParameters,
                    createRateLimitResponse()
            );
        }

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getClassRanking(
                                OCID,
                                "팬텀-전체 전직",
                                RANKING_DATE
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_CLIENT_ERROR
                );

        assertThat(output)
                .contains("retryCount=1")
                .contains("retryCount=2")
                .contains("retryCount=3");

        assertSensitiveValuesAreNotLogged(output);

        mockServer.verify();
    }

    @Test
    void 랭킹조회타임아웃은타임아웃오류로변환한다(
            CapturedOutput output
    ) {
        CharacterClientImpl timeoutClient =
                createFailingClient(
                        new SocketTimeoutException(
                                "read timeout"
                        )
                );

        CharacterException exception =
                catchThrowableOfType(
                        () -> timeoutClient.getOverallRanking(
                                OCID,
                                RANKING_DATE
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_TIMEOUT
                );

        assertSensitiveValuesAreNotLogged(output);
    }

    @Test
    void 오류응답파싱에실패하면클라이언트오류로변환한다(
            CapturedOutput output
    ) {
        expectGetRequest(
                CHARACTER_BASIC_PATH,
                "ocid",
                OCID,
                withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("invalid-error-response")
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getCharacterBasic(OCID),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_CLIENT_ERROR
                );

        assertThat(output)
                .contains("오류 응답 파싱에 실패했습니다.");

        assertSensitiveValuesAreNotLogged(output);

        mockServer.verify();
    }

    @Test
    void 서버5xx응답은서버오류로변환한다(
            CapturedOutput output
    ) {
        expectGetRequest(
                CHARACTER_BASIC_PATH,
                "ocid",
                OCID,
                withStatus(
                        HttpStatus.INTERNAL_SERVER_ERROR
                )
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient
                                .getCharacterBasic(OCID),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_SERVER_ERROR
                );

        assertSensitiveValuesAreNotLogged(output);

        mockServer.verify();
    }

    @Test
    void 응답역직렬화에실패하면잘못된응답오류로변환한다(
            CapturedOutput output
    ) {
        expectGetRequest(
                CHARACTER_BASIC_PATH,
                "ocid",
                OCID,
                withSuccess(
                        "{invalid-json",
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getCharacterBasic(OCID),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_RESPONSE_INVALID
                );

        assertSensitiveValuesAreNotLogged(output);

        mockServer.verify();
    }

    @Test
    void 응답본문이없으면잘못된응답오류를반환한다() {
        expectGetRequest(
                CHARACTER_OCID_PATH,
                "character_name",
                CHARACTER_NAME,
                withStatus(HttpStatus.OK)
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getOcid(
                                CHARACTER_NAME
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_RESPONSE_INVALID
                );

        mockServer.verify();
    }

    @Test
    void OCID가비어있으면잘못된응답오류를반환한다() {
        expectGetRequest(
                CHARACTER_OCID_PATH,
                "character_name",
                CHARACTER_NAME,
                withSuccess(
                        """
                        {
                          "ocid": ""
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getOcid(
                                CHARACTER_NAME
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_RESPONSE_INVALID
                );

        mockServer.verify();
    }

    @Test
    void OCID가누락되면잘못된응답오류를반환한다() {
        expectGetRequest(
                CHARACTER_OCID_PATH,
                "character_name",
                CHARACTER_NAME,
                withSuccess(
                        "{}",
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getOcid(
                                CHARACTER_NAME
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_RESPONSE_INVALID
                );

        mockServer.verify();
    }

    @Test
    void 타임아웃은타임아웃오류로변환한다() {
        CharacterClientImpl timeoutClient =
                createFailingClient(
                        new SocketTimeoutException(
                                "read timeout"
                        )
                );

        CharacterException exception =
                catchThrowableOfType(
                        () -> timeoutClient.getOcid(
                                CHARACTER_NAME
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_TIMEOUT
                );
    }

    @Test
    void 일반통신오류는서버오류로변환한다(
            CapturedOutput output
    ) {
        CharacterClientImpl connectionFailureClient =
                createFailingClient(
                        new ConnectException(
                                "connection refused"
                        )
                );

        CharacterException exception =
                catchThrowableOfType(
                        () -> connectionFailureClient
                                .getCharacterBasic(OCID),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_SERVER_ERROR
                );

        assertSensitiveValuesAreNotLogged(output);
    }

    private String createSkillResponseJson(
            String skillGrade,
            String skillName
    ) {
        return """
                {
                  "character_class": "팬텀",
                  "character_skill_grade": "%s",
                  "character_skill": [
                    {
                      "skill_name": "%s",
                      "skill_level": 30,
                      "skill_icon": "skill-icon"
                    }
                  ]
                }
                """.formatted(
                skillGrade,
                skillName
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
            String queryParameterName,
            String queryParameterValue,
            ResponseCreator responseCreator
    ) {
        mockServer.expect(request -> {
                    assertThat(request.getMethod())
                            .isEqualTo(HttpMethod.GET);

                    assertThat(request.getURI().getPath())
                            .isEqualTo(path);

                    assertThat(request.getURI().getQuery())
                            .isEqualTo(
                                    queryParameterName
                                            + "="
                                            + queryParameterValue
                            );
                })
                .andRespond(responseCreator);
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

                    String query =
                            URLDecoder.decode(
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

    private CharacterClientImpl createFailingClient(
            IOException exception
    ) {
        ClientHttpRequestFactory requestFactory =
                (uri, httpMethod) -> {
                    throw exception;
                };

        RestClient restClient =
                RestClient.builder()
                        .baseUrl(BASE_URL)
                        .defaultHeader(
                                "x-nxopen-api-key",
                                NEXON_API_KEY
                        )
                        .requestFactory(requestFactory)
                        .build();

        return new CharacterClientImpl(
                restClient,
                objectMapper
        );
    }
}
