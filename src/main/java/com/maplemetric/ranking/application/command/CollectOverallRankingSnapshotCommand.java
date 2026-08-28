package com.maplemetric.ranking.application.command;

import com.maplemetric.ranking.api.OverallRankingCollectionRequestClass;
import java.time.LocalDate;

public record CollectOverallRankingSnapshotCommand(
        LocalDate date,
        String worldName,
        Integer worldType,
        String className,
        int maxPages,
        OverallRankingCollectionRequestClass requestClass
) {

    private static final int MAX_PAGES_LIMIT = 100;

    /** 등급을 밝히지 않은 수집은 실패하면 안 되는 몫을 쓴다. */
    public CollectOverallRankingSnapshotCommand(
            LocalDate date,
            String worldName,
            Integer worldType,
            String className,
            int maxPages
    ) {
        this(
                date,
                worldName,
                worldType,
                className,
                maxPages,
                OverallRankingCollectionRequestClass.CRITICAL
        );
    }

    public CollectOverallRankingSnapshotCommand {
        if (requestClass == null) {
            throw new IllegalArgumentException(
                    "수집 요청 등급은 필수입니다."
            );
        }

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
