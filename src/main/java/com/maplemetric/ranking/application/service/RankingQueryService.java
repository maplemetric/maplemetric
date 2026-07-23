package com.maplemetric.ranking.application.service;

import com.maplemetric.ranking.application.port.out.LoadRankingListPort;
import com.maplemetric.ranking.application.result.GetDojangRankingResult;
import com.maplemetric.ranking.application.result.GetOverallRankingResult;
import com.maplemetric.ranking.application.result.GetUnionRankingResult;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RankingQueryService {

    private final LoadRankingListPort loadRankingListPort;
    private final Clock clock;

    @Autowired
    public RankingQueryService(
            LoadRankingListPort loadRankingListPort
    ) {
        this(
                loadRankingListPort,
                Clock.system(RankingDateResolver.KOREA_ZONE_ID)
        );
    }

    RankingQueryService(
            LoadRankingListPort loadRankingListPort,
            Clock clock
    ) {
        this.loadRankingListPort = loadRankingListPort;
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

        return loadRankingListPort.loadOverallRanking(
                rankingDate,
                worldName,
                worldType,
                className,
                page
        );
    }

    public GetUnionRankingResult getUnionRanking(
            LocalDate date,
            String worldName,
            int page
    ) {
        LocalDate rankingDate =
                RankingDateResolver.resolve(date, clock);

        return loadRankingListPort.loadUnionRanking(
                rankingDate,
                worldName,
                page
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

        return loadRankingListPort.loadDojangRanking(
                rankingDate,
                worldName,
                difficulty,
                className,
                page
        );
    }
}
