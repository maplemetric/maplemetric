package com.maplemetric;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

/**
 * Actuator를 인증 없이 여는 범위를 고정한다.
 *
 * 이 앞을 막는 것이 없다. Spring Security 의존성이 없고 내부 인증 필터는 /internal 만
 * 본다. 노출 목록에 무엇을 올리는 것은 곧 인증 없이 공개하는 것이다.
 *
 * 파일을 하나씩 확인하면 "기본이 열고 profile이 안 좁힌" 상태를 잡지 못한다. 실제로
 * 운영 설정이 그 상태였다. 그래서 Spring이 합치는 순서대로 합친 실효값을 본다.
 *
 * 애플리케이션을 띄우면 DB가 필요해 느리다. 설정 파일만 읽어 확인한다.
 */
class ActuatorExposureTest {

    private static final String EXPOSURE =
            "management.endpoints.web.exposure.include";

    private static final String SHOW_DETAILS =
            "management.endpoint.health.show-details";

    @Test
    void 기본은health하나만연다() throws IOException {
        PropertySourcesPropertyResolver base = effective();

        assertThat(base.getProperty(EXPOSURE + "[0]")).isEqualTo("health");
        assertThat(base.getProperty(EXPOSURE + "[1]")).isNull();
    }

    /**
     * profile을 지정하지 않고 떠도 넓게 열리지 않는다.
     *
     * 위의 닫힌 기본값은 그것이 실제로 쓰일 때만 무언가를 지킨다. profile 기본값이
     * 넓히는 profile이면 지정을 잊은 배포가 가장 넓게 열린 설정으로 뜨고, 닫힌
     * 기본값은 한 번도 쓰이지 않는다.
     *
     * 기본값을 아예 두지 않는 것과, 두더라도 닫힌 profile을 두는 것을 모두 통과시킨다.
     * 고정하려는 것은 "무엇이 기본인가"가 아니라 "기본으로 떠도 닫혀 있는가"다.
     */
    @Test
    void profile을지정하지않고떠도넓게열리지않는다() throws IOException {
        String fallbackProfile = profileFallback();

        if (fallbackProfile.isEmpty()) {
            return;
        }

        PropertySourcesPropertyResolver byDefault = effective(fallbackProfile);

        assertThat(byDefault.getProperty(EXPOSURE + "[0]"))
                .as("profile 기본값 %s의 노출", fallbackProfile)
                .isEqualTo("health");
        assertThat(byDefault.getProperty(EXPOSURE + "[1]"))
                .as("profile 기본값 %s의 노출", fallbackProfile)
                .isNull();
    }

    /**
     * 환경변수 없이 떴을 때 켜지는 profile이다.
     *
     * 이 property source에는 환경변수가 없으므로 placeholder가 자기 기본값으로
     * 풀린다. 그렇게 풀린 값이 곧 지정을 잊었을 때 켜지는 profile이다.
     */
    private String profileFallback() throws IOException {
        String fallback = effective().getProperty("spring.profiles.active");

        return fallback == null ? "" : fallback.trim();
    }

    @Test
    void 운영은health하나만연다() throws IOException {
        PropertySourcesPropertyResolver prod = effective("prod");

        assertThat(prod.getProperty(EXPOSURE + "[0]")).isEqualTo("health");
        assertThat(prod.getProperty(EXPOSURE + "[1]")).isNull();
    }

    @Test
    void 운영은상태상세를내보내지않는다() throws IOException {
        assertThat(effective("prod").getProperty(SHOW_DETAILS))
                .isEqualTo("never");
    }

    /**
     * 개발용으로 넓힌 값이 운영으로 새지 않는다.
     *
     * 넓히는 쪽을 local 하나로 두었으므로, 그것이 다른 profile에 영향을 주지 않아야
     * 이 구조가 의미를 가진다.
     */
    @Test
    void 개발용으로넓힌값이운영으로새지않는다() throws IOException {
        assertThat(effective("local").getProperty(EXPOSURE + "[1]"))
                .isNotNull();

        assertThat(effective("prod").getProperty(EXPOSURE + "[1]"))
                .isNull();
    }

    /**
     * 노출 목록에 적은 엔드포인트를 실제로 제공한다.
     *
     * Actuator만으로는 지표를 메모리에 담을 뿐 Prometheus 형식으로 내보내지 못한다.
     * registry 없이 목록에만 적으면 그 경로는 404인데, 설정만 읽는 사람에게는 열려
     * 있는 것으로 보인다. 실제로 운영 노출을 좁히는 작업에서 이것을 열려 있는
     * 엔드포인트로 세었다.
     *
     * 설정과 실제 제공을 따로 보면 이 어긋남을 잡지 못한다. 둘을 잇는다.
     */
    @Test
    void 노출한엔드포인트를실제로제공한다() throws IOException {
        for (String profile : new String[] {"", "local", "prod"}) {
            if (!exposes(profile, "prometheus")) {
                continue;
            }

            assertThat(onClasspath(
                    "io.micrometer.prometheusmetrics.PrometheusMeterRegistry"
            ))
                    .as("%s 노출에 prometheus가 있으나 registry가 없다", profile)
                    .isTrue();
        }
    }

    private boolean exposes(String profile, String endpoint)
            throws IOException {
        PropertySourcesPropertyResolver resolver = profile.isEmpty()
                ? effective()
                : effective(profile);

        for (int index = 0; ; index++) {
            String value =
                    resolver.getProperty(EXPOSURE + "[" + index + "]");

            if (value == null) {
                return false;
            }

            if (endpoint.equals(value)) {
                return true;
            }
        }
    }

    private boolean onClasspath(String className) {
        try {
            Class.forName(className, false, getClass().getClassLoader());

            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    /**
     * profile 설정이 기본 설정을 덮도록 Spring과 같은 순서로 쌓는다.
     */
    private PropertySourcesPropertyResolver effective(String... profiles)
            throws IOException {
        MutablePropertySources sources = new MutablePropertySources();

        for (String profile : profiles) {
            addFirst(sources, "application-" + profile + ".yaml");
        }

        addLast(sources, "application.yaml");

        PropertySourcesPropertyResolver resolver =
                new PropertySourcesPropertyResolver(sources);

        // 환경변수는 여기 없다. 풀리지 않는 placeholder는 적힌 그대로 두어야
        // 기본값을 읽을 수 있다.
        resolver.setIgnoreUnresolvableNestedPlaceholders(true);

        return resolver;
    }

    private void addFirst(MutablePropertySources sources, String fileName)
            throws IOException {
        for (PropertySource<?> source : load(fileName)) {
            sources.addFirst(source);
        }
    }

    private void addLast(MutablePropertySources sources, String fileName)
            throws IOException {
        for (PropertySource<?> source : load(fileName)) {
            sources.addLast(source);
        }
    }

    private List<PropertySource<?>> load(String fileName) throws IOException {
        ClassPathResource resource = new ClassPathResource(fileName);

        assertThat(resource.exists())
                .as("설정 파일 %s", fileName)
                .isTrue();

        return new YamlPropertySourceLoader().load(fileName, resource);
    }
}
