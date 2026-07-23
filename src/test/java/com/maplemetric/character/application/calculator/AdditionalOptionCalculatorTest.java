package com.maplemetric.character.application.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.maplemetric.character.application.port.out.LoadCharacterEquipmentPort.ItemOption;
import com.maplemetric.character.application.result.AdditionalOptionEvaluationResult;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AdditionalOptionCalculatorTest {

    private AdditionalOptionCalculationPolicyV1 policy;
    private AdditionalOptionCalculator calculator;

    @BeforeEach
    void setUp() {
        policy = new AdditionalOptionCalculationPolicyV1();
        calculator = new AdditionalOptionCalculator(policy);
    }

    @ParameterizedTest
    @MethodSource("generalProfileOptions")
    void 일반직업의추가옵션점수를계산한다(
            String characterClass,
            ItemOption option,
            String calculationType
    ) {
        AdditionalOptionEvaluationResult result =
                calculator.calculate(characterClass, option);

        assertThat(result.calculable()).isTrue();
        assertThat(result.score())
                .isEqualByComparingTo("158.0");
        assertThat(result.grade()).isEqualTo(150);
        assertThat(result.formulaVersion()).isEqualTo("v1");
        assertThat(result.calculationType())
                .isEqualTo(calculationType);
        assertThat(result.reason()).isNull();
    }

    @Test
    void 마법직업은공격력이아닌마력을사용한다() {
        ItemOption option = option(
                "0", "0", "80", "40",
                "0", "100", "6", "5"
        );

        AdditionalOptionEvaluationResult result =
                calculator.calculate(
                        "아크메이지(불,독)",
                        option
                );

        assertThat(result.score())
                .isEqualByComparingTo("158.0");
        assertThat(result.criteria().usesAttackPower())
                .isFalse();
        assertThat(result.criteria().usesMagicPower())
                .isTrue();
    }

    @Test
    void 추가옵션이null이면계산불가로반환한다() {
        AdditionalOptionEvaluationResult result =
                calculator.calculate("팬텀", null);

        assertNotCalculable(result, "LUK");
        assertThat(result.reason())
                .isEqualTo("추가옵션 정보가 없습니다.");
    }

    @Test
    void 원본추가옵션이모두0이면계산불가로반환한다() {
        AdditionalOptionEvaluationResult result =
                calculator.calculate(
                        "팬텀",
                        option("0", "", null, "0", "0", "0", "0", "0")
                );

        assertNotCalculable(result, "LUK");
        assertThat(result.reason())
                .isEqualTo("추가옵션 값이 모두 0입니다.");
    }

    @Test
    void 현재프로필에반영되는값이모두0이면계산불가로반환한다() {
        AdditionalOptionEvaluationResult result =
                calculator.calculate(
                        "팬텀",
                        option("100", "0", "30", "0", "20", "0", "10", "0")
                );

        assertNotCalculable(result, "LUK");
        assertThat(result.reason()).isEqualTo(
                "현재 직업 계산 기준에 반영되는 추가옵션이 없습니다."
        );
    }

    @ParameterizedTest
    @MethodSource("unsupportedClasses")
    void 미지원직업은계산불가로반환한다(
            String characterClass
    ) {
        AdditionalOptionEvaluationResult result =
                calculator.calculate(
                        characterClass,
                        option("10", "10", "10", "10", "10", "1", "1", "1")
                );

        assertNotCalculable(result, "UNSUPPORTED");
        assertThat(result.criteria()).isNull();
        assertThat(result.reason())
                .isEqualTo("지원하지 않는 직업입니다.");
    }

    @Test
    void 정책을직접호출해도제논을계산하지않는다() {
        AdditionalOptionEvaluationResult result = policy.calculate(
                CharacterStatProfile.XENON,
                new AdditionalOptionValues(
                        BigDecimal.TEN,
                        BigDecimal.TEN,
                        BigDecimal.ZERO,
                        BigDecimal.TEN,
                        BigDecimal.ZERO,
                        BigDecimal.TEN,
                        BigDecimal.ZERO,
                        BigDecimal.ONE
                )
        );

        assertNotCalculable(result, "UNSUPPORTED");
        assertThat(result.criteria()).isNull();
    }

    @ParameterizedTest
    @MethodSource("invalidNumbers")
    void 잘못된숫자문자열은응답오류로변환한다(
            String value
    ) {
        CharacterException exception = catchThrowableOfType(
                () -> calculator.calculate(
                        "팬텀",
                        option("0", "0", "0", value, "0", "0", "0", "0")
                ),
                CharacterException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(
                CharacterErrorCode.NEXON_API_RESPONSE_INVALID
        );
    }

    @Test
    void 표시점수는반올림하고급수는rawScore를내림한다() {
        AdditionalOptionValues values = new AdditionalOptionValues(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("140"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        AdditionalOptionValues boundaryValues = new AdditionalOptionValues(
                values.str(),
                new BigDecimal("99.5"),
                values.intelligence(),
                values.luk(),
                values.maxHp(),
                values.attackPower(),
                values.magicPower(),
                values.allStat()
        );

        AdditionalOptionEvaluationResult result = policy.calculate(
                CharacterStatProfile.LUK,
                boundaryValues
        );

        assertThat(result.score())
                .isEqualByComparingTo("150.0");
        assertThat(result.grade()).isEqualTo(140);
    }

    @Test
    void 급수가int범위를초과하면응답오류로변환한다() {
        CharacterException exception = catchThrowableOfType(
                () -> calculator.calculate(
                        "팬텀",
                        option(
                                "0", "0", "0", "2147483650",
                                "0", "0", "0", "0"
                        )
                ),
                CharacterException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(
                CharacterErrorCode.NEXON_API_RESPONSE_INVALID
        );
    }

    private void assertNotCalculable(
            AdditionalOptionEvaluationResult result,
            String calculationType
    ) {
        assertThat(result.calculable()).isFalse();
        assertThat(result.score()).isNull();
        assertThat(result.grade()).isNull();
        assertThat(result.calculationType())
                .isEqualTo(calculationType);
    }

    private static Stream<Arguments> generalProfileOptions() {
        return Stream.of(
                Arguments.of(
                        "히어로",
                        option("80", "40", "0", "0", "0", "6", "0", "5"),
                        "STR"
                ),
                Arguments.of(
                        "보우마스터",
                        option("40", "80", "0", "0", "0", "6", "0", "5"),
                        "DEX"
                ),
                Arguments.of(
                        "아크메이지(불,독)",
                        option("0", "0", "80", "40", "0", "0", "6", "5"),
                        "INT"
                ),
                Arguments.of(
                        "팬텀",
                        option("0", "40", "0", "80", "0", "6", "0", "5"),
                        "LUK"
                )
        );
    }

    private static Stream<String> unsupportedClasses() {
        return Stream.of(
                "제논",
                "데몬어벤져",
                "지원하지않는직업",
                " 팬텀"
        );
    }

    private static Stream<String> invalidNumbers() {
        return Stream.of(
                "invalid",
                "-1",
                "1.0"
        );
    }

    private static ItemOption option(
            String str,
            String dex,
            String intelligence,
            String luk,
            String maxHp,
            String attackPower,
            String magicPower,
            String allStat
    ) {
        return new ItemOption(
                str,
                dex,
                intelligence,
                luk,
                maxHp,
                null,
                attackPower,
                magicPower,
                null,
                null,
                null,
                null,
                null,
                allStat,
                null,
                null,
                null,
                null
        );
    }
}
