package com.maplemetric.ranking.api;

public record CharacterRanking(
        Integer overallRank,
        Integer worldRank,
        Integer classRank,
        Integer worldClassRank
) {
}
