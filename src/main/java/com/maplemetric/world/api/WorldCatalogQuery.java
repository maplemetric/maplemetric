package com.maplemetric.world.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface WorldCatalogQuery {

    Optional<CanonicalWorld> findBySlug(String worldSlug);

    List<CanonicalWorld> findAll();

    /**
     * 원본 월드 이름 집합을 한 번에 Canonical 월드로 해석한다.
     *
     * <p>Key는 입력한 원본 이름 그대로이며, 여러 원본 이름이 같은 Canonical 월드를
     * 가리키면 같은 값이 여러 Key에 매핑된다. 매칭되지 않은 이름은 Key 자체를 만들지
     * 않으므로 호출자가 입력 집합과 Key 집합의 차이로 미매칭을 구분한다.
     * 미매칭 관측 로그와 Metric은 이 계약이 아니라 수집 경로에서 남긴다.
     */
    Map<String, CanonicalWorld> resolveAliases(Collection<String> aliasNames);
}
