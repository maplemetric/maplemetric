package com.maplemetric.internal.presentation.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/**
 * Backfill 대상 기간이다.
 *
 * 미래 기준일은 랭킹이 존재하지 않아 호출해봐야 Quota만 쓴다. 역순 범위는 기준일이
 * 하나도 만들어지지 않아 아무 일도 하지 않는 Job이 된다. 둘 다 요청 단계에서 막는다.
 */
public record CreateOverallRankingBackfillHttpRequest(
        @NotNull
        @PastOrPresent
        LocalDate from,

        @NotNull
        @PastOrPresent
        LocalDate to
) {

    @AssertTrue(
            message = "시작 기준일은 종료 기준일보다 뒤일 수 없습니다."
    )
    public boolean isRangeOrdered() {
        if (from == null || to == null) {
            return true;
        }

        return !from.isAfter(to);
    }
}
