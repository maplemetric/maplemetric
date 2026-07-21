package com.maplemetric.character.application.calculator;

import com.maplemetric.character.application.result.AdditionalOptionEvaluationResult;

public interface AdditionalOptionCalculationPolicy {

    String formulaVersion();

    AdditionalOptionEvaluationResult.CriteriaResult criteria(
            CharacterStatProfile profile
    );

    AdditionalOptionEvaluationResult calculate(
            CharacterStatProfile profile,
            AdditionalOptionValues values
    );
}
