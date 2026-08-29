package com.maplemetric.ranking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.maplemetric.ranking.application.port.out.LoadCollectedSnapshotDatePort;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 수집되지 않은 기준일을 계산하는지 확인한다.
 *
 * 이 결과가 메우기의 대상이 되고 밖으로 알리는 누락 일수가 된다. 하나라도 빠뜨리면
 * 그 기준일은 아무도 채우지 않은 채 제공 기간을 넘긴다.
 */
class MissingOverallRankingDateServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 7, 1);

    private static final LocalDate TO = LocalDate.of(2026, 7, 5);

    private final LoadCollectedSnapshotDatePort loadCollectedDatePort =
            mock(LoadCollectedSnapshotDatePort.class);

    private final MissingOverallRankingDateService service =
            new MissingOverallRankingDateService(loadCollectedDatePort);

    @Test
    void 채워지지않은기준일만오름차순으로돌려준다() {
        givenCollected(FROM, FROM.plusDays(2));

        assertThat(service.findMissingDates(FROM, TO)).containsExactly(
                FROM.plusDays(1),
                FROM.plusDays(3),
                FROM.plusDays(4)
        );
    }

    /** 양 끝을 포함한다. 하루짜리 기간도 물어볼 수 있어야 한다. */
    @Test
    void 기간의양끝을포함한다() {
        givenCollected();

        assertThat(service.findMissingDates(FROM, FROM))
                .containsExactly(FROM);
    }

    @Test
    void 모두채워져있으면비어있는날이없다() {
        givenCollected(
                FROM,
                FROM.plusDays(1),
                FROM.plusDays(2),
                FROM.plusDays(3),
                FROM.plusDays(4)
        );

        assertThat(service.findMissingDates(FROM, TO)).isEmpty();
    }

    /**
     * 기간 밖의 수집은 계산에 끼어들지 않는다.
     *
     * 저장소가 기간 밖 날짜를 돌려주더라도 그 날이 기간 안의 빈 날을 채운 것처럼
     * 보이면 안 된다.
     */
    @Test
    void 기간밖의수집은계산을바꾸지않는다() {
        givenCollected(FROM.minusDays(1), TO.plusDays(1));

        assertThat(service.findMissingDates(FROM, TO)).hasSize(5);
    }

    @Test
    void 시작이끝보다뒤면계산하지않는다() {
        assertThatThrownBy(() -> service.findMissingDates(TO, FROM))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 기간이비어있으면계산하지않는다() {
        assertThatThrownBy(() -> service.findMissingDates(null, TO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.findMissingDates(FROM, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void givenCollected(LocalDate... dates) {
        given(loadCollectedDatePort.loadCollectedDates(any(), any()))
                .willReturn(List.of(dates));
    }
}
