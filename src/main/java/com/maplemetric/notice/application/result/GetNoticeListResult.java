package com.maplemetric.notice.application.result;

import com.maplemetric.notice.domain.model.NoticeCategory;
import java.time.LocalDate;
import java.util.List;

public record GetNoticeListResult(
        List<Notice> notices,
        int total,
        String category,
        String source
) {

    private static final String SOURCE =
            "NEXON_OPEN_API";

    public static GetNoticeListResult from(
            List<Notice> notices,
            NoticeCategory category
    ) {
        List<Notice> copiedNotices =
                List.copyOf(notices);

        return new GetNoticeListResult(
                copiedNotices,
                copiedNotices.size(),
                category.getValue(),
                SOURCE
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

        public static Notice of(
                Long noticeId,
                NoticeCategory category,
                String title,
                String url,
                LocalDate date,
                LocalDate saleStartDate,
                LocalDate saleEndDate,
                Boolean ongoing
        ) {
            return new Notice(
                    noticeId,
                    category.getValue(),
                    title,
                    url,
                    date,
                    saleStartDate,
                    saleEndDate,
                    ongoing
            );
        }
    }
}
