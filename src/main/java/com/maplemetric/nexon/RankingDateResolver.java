package com.maplemetric.nexon;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class RankingDateResolver {

    public static final ZoneId KOREA_ZONE_ID =
            ZoneId.of("Asia/Seoul");

    private static final LocalTime RANKING_AVAILABLE_TIME =
            LocalTime.of(9, 30);

    private RankingDateResolver() {
    }

    public static LocalDate resolve(
            LocalDate requestedDate,
            Clock clock
    ) {
        if (requestedDate != null) {
            return requestedDate;
        }

        ZonedDateTime now =
                ZonedDateTime.now(clock)
                        .withZoneSameInstant(KOREA_ZONE_ID);

        LocalDate today = now.toLocalDate();

        if (now.toLocalTime()
                .isBefore(RANKING_AVAILABLE_TIME)) {
            return today.minusDays(1);
        }

        return today;
    }
}
