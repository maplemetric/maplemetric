package com.maplemetric.ranking.infrastructure.client.nexon;

import com.maplemetric.ranking.application.port.out.LoadCharacterRankingPort;
import com.maplemetric.ranking.application.port.out.LoadCharacterRankingPort.RankingEntry;
import com.maplemetric.ranking.infrastructure.client.nexon.response.OverallRankingResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class NexonCharacterRankingAdapter
        implements LoadCharacterRankingPort {

    private final RankingClient rankingClient;

    NexonCharacterRankingAdapter(
            RankingClient rankingClient
    ) {
        this.rankingClient = rankingClient;
    }

    @Override
    public List<RankingEntry> loadOverallRanking(
            String ocid,
            LocalDate date
    ) {
        return mapRanking(
                rankingClient.getCharacterOverallRanking(
                        ocid,
                        date
                )
        );
    }

    @Override
    public List<RankingEntry> loadWorldRanking(
            String ocid,
            String worldName,
            LocalDate date
    ) {
        return mapRanking(
                rankingClient.getCharacterWorldRanking(
                        ocid,
                        worldName,
                        date
                )
        );
    }

    @Override
    public List<RankingEntry> loadClassRanking(
            String ocid,
            String classRankingFilter,
            LocalDate date
    ) {
        return mapRanking(
                rankingClient.getCharacterClassRanking(
                        ocid,
                        classRankingFilter,
                        date
                )
        );
    }

    @Override
    public List<RankingEntry> loadWorldClassRanking(
            String ocid,
            String worldName,
            String classRankingFilter,
            LocalDate date
    ) {
        return mapRanking(
                rankingClient.getCharacterWorldClassRanking(
                        ocid,
                        worldName,
                        classRankingFilter,
                        date
                )
        );
    }

    private List<RankingEntry> mapRanking(
            OverallRankingResponse response
    ) {
        if (response == null
                || response.ranking() == null) {
            return List.of();
        }

        return response.ranking()
                .stream()
                .filter(item -> item != null)
                .map(item -> new RankingEntry(
                        item.ranking(),
                        item.characterName(),
                        item.className(),
                        item.subClassName()
                ))
                .toList();
    }
}
