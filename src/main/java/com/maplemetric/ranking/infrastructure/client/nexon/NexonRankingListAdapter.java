package com.maplemetric.ranking.infrastructure.client.nexon;

import com.maplemetric.common.nexon.NexonApiFailure;
import com.maplemetric.ranking.application.port.out.LoadRankingListPort;
import com.maplemetric.ranking.application.result.GetDojangRankingResult;
import com.maplemetric.ranking.application.result.GetOverallRankingResult;
import com.maplemetric.ranking.application.result.GetUnionRankingResult;
import com.maplemetric.ranking.domain.exception.RankingException;
import com.maplemetric.ranking.infrastructure.client.nexon.response.DojangRankingResponse;
import com.maplemetric.ranking.infrastructure.client.nexon.response.OverallRankingResponse;
import com.maplemetric.ranking.infrastructure.client.nexon.response.UnionRankingResponse;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
class NexonRankingListAdapter
        implements LoadRankingListPort {

    private static final String SOURCE = "NEXON_OPEN_API";

    private final RankingClient rankingClient;

    NexonRankingListAdapter(
            RankingClient rankingClient
    ) {
        this.rankingClient = rankingClient;
    }

    @Override
    public GetOverallRankingResult loadOverallRanking(
            LocalDate date,
            String worldName,
            Integer worldType,
            String className,
            int page
    ) {
        OverallRankingResponse response =
                rankingClient.getOverallRanking(
                        date,
                        worldName,
                        worldType,
                        className,
                        page
                );

        LocalDate asOf = resolveAsOf(
                response.ranking(),
                date,
                item -> item.date()
        );

        List<GetOverallRankingResult.Ranking> ranking =
                response.ranking()
                        .stream()
                        .map(item ->
                                new GetOverallRankingResult.Ranking(
                                        item.ranking(),
                                        item.characterName(),
                                        item.worldName(),
                                        item.className(),
                                        item.subClassName(),
                                        item.characterLevel(),
                                        item.characterExp(),
                                        item.characterPopularity(),
                                        item.characterGuildName()
                                ))
                        .toList();

        return GetOverallRankingResult.of(
                ranking,
                page,
                asOf,
                SOURCE
        );
    }

    @Override
    public GetUnionRankingResult loadUnionRanking(
            LocalDate date,
            String worldName,
            int page
    ) {
        UnionRankingResponse response =
                rankingClient.getUnionRanking(
                        date,
                        worldName,
                        page
                );

        LocalDate asOf = resolveAsOf(
                response.ranking(),
                date,
                item -> item.date()
        );

        List<GetUnionRankingResult.Ranking> ranking =
                response.ranking()
                        .stream()
                        .map(item ->
                                new GetUnionRankingResult.Ranking(
                                        item.ranking(),
                                        item.characterName(),
                                        item.worldName(),
                                        item.className(),
                                        item.subClassName(),
                                        item.unionLevel(),
                                        item.unionPower()
                                ))
                        .toList();

        return GetUnionRankingResult.of(
                ranking,
                page,
                asOf,
                SOURCE
        );
    }

    @Override
    public GetDojangRankingResult loadDojangRanking(
            LocalDate date,
            String worldName,
            int difficulty,
            String className,
            int page
    ) {
        DojangRankingResponse response =
                rankingClient.getDojangRanking(
                        date,
                        worldName,
                        difficulty,
                        className,
                        page
                );

        LocalDate asOf = resolveAsOf(
                response.ranking(),
                date,
                item -> item.date()
        );

        List<GetDojangRankingResult.Ranking> ranking =
                response.ranking()
                        .stream()
                        .map(item ->
                                new GetDojangRankingResult.Ranking(
                                        item.ranking(),
                                        item.characterName(),
                                        item.worldName(),
                                        item.className(),
                                        item.subClassName(),
                                        item.characterLevel(),
                                        item.dojangFloor(),
                                        item.dojangTimeRecord()
                                ))
                        .toList();

        return GetDojangRankingResult.of(
                ranking,
                page,
                asOf,
                SOURCE
        );
    }

    private <T> LocalDate resolveAsOf(
            List<T> ranking,
            LocalDate requestedDate,
            Function<T, String> dateExtractor
    ) {
        validateRanking(ranking);

        if (ranking.isEmpty()) {
            return requestedDate;
        }

        LocalDate asOf = parseRankingDate(
                ranking.get(0),
                dateExtractor
        );

        boolean hasDifferentDate = ranking.stream()
                .map(item -> parseRankingDate(
                        item,
                        dateExtractor
                ))
                .anyMatch(date -> !asOf.equals(date));

        if (hasDifferentDate) {
            throw invalidResponseException();
        }

        return asOf;
    }

    private void validateRanking(
            List<?> ranking
    ) {
        if (ranking == null
                || ranking.stream()
                .anyMatch(item -> item == null)) {
            throw invalidResponseException();
        }
    }

    private <T> LocalDate parseRankingDate(
            T ranking,
            Function<T, String> dateExtractor
    ) {
        try {
            return LocalDate.parse(
                    dateExtractor.apply(ranking)
            );
        } catch (DateTimeParseException
                 | NullPointerException exception) {
            throw invalidResponseException();
        }
    }

    private RankingException invalidResponseException() {
        return new RankingException(
                NexonApiFailure.RESPONSE_INVALID
        );
    }
}
