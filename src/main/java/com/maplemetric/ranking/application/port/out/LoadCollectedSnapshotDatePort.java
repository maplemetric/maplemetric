package com.maplemetric.ranking.application.port.out;

import java.time.LocalDate;
import java.util.List;

/**
 * 이미 수집한 기준일을 읽는다.
 *
 * 비어 있는 날을 저장소가 만들어 내게 하지 않는다. 날짜를 만들어 내려면 저장소마다
 * 다른 함수를 써야 하고, 기간도 길어야 몇 년이라 채워진 날 목록이 크지 않다.
 */
public interface LoadCollectedSnapshotDatePort {

    /** 기간 안에서 이미 수집한 기준일을 오름차순으로 돌려준다. 양 끝을 포함한다. */
    List<LocalDate> loadCollectedDates(LocalDate from, LocalDate to);
}
