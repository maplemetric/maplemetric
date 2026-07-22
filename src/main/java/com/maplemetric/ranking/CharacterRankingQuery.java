package com.maplemetric.ranking;

public interface CharacterRankingQuery {

    CharacterRanking getCharacterRanking(
            String ocid,
            String characterName,
            String worldName
    );
}
