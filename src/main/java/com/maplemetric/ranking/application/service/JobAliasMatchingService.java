package com.maplemetric.ranking.application.service;

import com.maplemetric.ranking.application.port.out.LoadJobAliasPort;
import com.maplemetric.ranking.application.port.out.LoadJobAliasPort.MatchedJob;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class JobAliasMatchingService {

    private final LoadJobAliasPort loadJobAliasPort;

    JobAliasMatchingService(LoadJobAliasPort loadJobAliasPort) {
        this.loadJobAliasPort = loadJobAliasPort;
    }

    @Transactional(readOnly = true)
    public Optional<MatchedJob> matchJob(String aliasName) {
        if (aliasName == null || aliasName.isBlank()) {
            log.warn("직업 Alias 매칭 대상 이름이 비어 있습니다.");

            return Optional.empty();
        }

        Optional<MatchedJob> matched = loadJobAliasPort
                .findActiveByNormalizedAliasName(normalize(aliasName));

        if (matched.isEmpty()) {
            log.warn(
                    "등록되지 않은 직업 이름입니다. 운영 검수가 필요합니다. 이름={}",
                    aliasName
            );
        }

        return matched;
    }

    @Transactional(readOnly = true)
    public boolean matches(String aliasName) {
        if (aliasName == null || aliasName.isBlank()) {
            return false;
        }

        return loadJobAliasPort
                .findActiveByNormalizedAliasName(normalize(aliasName))
                .isPresent();
    }

    private String normalize(String aliasName) {
        return aliasName.trim().toLowerCase(Locale.ROOT);
    }
}
