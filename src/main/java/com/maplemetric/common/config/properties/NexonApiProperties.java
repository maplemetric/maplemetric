package com.maplemetric.common.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nexon Open API 접속 설정이다.
 *
 * Key를 여럿 둘 수 있다. Nexon은 초당·일일 한도를 애플리케이션 단위로 세므로,
 * 애플리케이션을 따로 등록해 받은 Key를 함께 쓰면 그만큼 한도가 늘어난다.
 *
 * 한도가 Key마다 따로 붙는다는 것이 요점이다. 하나로 합쳐진 한도를 나눠 쓰는 것이
 * 아니라, 각 Key가 자기 몫을 온전히 가진다.
 */
@Validated
@ConfigurationProperties(prefix = "maplemetric.nexon.open-api")
public record NexonApiProperties(

        @NotBlank(message = "Nexon Open API Base URL은 필수입니다.")
        String baseUrl,

        @NotEmpty(message = "Nexon Open API Key는 하나 이상 필요합니다.")
        List<String> keys
) {

    public NexonApiProperties {
        if (keys != null) {
            keys = keys.stream()
                    .filter(key -> key != null && !key.isBlank())
                    .map(String::trim)
                    .toList();
        }
    }
}
