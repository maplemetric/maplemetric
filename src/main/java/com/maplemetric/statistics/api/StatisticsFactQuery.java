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
