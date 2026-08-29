package com.maplemetric.internal.application.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

/**
 * 수집 결과를 자동으로 지우지 않는다는 결정을 설정에 고정한다.
 *
 * Nexon은 랭킹 이력을 2년만 준다. 2년 밖으로 나간 기준일을 지우면 다시 받을 수
 * 없고, 통계 이력의 전체 기간 조회는 남아 있는 전부를 보므로 지우는 만큼 그 지표의
 * 시작점이 앞으로 밀린다. 실수로 켜지거나 보존 일수가 줄어드는 변경을 여기서 막는다.
 */
class OverallRankingRetentionPropertiesTest {

    private static final String PREFIX =
            "maplemetric.internal.ranking.overall-ranking-retention";

    /** Nexon이 랭킹 이력을 주는 기간이다. */
    private static final int NEXON_HISTORY_DAYS = 730;

    @Test
    void 자동삭제는꺼진채로둔다() throws IOException {
        assertThat(declaredSettings().getProperty(PREFIX + ".enabled"))
                .isEqualTo("false");
    }

    /**
     * 보존 일수는 Nexon이 주는 2년보다 넉넉해야 한다.
     *
     * 2년보다 짧게 잡으면 켜는 순간 다시 받을 수 없는 기준일부터 지운다. 3650은
     * 삭제를 시작하겠다는 뜻이 아니라 10년 뒤에 다시 판단하기 위한 상한이다.
     *
     * 오늘 날짜로 "지금은 지울 대상이 없다"를 확인하지 않는다. 그것은 언젠가 거짓이
     * 되는 것이 정상인 사실이라, 검사로 두면 정책이 그대로인데도 어느 날 빌드가
     * 깨진다. 시간이 지나도 변하지 않아야 하는 것은 2년 창과의 관계다.
     */
    @Test
    void 보존일수는다시받을수없는구간을지우지않는다() throws IOException {
        String declared =
                declaredSettings().getProperty(PREFIX + ".retention-days");

        assertThat(declared).isEqualTo("3650");
        assertThat(Integer.parseInt(declared))
                .isGreaterThan(NEXON_HISTORY_DAYS);
    }

    /**
     * 설정 파일이 선언한 기본값을 읽는다.
     *
     * 애플리케이션 문맥을 띄워 읽으면 {@code application.yaml}이 가져오는
     * {@code .env}가 함께 적용된다. 그러면 "기본값이 이것이다"라고 주장하면서 실제로는
     * 그 개발자의 환경을 본다.
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
}
