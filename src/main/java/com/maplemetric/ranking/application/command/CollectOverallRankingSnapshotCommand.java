package com.maplemetric.ranking.application.command;

import java.time.LocalDate;

public record CollectOverallRankingSnapshotCommand(
        LocalDate date,
        String worldName,
        Integer worldType,
        String className,
        int maxPages
) {

    private static final int MAX_PAGES_LIMIT = 100;

    public CollectOverallRankingSnapshotCommand {
        if (maxPages < 1) {
            throw new IllegalArgumentException(
                    "최대 수집 페이지 수는 1 이상이어야 합니다."
            );
        }

        if (maxPages > MAX_PAGES_LIMIT) {
            throw new IllegalArgumentException(
                    "최대 수집 페이지 수는 "
                            + MAX_PAGES_LIMIT
                            + " 이하여야 합니다."
            );
        }
    }
}
