package com.maplemetric.ranking.api;

public interface CharacterRankingQuery {

    CharacterRanking getCharacterRanking(
            String ocid,
            String characterName,
            String worldName
    );
}
