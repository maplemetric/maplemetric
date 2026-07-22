package com.maplemetric.ranking.application.service;

import com.maplemetric.ranking.RankingDateResolver;
import com.maplemetric.ranking.application.result.GetDojangRankingResult;
import com.maplemetric.ranking.application.result.GetOverallRankingResult;
import com.maplemetric.ranking.application.result.GetUnionRankingResult;
import com.maplemetric.ranking.infrastructure.client.RankingClient;
import com.maplemetric.ranking.infrastructure.client.dto.DojangRankingResponse;
import com.maplemetric.ranking.infrastructure.client.dto.OverallRankingResponse;
import com.maplemetric.ranking.infrastructure.client.dto.UnionRankingResponse;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RankingQueryService {

    private final RankingClient rankingClient;
    private final Clock clock;

    @Autowired
    public RankingQueryService(
            RankingClient rankingClient
    ) {
        this(
                rankingClient,
                Clock.system(RankingDateResolver.KOREA_ZONE_ID)
        );
    }

    RankingQueryService(
            RankingClient rankingClient,
            Clock clock
    ) {
        this.rankingClient = rankingClient;
        this.clock = clock;
    }

    public GetOverallRankingResult getOverallRanking(
            LocalDate date,
            String worldName,
            Integer worldType,
            String className,
            int page
    ) {
        LocalDate rankingDate =
                RankingDateResolver.resolve(date, clock);

        OverallRankingResponse response =
                rankingClient.getOverallRanking(
                        rankingDate,
                        worldName,
                        worldType,
                        className,
                        page
                );

        return GetOverallRankingResult.from(
                response,
                page,
                rankingDate
        );
    }

    public GetUnionRankingResult getUnionRanking(
            LocalDate date,
            String worldName,
            int page
    ) {
        LocalDate rankingDate =
                RankingDateResolver.resolve(date, clock);

        UnionRankingResponse response =
                rankingClient.getUnionRanking(
                        rankingDate,
                        worldName,
                        page
                );

        return GetUnionRankingResult.from(
                response,
                page,
                rankingDate
        );
    }

    public GetDojangRankingResult getDojangRanking(
            LocalDate date,
            String worldName,
            int difficulty,
            String className,
            int page
    ) {
        LocalDate rankingDate =
                RankingDateResolver.resolve(date, clock);

        DojangRankingResponse response =
                rankingClient.getDojangRanking(
                        rankingDate,
                        worldName,
                        difficulty,
                        className,
                        page
                );

        return GetDojangRankingResult.from(
                response,
                page,
                rankingDate
        );
    }
}
