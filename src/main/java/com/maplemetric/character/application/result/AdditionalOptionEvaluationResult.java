package com.maplemetric.character.application.result;

import java.math.BigDecimal;
import java.util.List;

public record AdditionalOptionEvaluationResult(
        boolean calculable,
        BigDecimal score,
        Integer grade,
        String formulaVersion,
        String calculationType,
        CriteriaResult criteria,
        String reason
) {

    public static AdditionalOptionEvaluationResult calculated(
            BigDecimal score,
            Integer grade,
            String formulaVersion,
            String calculationType,
            CriteriaResult criteria
    ) {
        return new AdditionalOptionEvaluationResult(
                true,
                score,
                grade,
                formulaVersion,
                calculationType,
                criteria,
                null
        );
    }

    public static AdditionalOptionEvaluationResult notCalculable(
            String formulaVersion,
            String calculationType,
            CriteriaResult criteria,
            String reason
    ) {
        return new AdditionalOptionEvaluationResult(
                false,
                null,
                null,
                formulaVersion,
                calculationType,
                criteria,
                reason
        );
    }

    public static AdditionalOptionEvaluationResult unsupported(
            String formulaVersion
    ) {
        return notCalculable(
                formulaVersion,
                "UNSUPPORTED",
                null,
                "지원하지 않는 직업입니다."
        );
    }

    public record CriteriaResult(
            List<String> mainStats,
            List<String> subStats,
            boolean usesAttackPower,
            boolean usesMagicPower,
            BigDecimal mainStatWeight,
            BigDecimal subStatWeight,
            BigDecimal attackPowerWeight,
            BigDecimal magicPowerWeight,
            BigDecimal allStatRateWeight
    ) {
    }
}
