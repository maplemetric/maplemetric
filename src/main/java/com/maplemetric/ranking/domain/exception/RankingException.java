package com.maplemetric.ranking.domain.exception;

import com.maplemetric.common.nexon.NexonApiFailure;
import java.util.Objects;

public class RankingException extends RuntimeException {

    private final NexonApiFailure failure;

    public RankingException(NexonApiFailure failure) {
        this.failure = Objects.requireNonNull(failure);
    }

    public NexonApiFailure getFailure() {
        return failure;
    }
}
