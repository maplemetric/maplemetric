package com.maplemetric.ranking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.maplemetric.common.nexon.NexonApiFailure;
import com.maplemetric.ranking.application.result.GetDojangRankingResult;
import com.maplemetric.ranking.application.result.GetOverallRankingResult;
import com.maplemetric.ranking.application.result.GetUnionRankingResult;
import com.maplemetric.ranking.domain.exception.RankingException;
import com.maplemetric.ranking.infrastructure.client.nexon.RankingClient;
import com.maplemetric.ranking.infrastructure.client.nexon.response.DojangRankingResponse;
import com.maplemetric.ranking.infrastructure.client.nexon.response.OverallRankingResponse;
import com.maplemetric.ranking.infrastructure.client.nexon.response.UnionRankingResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
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
    private RankingClient rankingClient;

    @Test
    void 명시한기준일을랭킹세요청에그대로전달한다() {
        RankingQueryService service = createService(
                "2026-07-21T00:30:00Z"
        );

        given(rankingClient.getOverallRanking(
                RANKING_DATE,
                "루나",
                null,
                "팬텀-전체 전직",
                1
        )).willReturn(emptyOverallRanking());

        given(rankingClient.getUnionRanking(
                RANKING_DATE,
                "루나",
                1
        )).willReturn(emptyUnionRanking());

        given(rankingClient.getDojangRanking(
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

        verify(rankingClient).getOverallRanking(
                RANKING_DATE,
                "루나",
                null,
                "팬텀-전체 전직",
                1
        );

        verify(rankingClient).getUnionRanking(
                RANKING_DATE,
                "루나",
                1
        );

        verify(rankingClient).getDojangRanking(
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

        given(rankingClient.getOverallRanking(
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

        verify(rankingClient).getOverallRanking(
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

        given(rankingClient.getUnionRanking(
                expectedDate,
                null,
                1
        )).willReturn(emptyUnionRanking());

        service.getUnionRanking(
                null,
                null,
                1
        );

        verify(rankingClient).getUnionRanking(
                expectedDate,
                null,
                1
        );
    }

    @Test
    void 종합랭킹필드와응답기준일을매핑한다() {
        RankingQueryService service = createService(
                "2026-07-21T00:30:00Z"
        );

        OverallRankingResponse response =
                new OverallRankingResponse(
                        List.of(
                                new OverallRankingResponse.Ranking(
                                        "2026-07-18",
                                        1,
                                        "감점",
                                        "루나",
                                        "팬텀",
                                        "",
                                        290,
                                        1_234_567L,
                                        321,
                                        "메이플"
                                )
                        )
                );

        given(rankingClient.getOverallRanking(
                RANKING_DATE,
                null,
                0,
                null,
                2
        )).willReturn(response);

        GetOverallRankingResult result =
                service.getOverallRanking(
                        RANKING_DATE,
                        null,
                        0,
                        null,
                        2
                );

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.asOf())
                .isEqualTo(LocalDate.of(2026, 7, 18));
        assertThat(result.source())
                .isEqualTo("NEXON_OPEN_API");
        assertThat(result.ranking())
                .extracting(
                        ranking -> ranking.ranking(),
                        ranking -> ranking.characterName(),
                        ranking -> ranking.characterExp(),
                        ranking -> ranking.characterGuildName()
                )
                .containsExactly(
                        tuple(
                                1,
                                "감점",
                                1_234_567L,
                                "메이플"
                        )
                );
    }

    @Test
    void 유니온과무릉도장필드를매핑한다() {
        RankingQueryService service = createService(
                "2026-07-21T00:30:00Z"
        );

        given(rankingClient.getUnionRanking(
                RANKING_DATE,
                null,
                1
        )).willReturn(
                new UnionRankingResponse(
                        List.of(
                                new UnionRankingResponse.Ranking(
                                        "2026-07-19",
                                        10,
                                        "감점",
                                        "루나",
                                        "팬텀",
                                        "",
                                        9000,
                                        123_456_789L
                                )
                        )
                )
        );

        given(rankingClient.getDojangRanking(
                RANKING_DATE,
                null,
                0,
                null,
                1
        )).willReturn(
                new DojangRankingResponse(
                        List.of(
                                new DojangRankingResponse.Ranking(
                                        "2026-07-19",
                                        20,
                                        "감점",
                                        "루나",
                                        "팬텀",
                                        "",
                                        290,
                                        80,
                                        600
                                )
                        )
                )
        );

        GetUnionRankingResult unionResult =
                service.getUnionRanking(
                        RANKING_DATE,
                        null,
                        1
                );

        GetDojangRankingResult dojangResult =
                service.getDojangRanking(
                        RANKING_DATE,
                        null,
                        0,
                        null,
                        1
                );

        assertThat(unionResult.ranking())
                .singleElement()
                .satisfies(ranking -> {
                    assertThat(ranking.unionLevel())
                            .isEqualTo(9000);
                    assertThat(ranking.unionPower())
                            .isEqualTo(123_456_789L);
                });

        assertThat(dojangResult.ranking())
                .singleElement()
                .satisfies(ranking -> {
                    assertThat(ranking.dojangFloor())
                            .isEqualTo(80);
                    assertThat(ranking.dojangTimeRecord())
                            .isEqualTo(600);
                });
    }

    @Test
    void 빈랭킹목록은요청기준일과함께반환한다() {
        RankingQueryService service = createService(
                "2026-07-21T00:30:00Z"
        );

        given(rankingClient.getDojangRanking(
                RANKING_DATE,
                null,
                0,
                null,
                3
        )).willReturn(emptyDojangRanking());

        GetDojangRankingResult result =
                service.getDojangRanking(
                        RANKING_DATE,
                        null,
                        0,
                        null,
                        3
                );

        assertThat(result.ranking()).isEmpty();
        assertThat(result.page()).isEqualTo(3);
        assertThat(result.asOf()).isEqualTo(RANKING_DATE);
    }

    @Test
    void 랭킹항목의날짜가올바르지않으면응답오류를반환한다() {
        RankingQueryService service = createService(
                "2026-07-21T00:30:00Z"
        );

        given(rankingClient.getUnionRanking(
                RANKING_DATE,
                null,
                1
        )).willReturn(
                new UnionRankingResponse(
                        List.of(
                                new UnionRankingResponse.Ranking(
                                        "invalid-date",
                                        10,
                                        "감점",
                                        "루나",
                                        "팬텀",
                                        "",
                                        9000,
                                        123_456_789L
                                )
                        )
                )
        );

        RankingException exception = catchThrowableOfType(
                () -> service.getUnionRanking(
                        RANKING_DATE,
                        null,
                        1
                ),
                RankingException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(NexonApiFailure.RESPONSE_INVALID);
    }

    @Test
    void 랭킹항목이null이면응답오류를반환한다() {
        RankingQueryService service = createService(
                "2026-07-21T00:30:00Z"
        );

        given(rankingClient.getUnionRanking(
                RANKING_DATE,
                null,
                1
        )).willReturn(
                new UnionRankingResponse(
                        Collections.singletonList(null)
                )
        );

        RankingException exception = catchThrowableOfType(
                () -> service.getUnionRanking(
                        RANKING_DATE,
                        null,
                        1
                ),
                RankingException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(NexonApiFailure.RESPONSE_INVALID);
    }

    private RankingQueryService createService(
            String instant
    ) {
        return new RankingQueryService(
                rankingClient,
                Clock.fixed(
                        Instant.parse(instant),
                        ZoneId.of("Asia/Seoul")
                )
        );
    }

    private OverallRankingResponse emptyOverallRanking() {
        return new OverallRankingResponse(List.of());
    }

    private UnionRankingResponse emptyUnionRanking() {
        return new UnionRankingResponse(List.of());
    }

    private DojangRankingResponse emptyDojangRanking() {
        return new DojangRankingResponse(List.of());
    }
}
