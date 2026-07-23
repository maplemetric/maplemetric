package com.maplemetric.ranking.application.result;

import com.maplemetric.common.nexon.NexonApiFailure;
import com.maplemetric.ranking.domain.exception.RankingException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Function;

final class RankingResultSupport {

    static final String SOURCE = "NEXON_OPEN_API";

    private RankingResultSupport() {
    }

    static <T> LocalDate resolveAsOf(
            List<T> ranking,
            LocalDate requestedDate,
            Function<T, String> dateExtractor
    ) {
        if (ranking.isEmpty()) {
            return requestedDate;
        }

        LocalDate asOf = parseRankingDate(
                ranking.get(0),
                dateExtractor
        );

        boolean hasDifferentDate = ranking.stream()
                .map(item -> parseRankingDate(
                        item,
                        dateExtractor
                ))
                .anyMatch(date -> !asOf.equals(date));

        if (hasDifferentDate) {
            throw invalidResponseException();
        }

        return asOf;
    }

    private static <T> LocalDate parseRankingDate(
            T ranking,
            Function<T, String> dateExtractor
    ) {
        if (ranking == null) {
            throw invalidResponseException();
        }

        return parseDate(dateExtractor.apply(ranking));
    }

    private static LocalDate parseDate(
            String date
    ) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw invalidResponseException();
        }
    }

    private static RankingException invalidResponseException() {
        return new RankingException(
                NexonApiFailure.RESPONSE_INVALID
        );
    }
}
