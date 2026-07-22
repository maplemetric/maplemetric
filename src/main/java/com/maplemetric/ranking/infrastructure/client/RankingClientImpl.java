package com.maplemetric.ranking.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.nexon.NexonApiFailure;
import com.maplemetric.nexon.NexonApiRequester;
import com.maplemetric.ranking.domain.exception.RankingErrorCode;
import com.maplemetric.ranking.domain.exception.RankingException;
import com.maplemetric.ranking.infrastructure.client.dto.DojangRankingResponse;
import com.maplemetric.ranking.infrastructure.client.dto.OverallRankingResponse;
import com.maplemetric.ranking.infrastructure.client.dto.UnionRankingResponse;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class RankingClientImpl implements RankingClient {

    private static final String OVERALL_RANKING_PATH =
            "/maplestory/v1/ranking/overall";

    private static final String UNION_RANKING_PATH =
            "/maplestory/v1/ranking/union";

    private static final String DOJANG_RANKING_PATH =
            "/maplestory/v1/ranking/dojang";

    private static final String OVERALL_RANKING_API =
            "종합 랭킹 목록";

    private static final String UNION_RANKING_API =
            "유니온 랭킹 목록";

    private static final String DOJANG_RANKING_API =
            "무릉도장 랭킹 목록";

    private final NexonApiRequester nexonApiRequester;

    public RankingClientImpl(
            @Qualifier("nexonRestClient") RestClient nexonRestClient,
            ObjectMapper objectMapper
    ) {
        this.nexonApiRequester = new NexonApiRequester(
                nexonRestClient,
                objectMapper,
                this::createException,
                (apiName, errorCode) -> false
        );
    }

    @Override
    public OverallRankingResponse getOverallRanking(
            LocalDate date,
            String worldName,
            Integer worldType,
            String className,
            int page
    ) {
        LinkedHashMap<String, String> queryParameters =
                new LinkedHashMap<>();

        queryParameters.put("date", date.toString());

        if (StringUtils.hasText(worldName)) {
            queryParameters.put("world_name", worldName);
        } else if (worldType != null) {
            queryParameters.put(
                    "world_type",
                    worldType.toString()
            );
        }

        if (StringUtils.hasText(className)) {
            queryParameters.put("class", className);
        }

        queryParameters.put("page", Integer.toString(page));

        OverallRankingResponse response =
                nexonApiRequester.request(
                        OVERALL_RANKING_PATH,
                        queryParameters,
                        OverallRankingResponse.class,
                        OVERALL_RANKING_API,
                        "page",
                        Integer.toString(page)
                );

        validateRanking(response.ranking());

        return response;
    }

    @Override
    public UnionRankingResponse getUnionRanking(
            LocalDate date,
            String worldName,
            int page
    ) {
        LinkedHashMap<String, String> queryParameters =
                new LinkedHashMap<>();

        queryParameters.put("date", date.toString());

        if (StringUtils.hasText(worldName)) {
            queryParameters.put("world_name", worldName);
        }

        queryParameters.put("page", Integer.toString(page));

        UnionRankingResponse response =
                nexonApiRequester.request(
                        UNION_RANKING_PATH,
                        queryParameters,
                        UnionRankingResponse.class,
                        UNION_RANKING_API,
                        "page",
                        Integer.toString(page)
                );

        validateRanking(response.ranking());

        return response;
    }

    @Override
    public DojangRankingResponse getDojangRanking(
            LocalDate date,
            String worldName,
            int difficulty,
            String className,
            int page
    ) {
        LinkedHashMap<String, String> queryParameters =
                new LinkedHashMap<>();

        queryParameters.put("date", date.toString());

        if (StringUtils.hasText(worldName)) {
            queryParameters.put("world_name", worldName);
        }

        queryParameters.put(
                "difficulty",
                Integer.toString(difficulty)
        );

        if (StringUtils.hasText(className)) {
            queryParameters.put("class", className);
        }

        queryParameters.put("page", Integer.toString(page));

        DojangRankingResponse response =
                nexonApiRequester.request(
                        DOJANG_RANKING_PATH,
                        queryParameters,
                        DojangRankingResponse.class,
                        DOJANG_RANKING_API,
                        "page",
                        Integer.toString(page)
                );

        validateRanking(response.ranking());

        return response;
    }

    private void validateRanking(
            List<?> ranking
    ) {
        if (ranking == null
                || ranking.stream()
                .anyMatch(item -> item == null)) {
            throw new RankingException(
                    RankingErrorCode.NEXON_API_RESPONSE_INVALID
            );
        }
    }

    private RankingException createException(
            NexonApiFailure failure
    ) {
        RankingErrorCode errorCode = switch (failure) {
            case NOT_FOUND, CLIENT_ERROR ->
                    RankingErrorCode.NEXON_API_CLIENT_ERROR;
            case SERVER_ERROR ->
                    RankingErrorCode.NEXON_API_SERVER_ERROR;
            case TIMEOUT ->
                    RankingErrorCode.NEXON_API_TIMEOUT;
            case RESPONSE_INVALID ->
                    RankingErrorCode.NEXON_API_RESPONSE_INVALID;
        };

        return new RankingException(errorCode);
    }
}
