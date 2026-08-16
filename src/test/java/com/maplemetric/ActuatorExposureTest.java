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
     * profile 설정이 기본 설정을 덮도록 Spring과 같은 순서로 쌓는다.
     */
    private PropertySourcesPropertyResolver effective(String... profiles)
            throws IOException {
        MutablePropertySources sources = new MutablePropertySources();

        for (String profile : profiles) {
            addFirst(sources, "application-" + profile + ".yaml");
        }

        addLast(sources, "application.yaml");

        return new PropertySourcesPropertyResolver(sources);
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
