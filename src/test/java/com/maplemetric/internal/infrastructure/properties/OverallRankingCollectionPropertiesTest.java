package com.maplemetric.internal.infrastructure.properties;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class OverallRankingCollectionPropertiesTest {

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
    void 기본maxPages는10이고Scheduler는비활성이며zone은AsiaSeoul이다() {
        new ApplicationContextRunner()
                .withInitializer(
                        new ConfigDataApplicationContextInitializer()
                )
                .withUserConfiguration(PropertiesConfig.class)
                .run(context -> {
                    OverallRankingCollectionProperties collectionProperties =
                            context.getBean(
                                    OverallRankingCollectionProperties.class
                            );
                    OverallRankingCollectionSchedulerProperties
                            schedulerProperties = context.getBean(
                                    OverallRankingCollectionSchedulerProperties.class
                            );

                    assertThat(collectionProperties.maxPages())
                            .isEqualTo(10);
                    assertThat(schedulerProperties.enabled())
                            .isFalse();
                    assertThat(schedulerProperties.zone())
                            .isEqualTo("Asia/Seoul");
                });
    }

    private Validator createValidator() {
        ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory();

        return factory.getValidator();
    }

    @EnableConfigurationProperties({
            OverallRankingCollectionProperties.class,
            OverallRankingCollectionSchedulerProperties.class
    })
    static class PropertiesConfig {
    }
}
