package com.maplemetric.world.application.service;

import com.maplemetric.world.application.port.out.LoadWorldAliasPort;
import com.maplemetric.world.application.port.out.LoadWorldAliasPort.MatchedWorld;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class WorldAliasMatchingService {

    private final LoadWorldAliasPort loadWorldAliasPort;

    WorldAliasMatchingService(LoadWorldAliasPort loadWorldAliasPort) {
        this.loadWorldAliasPort = loadWorldAliasPort;
    }

    @Transactional(readOnly = true)
    public Optional<MatchedWorld> matchWorld(String aliasName) {
        if (aliasName == null || aliasName.isBlank()) {
            log.warn("월드 Alias 매칭 대상 이름이 비어 있습니다.");

            return Optional.empty();
        }

        Optional<MatchedWorld> matched = loadWorldAliasPort
                .findActiveByNormalizedAliasName(normalize(aliasName));

        if (matched.isEmpty()) {
            log.warn(
                    "등록되지 않은 월드 이름입니다. 운영 검수가 필요합니다. 이름={}",
                    aliasName
            );
        }

        return matched;
    }

    private String normalize(String aliasName) {
        return aliasName.trim().toLowerCase(Locale.ROOT);
    }
}
