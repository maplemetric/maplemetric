package com.maplemetric.statistics.application.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

/**
 * 직업·월드 History 기간 요청이다.
 *
 * Preset과 Custom 범위는 같은 규칙을 쓰므로 대상별로 복제하지 않고 이 타입을 공유한다.
 * 잘못된 요청에 던질 예외는 대상마다 ErrorCode가 달라 호출자가 Supplier로 넘긴다.
 */
record StatisticsHistoryRequest(
        HistoryPreset preset,
        LocalDate from,
        LocalDate to
) {

    private static final int MAX_HISTORY_DAYS = 365;

    static StatisticsHistoryRequest from(
            String range,
            LocalDate from,
            LocalDate to,
            LocalDate today,
            Supplier<RuntimeException> onInvalid
    ) {
        if (range != null) {
            if (range.isBlank() || from != null || to != null) {
                throw onInvalid.get();
            }

            return new StatisticsHistoryRequest(
                    HistoryPreset.from(range, onInvalid),
                    null,
                    null
            );
        }

        if (from == null && to == null) {
            return new StatisticsHistoryRequest(
                    HistoryPreset.SEVEN_DAYS,
                    null,
                    null
            );
        }

        validateCustomRange(from, to, today, onInvalid);

        return new StatisticsHistoryRequest(null, from, to);
    }

    private static void validateCustomRange(
            LocalDate from,
            LocalDate to,
            LocalDate today,
            Supplier<RuntimeException> onInvalid
    ) {
        if (from == null || to == null || from.isAfter(to)) {
            throw onInvalid.get();
        }

        long inclusiveDays = ChronoUnit.DAYS.between(from, to) + 1;

        if (inclusiveDays > MAX_HISTORY_DAYS || to.isAfter(today)) {
            throw onInvalid.get();
        }
    }

    HistoryPeriod resolve(LocalDate latestAsOf) {
        if (preset == null) {
            return new HistoryPeriod(from, to);
        }

        return new HistoryPeriod(
                latestAsOf.minusDays(preset.days() - 1L),
                latestAsOf
        );
    }

    String presetCode() {
        return preset == null ? null : preset.code();
    }

    record HistoryPeriod(
            LocalDate from,
            LocalDate to
    ) {
    }

    enum HistoryPreset {
        SEVEN_DAYS("7D", 7),
        THIRTY_DAYS("30D", 30),
        NINETY_DAYS("90D", 90),
        ONE_YEAR("1Y", 365);

        private final String code;
        private final int days;

        HistoryPreset(String code, int days) {
            this.code = code;
            this.days = days;
        }

        private static HistoryPreset from(
                String code,
                Supplier<RuntimeException> onInvalid
        ) {
            for (HistoryPreset preset : values()) {
                if (preset.code.equals(code)) {
                    return preset;
                }
            }

            throw onInvalid.get();
        }

        private String code() {
            return code;
        }

        private int days() {
            return days;
        }
    }
}
