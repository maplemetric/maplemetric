package com.maplemetric.character.application.calculator;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

public record AdditionalOptionValues(
        BigDecimal str,
        BigDecimal dex,
        BigDecimal intelligence,
        BigDecimal luk,
        BigDecimal maxHp,
        BigDecimal attackPower,
        BigDecimal magicPower,
        BigDecimal allStat
) {

    public boolean isAllZero() {
        return Stream.of(
                        str,
                        dex,
                        intelligence,
                        luk,
                        maxHp,
                        attackPower,
                        magicPower,
                        allStat
                )
                .allMatch(value -> value.signum() == 0);
    }

    public boolean isRelevantZero(
            CharacterStatProfile profile
    ) {
        boolean statsAreZero = Stream.concat(
                        profile.mainStats().stream(),
                        profile.subStats().stream()
                )
                .map(stat -> valueOf(stat))
                .allMatch(value -> value.signum() == 0);

        return statsAreZero
                && power(profile.powerType()).signum() == 0
                && allStat.signum() == 0;
    }

    public BigDecimal sum(
            List<CharacterStatProfile.Stat> stats
    ) {
        return stats.stream()
                .map(stat -> valueOf(stat))
                .reduce(
                        BigDecimal.ZERO,
                        (sum, value) -> sum.add(value)
                );
    }

    public BigDecimal power(
            PowerType powerType
    ) {
        return switch (powerType) {
            case ATTACK_POWER -> attackPower;
            case MAGIC_POWER -> magicPower;
            case NONE -> BigDecimal.ZERO;
        };
    }

    private BigDecimal valueOf(
            CharacterStatProfile.Stat stat
    ) {
        return switch (stat) {
            case STR -> str;
            case DEX -> dex;
            case INT -> intelligence;
            case LUK -> luk;
            case MAX_HP -> maxHp;
        };
    }
}
