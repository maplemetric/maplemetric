package com.maplemetric.common.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nexon Open API 접속 설정이다.
 *
 * Key를 여럿 둘 수 있다. Nexon은 초당·일일 한도를 애플리케이션 단위로 세므로,
 * 애플리케이션을 따로 등록해 받은 Key를 함께 쓰면 그만큼 한도가 늘어난다. 한도가
 * Key마다 따로 붙는다는 것이 요점이다.
 *
 * {@code key} 하나만 주던 설정도 그대로 받는다. 값을 채우는 방식이 환경마다 달라
 * 빈 문자열이 들어오는 경우가 있으므로, 비어 있으면 없는 것으로 보고 단수형을 쓴다.
 *
 * 같은 Key를 여러 번 적으면 하나로 본다. 그러지 않으면 관문이 Key 수만큼 창을
 * 만들어 실제 한도보다 많이 내보낸다.
 */
@Validated
@ConfigurationProperties(prefix = "maplemetric.nexon.open-api")
public record NexonApiProperties(

        @NotBlank(message = "Nexon Open API Base URL은 필수입니다.")
        String baseUrl,

        @NotEmpty(message = "Nexon Open API Key는 하나 이상 필요합니다.")
        List<String> keys,

        String key
) {

    public NexonApiProperties {
        List<String> normalized = normalize(keys);

        if (normalized.isEmpty() && key != null && !key.isBlank()) {
            normalized = List.of(key.trim());
        }

        keys = normalized;
    }

    private static List<String> normalize(List<String> keys) {
        if (keys == null) {
            return List.of();
        }

        LinkedHashSet<String> unique = new LinkedHashSet<>();

        for (String candidate : keys) {
            if (candidate != null && !candidate.isBlank()) {
                unique.add(candidate.trim());
            }
        }

        return List.copyOf(new ArrayList<>(unique));
    }
}
