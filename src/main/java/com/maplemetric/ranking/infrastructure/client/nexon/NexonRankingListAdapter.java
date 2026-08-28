package com.maplemetric.ranking.infrastructure.client.nexon;

import com.maplemetric.common.nexon.NexonApiFailure;
import com.maplemetric.common.nexon.NexonRequestClass;
import com.maplemetric.ranking.api.OverallRankingCollectionRequestClass;
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
            int page,
            OverallRankingCollectionRequestClass requestClass
    ) {
        OverallRankingResponse response =
                rankingClient.getOverallRanking(
                        date,
                        worldName,
                        worldType,
                        className,
                        page,
                        toNexonRequestClass(requestClass)
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
/**
     * 수집 등급을 외부 호출 등급으로 옮긴다.
     *
     * 두 이름이 같아 보여도 소유자가 다르다. 랭킹은 "이 수집이 미뤄도 되는가"를
     * 말하고, 외부 관문은 "어느 Key 몫을 쓰는가"를 말한다. 여기서 한 번 옮겨 두면
     * 랭킹의 공개 계약이 외부 공급자의 Key 배분 개념을 드러내지 않는다.
     */
    private static NexonRequestClass toNexonRequestClass(
            OverallRankingCollectionRequestClass requestClass
    ) {
        return switch (requestClass) {
            case CRITICAL -> NexonRequestClass.CRITICAL;
            case BULK -> NexonRequestClass.BULK;
        };
    }
}
