package com.maplemetric.character.application.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class HexaStatIncreaseTest {

    @ParameterizedTest
    @CsvSource({
            // 실측 대조 기준값 (구간 누진: Lv1~4 = R, Lv5~7 = 2R, Lv8~10 = 3R)
            "공격력 증가, 8, 65",
            "공격력 증가, 9, 80",
            "공격력 증가, 10, 95",
            "마력 증가, 3, 15",
            "마력 증가, 5, 30",
            "주력 스탯 증가, 1, 100",
            "주력 스탯 증가, 6, 800",
            "주력 스탯 증가, 10, 1900",
            "크리티컬 데미지 증가, 4, 1.4",
            "크리티컬 데미지 증가, 5, 2.1",
            "크리티컬 데미지 증가, 9, 5.6",
            "보스 데미지 증가, 2, 2",
            "보스 데미지 증가, 7, 10",
            "보스 데미지 증가, 8, 13",
            "데미지 증가, 3, 2.25",
            "데미지 증가, 7, 7.5",
            "데미지 증가, 10, 14.25"
    })
    void 메인스탯증가수치를구간누진으로계산한다(
            String statName,
            int statLevel,
            String expected
    ) {
        assertThat(HexaStatIncrease.ofMain(statName, statLevel))
                .isEqualByComparingTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            // 서브 스탯은 전 구간 선형
            "공격력 증가, 5, 25",
            "공격력 증가, 10, 50",
            "마력 증가, 7, 35",
            "주력 스탯 증가, 4, 400",
            "주력 스탯 증가, 10, 1000",
            "크리티컬 데미지 증가, 3, 1.05",
            "크리티컬 데미지 증가, 10, 3.5",
            "보스 데미지 증가, 5, 5",
            "보스 데미지 증가, 9, 9",
            "데미지 증가, 3, 2.25",
            "데미지 증가, 8, 6"
    })
    void 서브스탯증가수치를선형으로계산한다(
            String statName,
            int statLevel,
            String expected
    ) {
        assertThat(HexaStatIncrease.ofSub(statName, statLevel))
                .isEqualByComparingTo(expected);
    }

    @Test
    void 방어율무시는검증되지않아증가수치를내지않는다() {
        assertThat(HexaStatIncrease.ofMain("방어율 무시 증가", 10))
                .isNull();
        assertThat(HexaStatIncrease.ofSub("방어율 무시 증가", 10))
                .isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  ", "알 수 없는 스탯"})
    void 규칙을모르는스탯이름은예외없이null을반환한다(String statName) {
        assertThat(HexaStatIncrease.ofMain(statName, 5))
                .isNull();
        assertThat(HexaStatIncrease.ofSub(statName, 5))
                .isNull();
    }

    @Test
    void 레벨이없거나음수면null을반환한다() {
        assertThat(HexaStatIncrease.ofMain("공격력 증가", null))
                .isNull();
        assertThat(HexaStatIncrease.ofSub("공격력 증가", -1))
                .isNull();
    }

    @Test
    void 레벨0은증가수치가없다() {
        assertThat(HexaStatIncrease.ofMain("공격력 증가", 0))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(HexaStatIncrease.ofSub("공격력 증가", 0))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void 최대레벨을넘는값은최대레벨로고정한다() {
        assertThat(HexaStatIncrease.ofMain("공격력 증가", 99))
                .isEqualByComparingTo("95");
        assertThat(HexaStatIncrease.ofSub("공격력 증가", 99))
                .isEqualByComparingTo("50");
    }
}
