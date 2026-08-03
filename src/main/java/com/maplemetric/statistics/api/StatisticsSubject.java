package com.maplemetric.statistics.api;

/**
 * 통계의 대상이다.
 *
 * 직업과 월드를 한 타입으로 다룬다. 소비자 입장에서 둘은 "무엇의 비율이 얼마나
 * 변했다"는 같은 모양이고, 대상을 구분해야 할 때는 {@code type}을 본다.
 *
 * 표시 순서·운영 상태·직업군 같은 목록 표현용 속성은 담지 않는다. 그것들은 Catalog가
 * 소유하며, 통계 사실이 아니라 화면 배치 정보다.
 */
public record StatisticsSubject(
        StatisticsSubjectType type,
        String slug,
        String name
) {
}
