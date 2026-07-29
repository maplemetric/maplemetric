package com.maplemetric.ranking.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface JobCatalogQuery {

    Optional<CanonicalJob> findBySlug(String jobSlug);

    List<CanonicalJob> findAll();

    /**
     * 원본 직업 이름 집합을 한 번에 Canonical 직업으로 해석한다.
     *
     * Key는 입력한 원본 이름 그대로이며, 여러 원본 이름이 같은 Canonical 직업을
     * 가리키면 같은 값이 여러 Key에 매핑된다. 매칭되지 않은 이름은 Key 자체를 만들지
     * 않으므로 호출자가 입력 집합과 Key 집합의 차이로 미매칭을 구분한다.
     * 미매칭 관측 로그와 Metric은 이 계약이 아니라 수집 경로에서 남긴다.
     */
    Map<String, CanonicalJob> resolveAliases(Collection<String> aliasNames);
}
