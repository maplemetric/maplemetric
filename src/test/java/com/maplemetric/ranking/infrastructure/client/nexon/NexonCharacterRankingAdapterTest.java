package com.maplemetric.ranking.infrastructure.client.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.maplemetric.ranking.application.port.out.LoadCharacterRankingPort.RankingEntry;
import com.maplemetric.ranking.infrastructure.client.nexon.response.OverallRankingResponse;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NexonCharacterRankingAdapterTest {

    private static final String OCID = "test-ocid";
    private static final String WORLD_NAME = "루나";
    private static final String CLASS_FILTER =
            "팬텀-전체 전직";
    private static final LocalDate RANKING_DATE =
            LocalDate.of(2026, 7, 21);

    @Mock
    private RankingClient rankingClient;

    @Test
    void 네종류의캐릭터랭킹응답을Application항목으로매핑한다() {
        NexonCharacterRankingAdapter adapter = createAdapter();

        given(rankingClient.getCharacterOverallRanking(
                OCID,
                RANKING_DATE
        )).willReturn(createResponse(58333));

        given(rankingClient.getCharacterWorldRanking(
                OCID,
                WORLD_NAME,
                RANKING_DATE
        )).willReturn(createResponse(10244));

        given(rankingClient.getCharacterClassRanking(
                OCID,
                CLASS_FILTER,
                RANKING_DATE
        )).willReturn(createResponse(1588));

        given(rankingClient.getCharacterWorldClassRanking(
                OCID,
                WORLD_NAME,
                CLASS_FILTER,
                RANKING_DATE
        )).willReturn(createResponse(321));

        assertThat(adapter.loadOverallRanking(
                OCID,
                RANKING_DATE
        )).containsExactly(createEntry(58333));

        assertThat(adapter.loadWorldRanking(
                OCID,
                WORLD_NAME,
                RANKING_DATE
        )).containsExactly(createEntry(10244));

        assertThat(adapter.loadClassRanking(
                OCID,
                CLASS_FILTER,
                RANKING_DATE
        )).containsExactly(createEntry(1588));

        assertThat(adapter.loadWorldClassRanking(
                OCID,
                WORLD_NAME,
                CLASS_FILTER,
                RANKING_DATE
        )).containsExactly(createEntry(321));

        verify(rankingClient).getCharacterOverallRanking(
                OCID,
                RANKING_DATE
        );

        verify(rankingClient).getCharacterWorldRanking(
                OCID,
                WORLD_NAME,
                RANKING_DATE
        );

        verify(rankingClient).getCharacterClassRanking(
                OCID,
                CLASS_FILTER,
                RANKING_DATE
        );

        verify(rankingClient).getCharacterWorldClassRanking(
                OCID,
                WORLD_NAME,
                CLASS_FILTER,
                RANKING_DATE
        );
    }

    @Test
    void null응답과null목록과null항목은빈목록으로변환한다() {
        NexonCharacterRankingAdapter adapter = createAdapter();

        given(rankingClient.getCharacterOverallRanking(
                OCID,
                RANKING_DATE
        )).willReturn(null);

        given(rankingClient.getCharacterWorldRanking(
                OCID,
                WORLD_NAME,
                RANKING_DATE
        )).willReturn(new OverallRankingResponse(null));

        given(rankingClient.getCharacterClassRanking(
                OCID,
                CLASS_FILTER,
                RANKING_DATE
        )).willReturn(
                new OverallRankingResponse(
                        Collections.singletonList(null)
                )
        );

        assertThat(adapter.loadOverallRanking(
                OCID,
                RANKING_DATE
        )).isEmpty();

        assertThat(adapter.loadWorldRanking(
                OCID,
                WORLD_NAME,
                RANKING_DATE
        )).isEmpty();

        assertThat(adapter.loadClassRanking(
                OCID,
                CLASS_FILTER,
                RANKING_DATE
        )).isEmpty();
    }

    private NexonCharacterRankingAdapter createAdapter() {
        return new NexonCharacterRankingAdapter(rankingClient);
    }

    private OverallRankingResponse createResponse(
            int ranking
    ) {
        return new OverallRankingResponse(
                List.of(
                        new OverallRankingResponse.Ranking(
                                "2026-07-21",
                                ranking,
                                "감점",
                                WORLD_NAME,
                                "팬텀",
                                null,
                                290,
                                123_456_789L,
                                321,
                                "메이플"
                        )
                )
        );
    }

    private RankingEntry createEntry(
            int ranking
    ) {
        return new RankingEntry(
                ranking,
                "감점",
                "팬텀",
                null
        );
    }
}
