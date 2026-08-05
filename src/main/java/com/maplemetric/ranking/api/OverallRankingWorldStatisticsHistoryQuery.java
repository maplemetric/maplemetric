package com.maplemetric.ranking.api;

import java.time.LocalDate;
import java.util.List;

public interface OverallRankingWorldStatisticsHistoryQuery {

    List<OverallRankingWorldStatisticsSnapshot> getWorldStatisticsHistory(
            LocalDate from,
            LocalDate to
    );

    /**
     * 보존된 전체 기간을 조회한다.
     *
     * 시작점은 DB에 남아 있는 최초 성공 Collection이고 끝은 최신 성공 Collection이다.
     * Nexon 제공 시작일이나 이론상 전체 기간을 뜻하지 않는다.
     *
     * 누락된 날짜를 0으로 채우지 않는다. 실제 성공한 기준일만 오름차순으로 담기므로
     * 첫 원소와 마지막 원소의 {@code asOf}가 곧 가용 범위다.
     */
    List<OverallRankingWorldStatisticsSnapshot> getWorldStatisticsAllHistory();
}
