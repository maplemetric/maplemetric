package com.maplemetric.ranking.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 이미 수집한 기준일을 읽는다.
 *
 * 비어 있는 날을 저장소가 만들어 내게 하지 않는다. 날짜를 만들어 내려면 저장소마다
 * 다른 함수를 써야 하고, 기간도 길어야 몇 년이라 채워진 날 목록이 크지 않다.
 */
public interface LoadCollectedSnapshotDatePort {

    /** 기간 안에서 이미 수집한 기준일을 오름차순으로 돌려준다. 양 끝을 포함한다. */
    List<LocalDate> loadCollectedDates(LocalDate from, LocalDate to);

    /**
     * 수집한 가장 오래된 기준일이다. 한 번도 수집하지 않았으면 비어 있다.
     *
     * 이 날 이전은 수집을 시작하기 전이라 비어 있는 것이 정상이다.
     */
    Optional<LocalDate> loadEarliestCollectedDate();

    /**
     * 수집한 가장 최근 기준일이다. 한 번도 수집하지 않았으면 비어 있다.
     *
     * 마지막으로 언제 받았는지를 이 값으로 답한다.
     */
    Optional<LocalDate> loadLatestCollectedDate();
}
