package com.maplemetric.internal.application.properties;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

/**
 * 메우기 설정이 실제로 쓸 수 있는 값인지 확인한다.
 *
 * 예시 파일을 그대로 복사해 쓰는 사람이 있다. 거기 적힌 값이 검증을 통과하지 못하면
 * 설정을 읽는 도중 앱이 뜨지 못한다. 이름만 맞는지 보는 것으로는 잡히지 않는다.
 */
class OverallRankingGapRecoveryPropertiesTest {

    private static final String PREFIX =
            "maplemetric.internal.ranking.overall-ranking-gap-recovery";

    @Test
    void 선언된기본값은검증을통과한다() throws IOException {
        assertThat(violationsOf(propertiesFrom(declaredDefaults()))).isEmpty();
    }

    /**
     * 예시 파일의 값도 그대로 쓸 수 있어야 한다.
     *
     * 기본값을 좁혀 놓고 예시 파일만 옛 값으로 남겨 두면, 그것을 복사한 환경은 앱이
     * 뜨지 않는다. 잘못을 알아채는 시점이 배포 뒤가 된다.
     */
    @Test
    void 예시파일의값도검증을통과한다() throws IOException {
        assertThat(violationsOf(propertiesFrom(envExampleValues()))).isEmpty();
    }

    private OverallRankingGapRecoveryProperties propertiesFrom(
            Map<String, String> values
    ) {
        return new OverallRankingGapRecoveryProperties(
                Boolean.parseBoolean(values.get("enabled")),
                Integer.parseInt(values.get("lookbackDays")),
                Integer.parseInt(values.get("maxDatesPerRun"))
        );
    }

    private Map<String, String> declaredDefaults() throws IOException {
        PropertySourcesPropertyResolver resolver = declaredSettings();

        Map<String, String> values = new HashMap<>();

        values.put("enabled", resolver.getProperty(PREFIX + ".enabled"));
        values.put(
                "lookbackDays",
                resolver.getProperty(PREFIX + ".lookback-days")
        );
        values.put(
                "maxDatesPerRun",
                resolver.getProperty(PREFIX + ".max-dates-per-run")
        );

        return values;
    }

    private Map<String, String> envExampleValues() throws IOException {
        Map<String, String> variables = new HashMap<>();

        for (String line : Files.readAllLines(
                Path.of(".env.example"),
                StandardCharsets.UTF_8
        )) {
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            int separator = trimmed.indexOf('=');

            if (separator > 0) {
                variables.put(
                        trimmed.substring(0, separator),
                        trimmed.substring(separator + 1)
                );
            }
        }

        Map<String, String> values = new HashMap<>();

        values.put(
                "enabled",
                variables.get("RANKING_GAP_RECOVERY_ENABLED")
        );
        values.put(
                "lookbackDays",
                variables.get("RANKING_GAP_RECOVERY_LOOKBACK_DAYS")
        );
        values.put(
                "maxDatesPerRun",
                variables.get("RANKING_GAP_RECOVERY_MAX_DATES_PER_RUN")
        );

        return values;
    }

    private Set<ConstraintViolation<OverallRankingGapRecoveryProperties>>
            violationsOf(OverallRankingGapRecoveryProperties properties) {
        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            return validator.validate(properties);
        }
    }

    /**
     * 설정 파일이 선언한 기본값을 읽는다.
     *
     * 애플리케이션 문맥을 띄워 읽으면 {@code application.yaml}이 가져오는
     * {@code .env}가 함께 적용돼, 기본값이라고 주장하면서 그 개발자의 환경을 본다.
     */
    private PropertySourcesPropertyResolver declaredSettings()
            throws IOException {
        MutablePropertySources sources = new MutablePropertySources();

        ClassPathResource resource =
                new ClassPathResource("application.yaml");

        for (PropertySource<?> source
                : new YamlPropertySourceLoader()
                        .load("application.yaml", resource)) {
            sources.addLast(source);
        }

        PropertySourcesPropertyResolver resolver =
                new PropertySourcesPropertyResolver(sources);

        resolver.setIgnoreUnresolvableNestedPlaceholders(true);

        return resolver;
    }
}
