package com.maplemetric.notice.presentation.dto;

import com.maplemetric.notice.application.result.GetNoticeListResult;
import java.time.LocalDate;
import java.util.List;

public record GetNoticeListResponse(
        List<Notice> notices,
        int total,
        String category,
        String source
) {

    public static GetNoticeListResponse from(
            GetNoticeListResult result
    ) {
        List<Notice> notices = result.notices()
                .stream()
                .map(item -> new Notice(
                        item.noticeId(),
                        item.category(),
                        item.title(),
                        item.url(),
                        item.date(),
                        item.saleStartDate(),
                        item.saleEndDate(),
                        item.ongoing()
                ))
                .toList();

        return new GetNoticeListResponse(
                notices,
                result.total(),
                result.category(),
                result.source()
        );
    }

    public record Notice(
            Long noticeId,
            String category,
            String title,
            String url,
            LocalDate date,
            LocalDate saleStartDate,
            LocalDate saleEndDate,
            Boolean ongoing
    ) {
    }
}
