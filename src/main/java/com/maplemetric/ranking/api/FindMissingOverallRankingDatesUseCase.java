package com.maplemetric.ranking.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 아직 수집하지 못한 기준일을 찾는다.
 *
 * 정기 수집은 앱이 떠 있는 동안에만 돈다. 앱이 꺼져 있던 날은 그대로 빈다. 무엇이
 * 비었는지 알아야 메울 수도 있고, 며칠이나 비었는지 밖으로 알릴 수도 있다.
 *
 * 외부는 랭킹 이력을 정해진 기간만 제공하므로, 그 기간을 넘긴 빈 날은 찾아 봐야
 * 채울 수 없다. 어느 기간을 볼지는 부르는 쪽이 정한다.
 */
public interface FindMissingOverallRankingDatesUseCase {

    /**
     * 기간 안에서 수집되지 않은 기준일을 오름차순으로 돌려준다.
     *
     * 양 끝을 포함한다. 오래된 것이 앞에 온다. 채울 수 있는 기간이 정해져 있으므로
     * 오래된 것부터 메워야 먼저 사라질 날을 먼저 건진다.
     */
    List<LocalDate> findMissingDates(LocalDate from, LocalDate to);

    /**
     * 마지막으로 수집한 기준일이다. 한 번도 수집하지 않았으면 비어 있다.
     *
     * 빈 날 목록에서 거꾸로 세는 것으로는 이 값을 얻을 수 없다. 되짚는 기간 밖은
     * 목록에 없어서, 아주 오래 멈춰 있어도 그 기간만큼만 밀린 것으로 보인다.
     */
    Optional<LocalDate> findLatestCollectedDate();
}
