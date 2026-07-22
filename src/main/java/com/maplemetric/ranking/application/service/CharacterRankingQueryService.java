package com.maplemetric.ranking.application.service;

import com.maplemetric.ranking.CharacterRanking;
import com.maplemetric.ranking.CharacterRankingQuery;
import com.maplemetric.ranking.CharacterRankingQueryException;
import com.maplemetric.ranking.RankingDateResolver;
import com.maplemetric.ranking.domain.exception.RankingException;
import com.maplemetric.ranking.infrastructure.client.RankingClient;
import com.maplemetric.ranking.infrastructure.client.dto.OverallRankingResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CharacterRankingQueryService implements CharacterRankingQuery {

    private final RankingClient rankingClient;
    private final Clock clock;

    @Autowired
    public CharacterRankingQueryService(
            RankingClient rankingClient
    ) {
        this(
                rankingClient,
                Clock.system(RankingDateResolver.KOREA_ZONE_ID)
        );
    }

    CharacterRankingQueryService(
            RankingClient rankingClient,
            Clock clock
    ) {
        this.rankingClient = rankingClient;
        this.clock = clock;
    }

    @Override
    public CharacterRanking getCharacterRanking(
            String ocid,
            String characterName,
            String worldName
    ) {
        try {
            return queryCharacterRanking(
                    ocid,
                    characterName,
                    worldName
            );
        } catch (RankingException exception) {
            throw new CharacterRankingQueryException(
                    exception.getFailure()
            );
        }
    }

    private CharacterRanking queryCharacterRanking(
            String ocid,
            String characterName,
            String worldName
    ) {
        LocalDate rankingDate =
                RankingDateResolver.resolve(null, clock);

        OverallRankingResponse overallRankingResponse =
                rankingClient.getCharacterOverallRanking(
                        ocid,
                        rankingDate
                );

        OverallRankingResponse worldRankingResponse =
                rankingClient.getCharacterWorldRanking(
                        ocid,
                        worldName,
                        rankingDate
                );

        String classRankingFilter =
                resolveClassRankingFilter(
                        characterName,
                        overallRankingResponse
                );

        OverallRankingResponse classRankingResponse = null;
        OverallRankingResponse worldClassRankingResponse = null;

        if (StringUtils.hasText(classRankingFilter)) {
            classRankingResponse =
                    rankingClient.getCharacterClassRanking(
                            ocid,
                            classRankingFilter,
                            rankingDate
                    );

            worldClassRankingResponse =
                    rankingClient.getCharacterWorldClassRanking(
                            ocid,
                            worldName,
                            classRankingFilter,
                            rankingDate
                    );
        }

        return new CharacterRanking(
                extractRank(characterName, overallRankingResponse),
                extractRank(characterName, worldRankingResponse),
                extractRank(characterName, classRankingResponse),
                extractRank(characterName, worldClassRankingResponse)
        );
    }

    private String resolveClassRankingFilter(
            String characterName,
            OverallRankingResponse response
    ) {
        if (response == null
                || response.ranking() == null) {
            return null;
        }

        return response.ranking()
                .stream()
                .filter(ranking -> ranking != null)
                .filter(ranking -> Objects.equals(
                        characterName,
                        ranking.characterName()
                ))
                .findFirst()
                .map(ranking -> createClassRankingFilter(
                        ranking.className(),
                        ranking.subClassName()
                ))
                .orElse(null);
    }

    private String createClassRankingFilter(
            String className,
            String subClassName
    ) {
        if (!StringUtils.hasText(className)) {
            return null;
        }

        if (StringUtils.hasText(subClassName)) {
            return className
                    + "-"
                    + subClassName;
        }

        return className
                + "-전체 전직";
    }

    private Integer extractRank(
            String characterName,
            OverallRankingResponse response
    ) {
        if (response == null
                || response.ranking() == null) {
            return null;
        }

        return response.ranking()
                .stream()
                .filter(ranking -> ranking != null)
                .filter(ranking -> Objects.equals(
                        characterName,
                        ranking.characterName()
                ))
                .findFirst()
                .map(ranking -> ranking.ranking())
                .orElse(null);
    }
}
