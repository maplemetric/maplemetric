package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.AdditionalOptionEvaluationResult;
import java.math.BigDecimal;
import java.util.List;

public record AdditionalOptionEvaluationResponse(
        boolean calculable,
        BigDecimal score,
        Integer grade,
        String formulaVersion,
        String calculationType,
        CriteriaResponse criteria,
        String reason
) {

    public static AdditionalOptionEvaluationResponse from(
            AdditionalOptionEvaluationResult result
    ) {
        return new AdditionalOptionEvaluationResponse(
                result.calculable(),
                result.score(),
                result.grade(),
                result.formulaVersion(),
                result.calculationType(),
                CriteriaResponse.from(result.criteria()),
                result.reason()
        );
    }

    public record CriteriaResponse(
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

        public static CriteriaResponse from(
                AdditionalOptionEvaluationResult.CriteriaResult result
        ) {
            if (result == null) {
                return null;
            }

            return new CriteriaResponse(
                    result.mainStats(),
                    result.subStats(),
                    result.usesAttackPower(),
                    result.usesMagicPower(),
                    result.mainStatWeight(),
                    result.subStatWeight(),
                    result.attackPowerWeight(),
                    result.magicPowerWeight(),
                    result.allStatRateWeight()
            );
        }
    }
}
