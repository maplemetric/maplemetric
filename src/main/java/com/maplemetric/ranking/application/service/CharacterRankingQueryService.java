package com.maplemetric.ranking.application.service;

import com.maplemetric.ranking.CharacterRanking;
import com.maplemetric.ranking.CharacterRankingQuery;
import com.maplemetric.ranking.CharacterRankingQueryException;
import com.maplemetric.ranking.application.port.out.LoadCharacterRankingPort;
import com.maplemetric.ranking.application.port.out.LoadCharacterRankingPort.RankingEntry;
import com.maplemetric.ranking.domain.exception.RankingException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CharacterRankingQueryService implements CharacterRankingQuery {

    private final LoadCharacterRankingPort loadCharacterRankingPort;
    private final Clock clock;

    @Autowired
    public CharacterRankingQueryService(
            LoadCharacterRankingPort loadCharacterRankingPort
    ) {
        this(
                loadCharacterRankingPort,
                Clock.system(RankingDateResolver.KOREA_ZONE_ID)
        );
    }

    CharacterRankingQueryService(
            LoadCharacterRankingPort loadCharacterRankingPort,
            Clock clock
    ) {
        this.loadCharacterRankingPort = loadCharacterRankingPort;
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

        List<RankingEntry> overallRanking =
                loadCharacterRankingPort.loadOverallRanking(
                        ocid,
                        rankingDate
                );

        List<RankingEntry> worldRanking =
                loadCharacterRankingPort.loadWorldRanking(
                        ocid,
                        worldName,
                        rankingDate
                );

        String classRankingFilter =
                resolveClassRankingFilter(
                        characterName,
                        overallRanking
                );

        List<RankingEntry> classRanking = List.of();
        List<RankingEntry> worldClassRanking = List.of();

        if (StringUtils.hasText(classRankingFilter)) {
            classRanking =
                    loadCharacterRankingPort.loadClassRanking(
                            ocid,
                            classRankingFilter,
                            rankingDate
                    );

            worldClassRanking =
                    loadCharacterRankingPort.loadWorldClassRanking(
                            ocid,
                            worldName,
                            classRankingFilter,
                            rankingDate
                    );
        }

        return new CharacterRanking(
                extractRank(characterName, overallRanking),
                extractRank(characterName, worldRanking),
                extractRank(characterName, classRanking),
                extractRank(characterName, worldClassRanking)
        );
    }

    private String resolveClassRankingFilter(
            String characterName,
            List<RankingEntry> ranking
    ) {
        return findRanking(characterName, ranking)
                .map(item -> createClassRankingFilter(
                        item.className(),
                        item.subClassName()
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
            List<RankingEntry> ranking
    ) {
        return findRanking(characterName, ranking)
                .map(item -> item.ranking())
                .orElse(null);
    }

    private Optional<RankingEntry> findRanking(
            String characterName,
            List<RankingEntry> ranking
    ) {
        if (ranking == null) {
            return Optional.empty();
        }

        return ranking
                .stream()
                .filter(item -> item != null)
                .filter(item -> Objects.equals(
                        characterName,
                        item.characterName()
                ))
                .findFirst();
    }
}
