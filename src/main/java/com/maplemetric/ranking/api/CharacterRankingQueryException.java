package com.maplemetric.ranking.api;

import java.util.Objects;

public class CharacterRankingQueryException extends RuntimeException {

    private final CharacterRankingQueryFailure failure;

    public CharacterRankingQueryException(
            CharacterRankingQueryFailure failure
    ) {
        this.failure = Objects.requireNonNull(failure);
    }

    public CharacterRankingQueryFailure getFailure() {
        return failure;
    }
}
