package com.maplemetric.ranking.infrastructure.client.nexon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.common.nexon.NexonApiFailure;
import com.maplemetric.common.nexon.NexonApiRequester;
import com.maplemetric.common.nexon.NexonRequestRateGate;
import com.maplemetric.ranking.domain.exception.RankingException;
import com.maplemetric.ranking.infrastructure.client.nexon.response.DojangRankingResponse;
import com.maplemetric.ranking.infrastructure.client.nexon.response.OverallRankingResponse;
import com.maplemetric.ranking.infrastructure.client.nexon.response.UnionRankingResponse;
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

    private static final String CHARACTER_OVERALL_RANKING_API =
            "캐릭터 종합 랭킹 정보";

    private static final String CHARACTER_WORLD_RANKING_API =
            "캐릭터 월드 랭킹 정보";

    private static final String CHARACTER_CLASS_RANKING_API =
            "캐릭터 직업 랭킹 정보";

    private static final String CHARACTER_WORLD_CLASS_RANKING_API =
            "캐릭터 월드 내 직업 랭킹 정보";

    private static final String UNION_RANKING_API =
            "유니온 랭킹 목록";

    private static final String DOJANG_RANKING_API =
            "무릉도장 랭킹 목록";

    private final NexonApiRequester nexonApiRequester;

    public RankingClientImpl(
            @Qualifier("nexonRestClient") RestClient nexonRestClient,
            ObjectMapper objectMapper,
            NexonRequestRateGate rateGate
    ) {
        this.nexonApiRequester = new NexonApiRequester(
                nexonRestClient,
                objectMapper,
                this::createException,
                (apiName, errorCode) -> false,
                rateGate
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
    public OverallRankingResponse getCharacterOverallRanking(
            String ocid,
            LocalDate date
    ) {
        return requestCharacterRanking(
                ocid,
                date,
                null,
                null,
                CHARACTER_OVERALL_RANKING_API
        );
    }

    @Override
    public OverallRankingResponse getCharacterWorldRanking(
            String ocid,
            String worldName,
            LocalDate date
    ) {
        return requestCharacterRanking(
                ocid,
                date,
                worldName,
                null,
                CHARACTER_WORLD_RANKING_API
        );
    }

    @Override
    public OverallRankingResponse getCharacterClassRanking(
            String ocid,
            String classRankingFilter,
            LocalDate date
    ) {
        return requestCharacterRanking(
                ocid,
                date,
                null,
                classRankingFilter,
                CHARACTER_CLASS_RANKING_API
        );
    }

    @Override
    public OverallRankingResponse getCharacterWorldClassRanking(
            String ocid,
            String worldName,
            String classRankingFilter,
            LocalDate date
    ) {
        return requestCharacterRanking(
                ocid,
                date,
                worldName,
                classRankingFilter,
                CHARACTER_WORLD_CLASS_RANKING_API
        );
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
                    NexonApiFailure.RESPONSE_INVALID
            );
        }
    }

    private OverallRankingResponse requestCharacterRanking(
            String ocid,
            LocalDate date,
            String worldName,
            String classRankingFilter,
            String apiName
    ) {
        LinkedHashMap<String, String> queryParameters =
                new LinkedHashMap<>();

        queryParameters.put("date", date.toString());

        if (StringUtils.hasText(worldName)) {
            queryParameters.put("world_name", worldName);
        }

        if (StringUtils.hasText(classRankingFilter)) {
            queryParameters.put("class", classRankingFilter);
        }

        queryParameters.put("ocid", ocid);

        return nexonApiRequester.request(
                OVERALL_RANKING_PATH,
                queryParameters,
                OverallRankingResponse.class,
                apiName,
                "ocid",
                ocid
        );
    }

    private RankingException createException(
            NexonApiFailure failure
    ) {
        return new RankingException(failure);
    }
}
