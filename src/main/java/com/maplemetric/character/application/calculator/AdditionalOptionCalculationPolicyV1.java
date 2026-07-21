package com.maplemetric.character.application.calculator;

import com.maplemetric.character.application.result.AdditionalOptionEvaluationResult;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AdditionalOptionCalculationPolicyV1
        implements AdditionalOptionCalculationPolicy {

    private static final String FORMULA_VERSION = "v1";

    private static final BigDecimal MAIN_STAT_WEIGHT = new BigDecimal("1.0");

    private static final BigDecimal SUB_STAT_WEIGHT = new BigDecimal("0.1");

    private static final BigDecimal ATTACK_POWER_WEIGHT = new BigDecimal("4.0");

    private static final BigDecimal MAGIC_POWER_WEIGHT = new BigDecimal("4.0");

    private static final BigDecimal ALL_STAT_RATE_WEIGHT = new BigDecimal("10.0");

    @Override
    public String formulaVersion() {
        return FORMULA_VERSION;
    }

    @Override
    public AdditionalOptionEvaluationResult.CriteriaResult criteria(
            CharacterStatProfile profile
    ) {
        if (!profile.calculable()) {
            return null;
        }

        return new AdditionalOptionEvaluationResult.CriteriaResult(
                convertStats(profile.mainStats()),
                convertStats(profile.subStats()),
                profile.powerType() == PowerType.ATTACK_POWER,
                profile.powerType() == PowerType.MAGIC_POWER,
                MAIN_STAT_WEIGHT,
                SUB_STAT_WEIGHT,
                ATTACK_POWER_WEIGHT,
                MAGIC_POWER_WEIGHT,
                ALL_STAT_RATE_WEIGHT
        );
    }

    @Override
    public AdditionalOptionEvaluationResult calculate(
            CharacterStatProfile profile,
            AdditionalOptionValues values
    ) {
        if (!profile.calculable()) {
            return AdditionalOptionEvaluationResult.unsupported(
                    formulaVersion()
            );
        }

        BigDecimal powerWeight = switch (profile.powerType()) {
            case ATTACK_POWER -> ATTACK_POWER_WEIGHT;
            case MAGIC_POWER -> MAGIC_POWER_WEIGHT;
            case NONE -> BigDecimal.ZERO;
        };

        BigDecimal rawScore = values.sum(profile.mainStats())
                .multiply(MAIN_STAT_WEIGHT)
                .add(
                        values.sum(profile.subStats())
                                .multiply(SUB_STAT_WEIGHT)
                )
                .add(
                        values.power(profile.powerType())
                                .multiply(powerWeight)
                )
                .add(
                        values.allStat()
                                .multiply(ALL_STAT_RATE_WEIGHT)
                );

        BigDecimal score = rawScore.setScale(
                1,
                RoundingMode.HALF_UP
        );

        return AdditionalOptionEvaluationResult.calculated(
                score,
                calculateGrade(rawScore),
                formulaVersion(),
                profile.name(),
                criteria(profile)
        );
    }

    private Integer calculateGrade(
            BigDecimal rawScore
    ) {
        try {
            return rawScore.divide(
                            BigDecimal.TEN,
                            0,
                            RoundingMode.FLOOR
                    )
                    .multiply(BigDecimal.TEN)
                    .intValueExact();
        } catch (ArithmeticException exception) {
            throw new CharacterException(
                    CharacterErrorCode.NEXON_API_RESPONSE_INVALID
            );
        }
    }

    private List<String> convertStats(
            List<CharacterStatProfile.Stat> stats
    ) {
        return stats.stream()
                .map(stat -> stat.name())
                .toList();
    }
}
