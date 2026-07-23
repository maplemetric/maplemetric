package com.maplemetric.ranking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.maplemetric.ranking.application.port.out.LoadRankingListPort;
import com.maplemetric.ranking.application.result.GetDojangRankingResult;
import com.maplemetric.ranking.application.result.GetOverallRankingResult;
import com.maplemetric.ranking.application.result.GetUnionRankingResult;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RankingQueryServiceTest {

    private static final LocalDate RANKING_DATE =
            LocalDate.of(2026, 7, 19);

    @Mock
    private LoadRankingListPort loadRankingListPort;

    @Test
    void 명시한기준일을랭킹세요청에그대로전달한다() {
        RankingQueryService service = createService(
                "2026-07-21T00:30:00Z"
        );

        given(loadRankingListPort.loadOverallRanking(
                RANKING_DATE,
                "루나",
                null,
                "팬텀-전체 전직",
                1
        )).willReturn(emptyOverallRanking());

        given(loadRankingListPort.loadUnionRanking(
                RANKING_DATE,
                "루나",
                1
        )).willReturn(emptyUnionRanking());

        given(loadRankingListPort.loadDojangRanking(
                RANKING_DATE,
                "루나",
                1,
                "팬텀-전체 전직",
                1
        )).willReturn(emptyDojangRanking());

        service.getOverallRanking(
                RANKING_DATE,
                "루나",
                null,
                "팬텀-전체 전직",
                1
        );

        service.getUnionRanking(
                RANKING_DATE,
                "루나",
                1
        );

        service.getDojangRanking(
                RANKING_DATE,
                "루나",
                1,
                "팬텀-전체 전직",
                1
        );

        verify(loadRankingListPort).loadOverallRanking(
                RANKING_DATE,
                "루나",
                null,
                "팬텀-전체 전직",
                1
        );

        verify(loadRankingListPort).loadUnionRanking(
                RANKING_DATE,
                "루나",
                1
        );

        verify(loadRankingListPort).loadDojangRanking(
                RANKING_DATE,
                "루나",
                1,
                "팬텀-전체 전직",
                1
        );
    }

    @Test
    void 기준일생략시KST오전9시29분이면전일을사용한다() {
        RankingQueryService service = createService(
                "2026-07-21T00:29:00Z"
        );

        LocalDate expectedDate =
                LocalDate.of(2026, 7, 20);

        given(loadRankingListPort.loadOverallRanking(
                expectedDate,
                null,
                null,
                null,
                1
        )).willReturn(emptyOverallRanking());

        service.getOverallRanking(
                null,
                null,
                null,
                null,
                1
        );

        verify(loadRankingListPort).loadOverallRanking(
                expectedDate,
                null,
                null,
                null,
                1
        );
    }

    @Test
    void 기준일생략시KST오전9시30분이면당일을사용한다() {
        RankingQueryService service = createService(
                "2026-07-21T00:30:00Z"
        );

        LocalDate expectedDate =
                LocalDate.of(2026, 7, 21);

        given(loadRankingListPort.loadUnionRanking(
                expectedDate,
                null,
                1
        )).willReturn(emptyUnionRanking());

        service.getUnionRanking(
                null,
                null,
                1
        );

        verify(loadRankingListPort).loadUnionRanking(
                expectedDate,
                null,
                1
        );
    }

    @Test
    void OutputPort의조회결과를그대로반환한다() {
        RankingQueryService service = createService(
                "2026-07-21T00:30:00Z"
        );

        GetOverallRankingResult expected =
                GetOverallRankingResult.of(
                        List.of(),
                        2,
                        RANKING_DATE,
                        "NEXON_OPEN_API"
                );

        given(loadRankingListPort.loadOverallRanking(
                RANKING_DATE,
                null,
                0,
                null,
                2
        )).willReturn(expected);

        GetOverallRankingResult result =
                service.getOverallRanking(
                        RANKING_DATE,
                        null,
                        0,
                        null,
                        2
                );

        assertThat(result).isSameAs(expected);
    }

    private RankingQueryService createService(
            String instant
    ) {
        return new RankingQueryService(
                loadRankingListPort,
                Clock.fixed(
                        Instant.parse(instant),
                        ZoneId.of("Asia/Seoul")
                )
        );
    }

    private GetOverallRankingResult emptyOverallRanking() {
        return GetOverallRankingResult.of(
                List.of(),
                1,
                RANKING_DATE,
                "NEXON_OPEN_API"
        );
    }

    private GetUnionRankingResult emptyUnionRanking() {
        return GetUnionRankingResult.of(
                List.of(),
                1,
                RANKING_DATE,
                "NEXON_OPEN_API"
        );
    }

    private GetDojangRankingResult emptyDojangRanking() {
        return GetDojangRankingResult.of(
                List.of(),
                1,
                RANKING_DATE,
                "NEXON_OPEN_API"
        );
    }
}
