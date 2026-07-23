package com.maplemetric.character.application.calculator;

import com.maplemetric.character.application.port.out.LoadCharacterEquipmentPort.ItemOption;
import com.maplemetric.character.application.result.AdditionalOptionEvaluationResult;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AdditionalOptionCalculator {

    private static final Pattern INTEGER_PATTERN = Pattern.compile("^[0-9]+$");

    private static final String NO_OPTION_REASON = "추가옵션 정보가 없습니다.";

    private static final String ALL_ZERO_REASON = "추가옵션 값이 모두 0입니다.";

    private static final String NO_RELEVANT_OPTION_REASON = "현재 직업 계산 기준에 반영되는 추가옵션이 없습니다.";

    private final AdditionalOptionCalculationPolicy policy;

    public AdditionalOptionCalculator(
            AdditionalOptionCalculationPolicy policy
    ) {
        this.policy = policy;
    }

    public AdditionalOptionEvaluationResult calculate(
            String characterClass,
            ItemOption option
    ) {
        CharacterStatProfile profile =
                CharacterStatProfile.from(characterClass)
                        .orElse(null);

        if (profile == null || !profile.calculable()) {
            return AdditionalOptionEvaluationResult.unsupported(
                    policy.formulaVersion()
            );
        }

        if (option == null) {
            return notCalculable(
                    profile,
                    NO_OPTION_REASON
            );
        }

        AdditionalOptionValues values = convert(option);

        if (values.isAllZero()) {
            return notCalculable(
                    profile,
                    ALL_ZERO_REASON
            );
        }

        if (values.isRelevantZero(profile)) {
            return notCalculable(
                    profile,
                    NO_RELEVANT_OPTION_REASON
            );
        }

        return policy.calculate(profile, values);
    }

    private AdditionalOptionEvaluationResult notCalculable(
            CharacterStatProfile profile,
            String reason
    ) {
        return AdditionalOptionEvaluationResult.notCalculable(
                policy.formulaVersion(),
                profile.name(),
                policy.criteria(profile),
                reason
        );
    }

    private AdditionalOptionValues convert(
            ItemOption option
    ) {
        return new AdditionalOptionValues(
                convertNumber(option.str()),
                convertNumber(option.dex()),
                convertNumber(option.intelligence()),
                convertNumber(option.luk()),
                convertNumber(option.maxHp()),
                convertNumber(option.attackPower()),
                convertNumber(option.magicPower()),
                convertNumber(option.allStat())
        );
    }

    private BigDecimal convertNumber(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }

        String normalizedValue = value.trim();

        if (!INTEGER_PATTERN
                .matcher(normalizedValue)
                .matches()) {
            throw invalidResponse();
        }

        try {
            return new BigDecimal(normalizedValue);
        } catch (NumberFormatException exception) {
            throw invalidResponse();
        }
    }

    private CharacterException invalidResponse() {
        return new CharacterException(
                CharacterErrorCode.NEXON_API_RESPONSE_INVALID
        );
    }
}
