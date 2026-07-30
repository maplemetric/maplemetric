package com.maplemetric.character.application.calculator;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.function.IntUnaryOperator;

/**
 * HEXA 스탯의 레벨을 실제 증가 수치로 환산한다.
 *
 * Nexon API는 스탯 이름과 레벨만 주고 증가 수치를 주지 않아, 환산 규칙을 우리가 보유한다.
 * 규칙은 랭커 표본과 게임 내 표시값을 대조해 도출했다.
 *
 * 메인 스탯은 구간 누진(Lv1~4 = R, Lv5~7 = 2R, Lv8~10 = 3R)이고
 * 서브 스탯은 전 구간 R 선형이다.
 *
 * {@code 방어율 무시 증가}는 채택한 표본을 확보하지 못해 R을 검증하지 못했다.
 * 검증되지 않은 값을 사실처럼 내보내지 않기 위해 목록에서 제외하고 {@code null}을 반환한다.
 */
public enum HexaStatIncrease {

    CRITICAL_DAMAGE("크리티컬 데미지 증가", "0.35"),
    BOSS_DAMAGE("보스 데미지 증가", "1"),
    DAMAGE("데미지 증가", "0.75"),
    ATTACK_POWER("공격력 증가", "5"),
    MAGIC_POWER("마력 증가", "5"),
    MAIN_STAT("주력 스탯 증가", "100");

    private static final int FIRST_SECTION_MAX_LEVEL = 4;

    private static final int SECOND_SECTION_MAX_LEVEL = 7;

    private static final int MAX_LEVEL = 10;

    private final String statName;

    private final BigDecimal unitIncrease;

    HexaStatIncrease(
            String statName,
            String unitIncrease
    ) {
        this.statName = statName;
        this.unitIncrease = new BigDecimal(unitIncrease);
    }

    /**
     * 메인 스탯 증가 수치를 반환한다. 규칙을 모르는 스탯이면 {@code null}을 반환한다.
     */
    public static BigDecimal ofMain(
            String statName,
            Integer statLevel
    ) {
        return calculate(
                statName,
                statLevel,
                level -> mainMultiplier(level)
        );
    }

    /**
     * 서브 스탯 증가 수치를 반환한다. 규칙을 모르는 스탯이면 {@code null}을 반환한다.
     */
    public static BigDecimal ofSub(
            String statName,
            Integer statLevel
    ) {
        return calculate(
                statName,
                statLevel,
                level -> level
        );
    }

    private static BigDecimal calculate(
            String statName,
            Integer statLevel,
            IntUnaryOperator multiplier
    ) {
        if (statLevel == null || statLevel < 0) {
            return null;
        }

        HexaStatIncrease stat = find(statName);

        if (stat == null) {
            return null;
        }

        int level = Math.min(statLevel, MAX_LEVEL);

        return stat.unitIncrease.multiply(
                BigDecimal.valueOf(multiplier.applyAsInt(level))
        );
    }

    private static int mainMultiplier(int level) {
        int firstSection =
                Math.min(level, FIRST_SECTION_MAX_LEVEL);

        int secondSection = Math.max(
                0,
                Math.min(level, SECOND_SECTION_MAX_LEVEL)
                        - FIRST_SECTION_MAX_LEVEL
        );

        int thirdSection = Math.max(
                0,
                level - SECOND_SECTION_MAX_LEVEL
        );

        return firstSection
                + secondSection * 2
                + thirdSection * 3;
    }

    private static HexaStatIncrease find(String statName) {
        if (statName == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(stat -> stat.statName.equals(statName.trim()))
                .findFirst()
                .orElse(null);
    }
}
