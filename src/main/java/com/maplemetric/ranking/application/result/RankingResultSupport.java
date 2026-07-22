package com.maplemetric.ranking.application.result;

import com.maplemetric.ranking.domain.exception.RankingErrorCode;
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

        LocalDate asOf = parseDate(
                dateExtractor.apply(ranking.get(0))
        );

        boolean hasDifferentDate = ranking.stream()
                .map(item -> parseDate(dateExtractor.apply(item)))
                .anyMatch(date -> !asOf.equals(date));

        if (hasDifferentDate) {
            throw invalidResponseException();
        }

        return asOf;
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
                RankingErrorCode.NEXON_API_RESPONSE_INVALID
        );
    }
}
