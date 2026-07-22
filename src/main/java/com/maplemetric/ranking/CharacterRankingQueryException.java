package com.maplemetric.ranking;

import com.maplemetric.common.nexon.NexonApiFailure;
import java.util.Objects;

public class CharacterRankingQueryException extends RuntimeException {

    private final NexonApiFailure failure;

    public CharacterRankingQueryException(
            NexonApiFailure failure
    ) {
        this.failure = Objects.requireNonNull(failure);
    }

    public NexonApiFailure getFailure() {
        return failure;
    }
}
