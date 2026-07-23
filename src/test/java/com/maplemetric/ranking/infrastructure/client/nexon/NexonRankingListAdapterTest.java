package com.maplemetric.ranking.infrastructure.client.nexon;

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
import com.maplemetric.ranking.infrastructure.client.nexon.response.DojangRankingResponse;
import com.maplemetric.ranking.infrastructure.client.nexon.response.OverallRankingResponse;
import com.maplemetric.ranking.infrastructure.client.nexon.response.UnionRankingResponse;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NexonRankingListAdapterTest {

    private static final LocalDate RANKING_DATE =
            LocalDate.of(2026, 7, 19);

    @Mock
    private RankingClient rankingClient;

    @Test
    void 종합랭킹응답필드와응답기준일을매핑한다() {
        NexonRankingListAdapter adapter = createAdapter();

        given(rankingClient.getOverallRanking(
                RANKING_DATE,
                null,
                0,
                null,
                2
        )).willReturn(
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
                )
        );

        GetOverallRankingResult result =
                adapter.loadOverallRanking(
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

        verify(rankingClient).getOverallRanking(
                RANKING_DATE,
                null,
                0,
                null,
                2
        );
    }

    @Test
    void 유니온과무릉도장응답필드를매핑한다() {
        NexonRankingListAdapter adapter = createAdapter();

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
                adapter.loadUnionRanking(
                        RANKING_DATE,
                        null,
                        1
                );

        GetDojangRankingResult dojangResult =
                adapter.loadDojangRanking(
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
        NexonRankingListAdapter adapter = createAdapter();

        given(rankingClient.getDojangRanking(
                RANKING_DATE,
                null,
                0,
                null,
                3
        )).willReturn(
                new DojangRankingResponse(List.of())
        );

        GetDojangRankingResult result =
                adapter.loadDojangRanking(
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
        NexonRankingListAdapter adapter = createAdapter();

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
                () -> adapter.loadUnionRanking(
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
        NexonRankingListAdapter adapter = createAdapter();

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
                () -> adapter.loadUnionRanking(
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
    void 랭킹항목의기준일이서로다르면응답오류를반환한다() {
        NexonRankingListAdapter adapter = createAdapter();

        given(rankingClient.getUnionRanking(
                RANKING_DATE,
                null,
                1
        )).willReturn(
                new UnionRankingResponse(
                        List.of(
                                createUnionRanking("2026-07-18"),
                                createUnionRanking("2026-07-19")
                        )
                )
        );

        RankingException exception = catchThrowableOfType(
                () -> adapter.loadUnionRanking(
                        RANKING_DATE,
                        null,
                        1
                ),
                RankingException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(NexonApiFailure.RESPONSE_INVALID);
    }

    private NexonRankingListAdapter createAdapter() {
        return new NexonRankingListAdapter(rankingClient);
    }

    private UnionRankingResponse.Ranking createUnionRanking(
            String date
    ) {
        return new UnionRankingResponse.Ranking(
                date,
                10,
                "감점",
                "루나",
                "팬텀",
                "",
                9000,
                123_456_789L
        );
    }
}
