package com.maplemetric.ranking.application.service;

import com.maplemetric.ranking.api.FindMissingOverallRankingDatesUseCase;
import com.maplemetric.ranking.application.port.out.LoadCollectedSnapshotDatePort;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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

    /**
     * 수집을 시작한 뒤로 비어 있는 기준일을 찾는다.
     *
     * 첫 수집보다 앞은 세지 않는다. 그 기간은 빠진 것이 아니라 애초에 받은 적이 없는
     * 기간이다. 그것까지 비었다고 세면, 서비스를 막 시작했거나 오래된 기준일이 보관
     * 기간을 넘겨 사라진 뒤에 지난 몇 년이 통째로 대상이 된다. 그러면 정작 최근에
     * 빠진 날이 뒤로 밀린다.
     *
     * 한 번도 수집하지 않았으면 비어 있는 날도 없다고 본다. 채울 기준이 없다.
     */
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

        Optional<LocalDate> earliestCollected =
                loadCollectedSnapshotDatePort.loadEarliestCollectedDate();

        if (earliestCollected.isEmpty()) {
            return List.of();
        }

        LocalDate start = earliestCollected.get().isAfter(from)
                ? earliestCollected.get()
                : from;

        if (start.isAfter(to)) {
            return List.of();
        }

        Set<LocalDate> collected = new HashSet<>(
                loadCollectedSnapshotDatePort.loadCollectedDates(start, to)
        );

        List<LocalDate> missing = new ArrayList<>();

        for (
                LocalDate date = start;
                !date.isAfter(to);
                date = date.plusDays(1)
        ) {
            if (!collected.contains(date)) {
                missing.add(date);
            }
        }

        return List.copyOf(missing);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDate> findLatestCollectedDate() {
        return loadCollectedSnapshotDatePort.loadLatestCollectedDate();
    }
}
