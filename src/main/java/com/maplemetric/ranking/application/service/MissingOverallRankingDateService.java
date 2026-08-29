package com.maplemetric.ranking.application.service;

import com.maplemetric.ranking.api.FindMissingOverallRankingDatesUseCase;
import com.maplemetric.ranking.application.port.out.LoadCollectedSnapshotDatePort;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수집되지 않은 기준일을 계산한다.
 *
 * 채워진 날을 받아 기간에서 빼는 방식이다. 저장소에 날짜를 만들어 내게 하는 것보다
 * 옮기기 쉽고, 무엇을 빼는지가 코드에 그대로 보인다.
 */
@Service
public class MissingOverallRankingDateService
        implements FindMissingOverallRankingDatesUseCase {

    private final LoadCollectedSnapshotDatePort loadCollectedSnapshotDatePort;

    public MissingOverallRankingDateService(
            LoadCollectedSnapshotDatePort loadCollectedSnapshotDatePort
    ) {
        this.loadCollectedSnapshotDatePort = loadCollectedSnapshotDatePort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDate> findMissingDates(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "기간의 양 끝은 비어 있을 수 없습니다."
            );
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "기간의 시작은 끝보다 뒤일 수 없습니다."
            );
        }

        Set<LocalDate> collected = new HashSet<>(
                loadCollectedSnapshotDatePort.loadCollectedDates(from, to)
        );

        List<LocalDate> missing = new ArrayList<>();

        for (
                LocalDate date = from;
                !date.isAfter(to);
                date = date.plusDays(1)
        ) {
            if (!collected.contains(date)) {
                missing.add(date);
            }
        }

        return List.copyOf(missing);
    }
}
