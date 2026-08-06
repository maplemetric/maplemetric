package com.maplemetric.analysis.api;

/**
 * 통계 설명을 미리 만들어 저장한다.
 *
 * 조회 시점에 만들지 않는다. OpenAI 예산이 정해져 있어 트래픽이 곧 비용이 되면
 * 상한을 넘는 순간 기능이 멈추고, 첫 방문자만 느려지는 지연도 생긴다.
 *
 * 스스로 실행되지 않는다. Scheduler를 두지 않았으므로 승인된 시점에만 호출한다.
 */
public interface GenerateStatisticsInsightUseCase {

    /**
     * 아직 설명이 없는 대상을 생성한다.
     *
     * @return 생성·건너뜀·실패 건수
     */
    GenerateStatisticsInsightOutcome generate();
}
