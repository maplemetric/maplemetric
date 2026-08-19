package com.maplemetric.common.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Key 설정을 어떻게 받아들이는지 고정한다.
 *
 * 한도가 Key마다 따로 붙으므로, 여기서 몇 개로 보느냐가 곧 관문이 내보내는 양이
 * 된다. 잘못 세면 실제 한도를 넘겨 429를 부른다.
 */
class NexonApiPropertiesTest {

    private static final String BASE_URL = "https://open.api.nexon.com";

    /**
     * Key 하나만 주던 설정을 그대로 받는다.
     *
     * 복수형으로 옮기면서 단수형을 끊으면 기존 환경이 뜨지 않는다.
     */
    @Test
    void 단수형만주어도Key로쓴다() {
        NexonApiProperties properties =
                new NexonApiProperties(BASE_URL, List.of(), "single");

        assertThat(properties.keys()).containsExactly("single");
    }

    /**
     * 복수형이 비어 있어도 단수형을 쓴다.
     *
     * 환경 파일에 이름만 적고 값을 비워 두는 일이 흔하다. 그때 정의됐다는 이유로
     * 단수형을 무시하면 Key가 하나도 없는 채로 뜬다.
     */
    @Test
    void 복수형이비어있으면단수형을쓴다() {
        NexonApiProperties properties = new NexonApiProperties(
                BASE_URL,
                Arrays.asList("", "   "),
                "single"
        );

        assertThat(properties.keys()).containsExactly("single");
    }

    /**
     * 복수형이 있으면 그것을 쓴다.
     */
    @Test
    void 복수형이있으면그것을쓴다() {
        NexonApiProperties properties = new NexonApiProperties(
                BASE_URL,
                List.of("a", "b"),
                "single"
        );

        assertThat(properties.keys()).containsExactly("a", "b");
    }

    /**
     * 같은 Key를 여러 번 적으면 하나로 본다.
     *
     * 그러지 않으면 관문이 같은 Key로 창을 여러 개 만들어 실제 한도보다 그 배수만큼
     * 많이 내보낸다. 429가 돌아오고 그 응답도 호출로 집계된다.
     */
    @Test
    void 같은Key를여러번적어도하나로본다() {
        NexonApiProperties properties = new NexonApiProperties(
                BASE_URL,
                List.of("same", " same ", "other"),
                null
        );

        assertThat(properties.keys()).containsExactly("same", "other");
    }
}
