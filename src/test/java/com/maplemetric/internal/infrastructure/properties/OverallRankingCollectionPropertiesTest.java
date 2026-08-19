package com.maplemetric.internal.infrastructure.properties;

import static org.assertj.core.api.Assertions.assertThat;

import com.maplemetric.internal.application.properties.OverallRankingCollectionProperties;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.IOException;
import java.util.Set;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;
import org.junit.jupiter.api.Test;

class OverallRankingCollectionPropertiesTest {

    private static final String PREFIX =
            "maplemetric.internal.ranking.overall-ranking-collection";

    private final Validator validator = createValidator();

    @Test
    void maxPages는1을허용한다() {
        Set<ConstraintViolation<OverallRankingCollectionProperties>> violations =
                validator.validate(
                        new OverallRankingCollectionProperties(1)
                );

        assertThat(violations).isEmpty();
    }

    @Test
    void maxPages는100을허용한다() {
        Set<ConstraintViolation<OverallRankingCollectionProperties>> violations =
                validator.validate(
                        new OverallRankingCollectionProperties(100)
                );

        assertThat(violations).isEmpty();
    }

    @Test
    void maxPages는0을거부한다() {
        Set<ConstraintViolation<OverallRankingCollectionProperties>> violations =
                validator.validate(
                        new OverallRankingCollectionProperties(0)
                );

        assertThat(violations).isNotEmpty();
    }

    @Test
    void maxPages는101을거부한다() {
        Set<ConstraintViolation<OverallRankingCollectionProperties>> violations =
                validator.validate(
                        new OverallRankingCollectionProperties(101)
                );

        assertThat(violations).isNotEmpty();
    }

    @Test
    void 기본maxPages는10이고Scheduler는비활성이며zone은AsiaSeoul이다()
            throws IOException {
        PropertySourcesPropertyResolver declared = declaredSettings();

        assertThat(declared.getProperty(PREFIX + ".max-pages"))
                .isEqualTo("10");
        assertThat(declared.getProperty(PREFIX + ".scheduler.enabled"))
                .isEqualTo("false");
        assertThat(declared.getProperty(PREFIX + ".scheduler.zone"))
                .isEqualTo("Asia/Seoul");
    }

    /**
     * 설정 파일이 선언한 기본값을 읽는다.
     *
     * 애플리케이션 문맥을 띄워 읽으면 {@code application.yaml}이 가져오는 {@code .env}가
     * 함께 적용된다. 그러면 "기본값이 이것이다"라고 주장하면서 실제로는 그 개발자의
     * 환경을 본다. 실제로 수집 Scheduler를 켠 환경에서 이 검증이 깨졌다.
     *
     * 환경변수가 없는 property source에서는 placeholder가 자기 기본값으로 풀린다.
     * 그렇게 풀린 값이 곧 설정 파일이 선언한 기본값이다.
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

    private Validator createValidator() {
        ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory();

        return factory.getValidator();
    }
}
