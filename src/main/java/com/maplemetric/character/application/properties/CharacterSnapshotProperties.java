package com.maplemetric.character.application.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 캐릭터 저장본 갱신 한도다.
 *
 * 갱신 한 번이 Nexon을 21회 호출한다. 사용자가 연타해도 그만큼씩 소비하지 않도록
 * 최소 간격을 둔다. 간격 안의 갱신 요청은 저장본을 그대로 돌려준다.
 */
@ConfigurationProperties(prefix = "maplemetric.character.snapshot")
public record CharacterSnapshotProperties(
        Duration minRefreshInterval
) {

    public CharacterSnapshotProperties {
        if (minRefreshInterval == null || minRefreshInterval.isNegative()) {
            throw new IllegalArgumentException(
                    "캐릭터 갱신 최소 간격은 0 이상이어야 합니다."
            );
        }
    }
}
