package com.maplemetric.statistics.api;

import java.time.LocalDate;

/**
 * 검증된 통계 사실을 모듈 밖에 제공한다.
 *
 * HTTP Response와 Persistence 타입은 넘기지 않는다. 소비자가 그것들에 묶이면 응답
 * 형식을 바꿀 때마다 소비자가 함께 깨진다.
 *
 * 대상을 찾지 못하거나 수집 데이터가 잘못되면 Statistics의 기존 예외를 그대로
 * 올린다. Fact 계층이 예외를 새로 정의하지 않는다.
 */
public interface StatisticsFactQuery {

    /**
     * 통계를 낼 수 있는 대상을 모두 돌려준다.
     *
     * 소비자가 직업·월드 Catalog를 각각 알 필요 없이 이 계약 하나로 순회할 수 있다.
     * 순서는 직업 다음 월드이며 각 그룹 안에서는 Catalog 순서를 따른다.
     */
    java.util.List<StatisticsSubject> listSubjects();

    StatisticsDetailFact getJobDetailFact(String jobSlug);

    StatisticsDetailFact getWorldDetailFact(String worldSlug);

    StatisticsHistoryFact getJobHistoryFact(
            String jobSlug,
            String range,
            LocalDate from,
            LocalDate to
    );

    StatisticsHistoryFact getWorldHistoryFact(
            String worldSlug,
            String range,
            LocalDate from,
            LocalDate to
    );
}
