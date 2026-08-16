package com.maplemetric.character.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.common.nexon.NexonRateLimitProperties;
import com.maplemetric.common.nexon.NexonRequestRateGate;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import com.maplemetric.character.infrastructure.client.dto.CharacterAbilityResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterDojangResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterEquipmentResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHyperStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterLinkSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterPopularityResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSetEffectResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSymbolResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterUnionResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterVMatrixResponse;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

    private static final String CHARACTER_POPULARITY_PATH =
            "/maplestory/v1/character/popularity";

    private static final String CHARACTER_HYPER_STAT_PATH =
            "/maplestory/v1/character/hyper-stat";

    private static final String CHARACTER_ABILITY_PATH =
            "/maplestory/v1/character/ability";

    private static final String CHARACTER_UNION_PATH =
            "/maplestory/v1/user/union";

    private static final String CHARACTER_SYMBOL_PATH =
            "/maplestory/v1/character/symbol-equipment";

    private static final String CHARACTER_SET_EFFECT_PATH =
            "/maplestory/v1/character/set-effect";

    private static final String CHARACTER_SKILL_PATH =
            "/maplestory/v1/character/skill";

    private static final String CHARACTER_LINK_SKILL_PATH =
            "/maplestory/v1/character/link-skill";

    private static final String CHARACTER_EQUIPMENT_PATH =
            "/maplestory/v1/character/item-equipment";

    private static final String CHARACTER_V_MATRIX_PATH =
            "/maplestory/v1/character/vmatrix";

    private static final String CHARACTER_HEXA_MATRIX_PATH =
            "/maplestory/v1/character/hexamatrix";

    private static final String CHARACTER_HEXA_MATRIX_STAT_PATH =
            "/maplestory/v1/character/hexamatrix-stat";

    private static final String CHARACTER_DOJANG_PATH =
            "/maplestory/v1/character/dojang";

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
                objectMapper,
                new NexonRequestRateGate(new NexonRateLimitProperties(1000))
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
    void 심볼수치와성장치를바인딩한다() {
        expectGetRequest(
                CHARACTER_SYMBOL_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "symbol": [
                            {
                              "symbol_name": "아케인심볼 : 소멸의 여로",
                              "symbol_description": "아케인포스 530 증가",
                              "symbol_other_effect_description": "STR 2200 증가",
                              "symbol_force": "530",
                              "symbol_level": 20,
                              "symbol_str": "2200",
                              "symbol_dex": "0",
                              "symbol_int": "100",
                              "symbol_luk": "0",
                              "symbol_hp": "0",
                              "symbol_drop_rate": "1",
                              "symbol_meso_rate": "2",
                              "symbol_exp_rate": "3",
                              "symbol_growth_count": 2678,
                              "symbol_require_growth_count": 4000
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterSymbolResponse result =
                characterClient.getCharacterSymbol(OCID);

        assertThat(result.symbol())
                .extracting(
                        symbol -> symbol.symbolForce(),
                        symbol -> symbol.symbolStr(),
                        symbol -> symbol.symbolIntelligence(),
                        symbol -> symbol.symbolDropRate(),
                        symbol -> symbol.symbolGrowthCount(),
                        symbol -> symbol.symbolRequireGrowthCount()
                )
                .containsExactly(
                        tuple("530", "2200", "100", "1", 2678, 4000)
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
    void 세트효과정보를조회한다() {
        expectGetRequest(
                CHARACTER_SET_EFFECT_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "set_effect": [
                            {
                              "set_name": "여명의 보스 세트",
                              "total_set_count": 2,
                              "set_effect_info": [
                                {
                                  "set_count": 2,
                                  "set_option": "보스 몬스터 공격 시 데미지 : +10%"
                                }
                              ],
                              "set_option_full": [
                                {
                                  "set_count": 2,
                                  "set_option": "보스 몬스터 공격 시 데미지 : +10%"
                                },
                                {
                                  "set_count": 3,
                                  "set_option": "올스탯 : +20"
                                }
                              ]
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterSetEffectResponse result =
                characterClient.getCharacterSetEffect(OCID);

        assertThat(result.setEffect())
                .extracting(
                        effect -> effect.setName(),
                        effect -> effect.totalSetCount(),
                        effect -> effect.setEffectInfo().size(),
                        effect -> effect.setOptionFull().get(1).setCount(),
                        effect -> effect.setOptionFull().get(1).setOption()
                )
                .containsExactly(
                        tuple("여명의 보스 세트", 2, 1, 3, "올스탯 : +20")
                );

        mockServer.verify();
    }

    @Test
    void 스킬설명과효과를바인딩한다() {
        expectGetRequest(
                CHARACTER_SKILL_PATH,
                Map.of(
                        "ocid", OCID,
                        "character_skill_grade", "6"
                ),
                withSuccess(
                        """
                        {
                          "character_skill": [
                            {
                              "skill_name": "템페스트 오브 카드 VI",
                              "skill_level": 18,
                              "skill_icon": "tempest-icon",
                              "skill_description": "카드를 흩뿌린다.",
                              "skill_effect": "데미지 500%",
                              "skill_effect_next": "데미지 520%"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterSkillResponse result =
                characterClient.getCharacterSkill(OCID, "6");

        assertThat(result.characterSkill())
                .extracting(
                        skill -> skill.skillDescription(),
                        skill -> skill.skillEffect(),
                        skill -> skill.skillEffectNext()
                )
                .containsExactly(
                        tuple(
                                "카드를 흩뿌린다.",
                                "데미지 500%",
                                "데미지 520%"
                        )
                );

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
                              "skill_icon": "link-icon",
                              "skill_description": "치명적인 일격을 노린다.",
                              "skill_effect": "크리티컬 확률 10% 증가",
                              "skill_effect_next": "크리티컬 확률 15% 증가"
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
                        skill -> skill.skillIcon(),
                        skill -> skill.skillDescription(),
                        skill -> skill.skillEffectNext()
                )
                .containsExactly(
                        tuple(
                                "데들리 인스팅트",
                                2,
                                "link-icon",
                                "치명적인 일격을 노린다.",
                                "크리티컬 확률 15% 증가"
                        )
                );

        mockServer.verify();
    }

    @Test
    void 장비프리셋1과2와3을모두바인딩한다() {
        // SnakeCaseStrategy는 끝자리 숫자 앞에 밑줄을 넣지 않아
        // item_equipment_preset_1을 스스로 찾지 못한다. 이름을 명시하지 않으면
        // 세 프리셋이 모두 null로 바인딩된다.
        expectGetRequest(
                CHARACTER_EQUIPMENT_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "character_class": "팬텀",
                          "preset_no": 2,
                          "item_equipment": [
                            { "item_equipment_slot": "모자", "item_name": "현재 장착 모자" }
                          ],
                          "item_equipment_preset_1": [
                            { "item_equipment_slot": "모자", "item_name": "프리셋1 모자" }
                          ],
                          "item_equipment_preset_2": [
                            { "item_equipment_slot": "모자", "item_name": "프리셋2 모자" }
                          ],
                          "item_equipment_preset_3": [
                            { "item_equipment_slot": "모자", "item_name": "프리셋3 모자" }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterEquipmentResponse result =
                characterClient.getCharacterEquipment(OCID);

        assertThat(result.presetNo())
                .isEqualTo(2);
        assertThat(result.itemEquipment())
                .extracting(item -> item.itemName())
                .containsExactly("현재 장착 모자");
        assertThat(result.itemEquipmentPreset1())
                .extracting(item -> item.itemName())
                .containsExactly("프리셋1 모자");
        assertThat(result.itemEquipmentPreset2())
                .extracting(item -> item.itemName())
                .containsExactly("프리셋2 모자");
        assertThat(result.itemEquipmentPreset3())
                .extracting(item -> item.itemName())
                .containsExactly("프리셋3 모자");

        mockServer.verify();
    }

    @Test
    void 장비옵션의INT수치를바인딩한다() {
        // Nexon 필드명은 int인데 Java 예약어라 DTO에서는 intelligence로 둔다.
        // 이름을 명시하지 않으면 INT만 조용히 null이 된다.
        expectGetRequest(
                CHARACTER_EQUIPMENT_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "item_equipment": [
                            {
                              "item_equipment_slot": "모자",
                              "item_name": "테스트 모자",
                              "item_total_option": {
                                "str": "10",
                                "dex": "20",
                                "int": "196",
                                "luk": "30"
                              },
                              "item_add_option": {
                                "int": "44"
                              }
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterEquipmentResponse result =
                characterClient.getCharacterEquipment(OCID);

        // ItemOption은 total·add·etc·starforce가 공유하는 record라 한 곳만 검증해도
        // 바인딩은 같지만, 소비되는 경로마다 실제로 값이 오는지 함께 고정한다.
        assertThat(result.itemEquipment())
                .extracting(
                        item -> item.itemTotalOption().str(),
                        item -> item.itemTotalOption().dex(),
                        item -> item.itemTotalOption().intelligence(),
                        item -> item.itemTotalOption().luk(),
                        item -> item.itemAddOption().intelligence()
                )
                .containsExactly(
                        tuple("10", "20", "196", "30", "44")
                );

        mockServer.verify();
    }

    @Test
    void 장비기본옵션과요구레벨을바인딩한다() {
        expectGetRequest(
                CHARACTER_EQUIPMENT_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "item_equipment": [
                            {
                              "item_equipment_slot": "무기",
                              "item_name": "테스트 무기",
                              "item_base_option": {
                                "str": "10",
                                "int": "150",
                                "attack_power": "326",
                                "boss_damage": "30",
                                "base_equipment_level": 200
                              }
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterEquipmentResponse result =
                characterClient.getCharacterEquipment(OCID);

        assertThat(result.itemEquipment())
                .extracting(
                        item -> item.itemBaseOption().str(),
                        item -> item.itemBaseOption().intelligence(),
                        item -> item.itemBaseOption().attackPower(),
                        item -> item.itemBaseOption().bossDamage(),
                        item -> item.itemBaseOption().baseEquipmentLevel()
                )
                .containsExactly(
                        tuple("10", "150", "326", "30", 200)
                );

        mockServer.verify();
    }

    @Test
    void 장비익셉셔널강화옵션을바인딩한다() {
        expectGetRequest(
                CHARACTER_EQUIPMENT_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "item_equipment": [
                            {
                              "item_equipment_slot": "장갑",
                              "item_name": "테스트 장갑",
                              "item_exceptional_option": {
                                "str": "50",
                                "dex": "50",
                                "int": "50",
                                "luk": "50",
                                "max_hp": "500",
                                "max_mp": "500",
                                "attack_power": "10",
                                "magic_power": "10",
                                "exceptional_upgrade": 2
                              }
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterEquipmentResponse result =
                characterClient.getCharacterEquipment(OCID);

        assertThat(result.itemEquipment())
                .extracting(
                        item -> item.itemExceptionalOption().str(),
                        item -> item.itemExceptionalOption().intelligence(),
                        item -> item.itemExceptionalOption().maxHp(),
                        item -> item.itemExceptionalOption().attackPower(),
                        item -> item.itemExceptionalOption().exceptionalUpgrade()
                )
                .containsExactly(
                        tuple("50", "50", "500", "10", 2)
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
    void 무릉최고기록정보는date없이ocid로조회한다() {
        expectGetRequest(
                CHARACTER_DOJANG_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "date": "2026-07-19T00:00+09:00",
                          "character_class": "팬텀",
                          "world_name": "루나",
                          "dojang_best_floor": 57,
                          "date_dojang_record": "2026-07-18T00:00+09:00",
                          "dojang_best_time": 600
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterDojangResponse result =
                characterClient.getCharacterDojang(OCID);

        assertThat(result.dojangBestFloor())
                .isEqualTo(57);

        assertThat(result.date())
                .isEqualTo("2026-07-19T00:00+09:00");

        assertThat(result.characterClass())
                .isEqualTo("팬텀");

        assertThat(result.worldName())
                .isEqualTo("루나");

        assertThat(result.dateDojangRecord())
                .isEqualTo("2026-07-18T00:00+09:00");

        assertThat(result.dojangBestTime())
                .isEqualTo(600);

        mockServer.verify();
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

    @Test
    void 인기도정보를ocid로조회하고역직렬화한다() {
        expectGetRequest(
                CHARACTER_POPULARITY_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "date": "2026-07-19T00:00+09:00",
                          "popularity": 1234
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterPopularityResponse result =
                characterClient.getCharacterPopularity(OCID);

        assertThat(result.date())
                .isEqualTo("2026-07-19T00:00+09:00");

        assertThat(result.popularity())
                .isEqualTo(1234L);

        mockServer.verify();
    }

    @Test
    void 하이퍼스탯정보를ocid로조회하고프리셋을역직렬화한다() {
        expectGetRequest(
                CHARACTER_HYPER_STAT_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "date": "2026-07-19T00:00+09:00",
                          "character_class": "팬텀",
                          "use_preset_no": "2",
                          "use_available_hyper_stat": 5,
                          "hyper_stat_preset_1": [
                            {
                              "stat_type": "크리티컬 확률",
                              "stat_point": 15,
                              "stat_level": 5,
                              "stat_increase": "크리티컬 확률 5% 증가"
                            }
                          ],
                          "hyper_stat_preset_1_remain_point": 10,
                          "hyper_stat_preset_2": [],
                          "hyper_stat_preset_2_remain_point": 20,
                          "hyper_stat_preset_3": [],
                          "hyper_stat_preset_3_remain_point": 30
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterHyperStatResponse result =
                characterClient.getCharacterHyperStat(OCID);

        assertThat(result.usePresetNo())
                .isEqualTo("2");

        assertThat(result.useAvailableHyperStat())
                .isEqualTo(5L);

        assertThat(result.hyperStatPreset1())
                .extracting(
                        stat -> stat.statType(),
                        stat -> stat.statPoint(),
                        stat -> stat.statLevel()
                )
                .containsExactly(
                        tuple("크리티컬 확률", 15L, 5)
                );

        mockServer.verify();
    }

    @Test
    void 어빌리티정보를ocid로조회하고프리셋을역직렬화한다() {
        expectGetRequest(
                CHARACTER_ABILITY_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "date": "2026-07-19T00:00+09:00",
                          "ability_grade": "레전드리",
                          "ability_info": [
                            {
                              "ability_no": "1",
                              "ability_grade": "레전드리",
                              "ability_value": "보스 몬스터 공격 시 데미지 20% 증가"
                            }
                          ],
                          "remain_fame": 100,
                          "preset_no": 1,
                          "ability_preset_1": {
                            "ability_preset_grade": "레전드리",
                            "ability_info": []
                          },
                          "ability_preset_2": null,
                          "ability_preset_3": null
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterAbilityResponse result =
                characterClient.getCharacterAbility(OCID);

        assertThat(result.abilityGrade())
                .isEqualTo("레전드리");

        assertThat(result.abilityInfo())
                .extracting(
                        option -> option.abilityNo(),
                        option -> option.abilityValue()
                )
                .containsExactly(
                        tuple(
                                "1",
                                "보스 몬스터 공격 시 데미지 20% 증가"
                        )
                );

        assertThat(result.abilityPreset1().abilityPresetGrade())
                .isEqualTo("레전드리");

        mockServer.verify();
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
                objectMapper,
                new NexonRequestRateGate(new NexonRateLimitProperties(1000))
        );
    }
}
