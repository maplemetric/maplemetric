package com.maplemetric.notice.application.result;

import com.maplemetric.notice.domain.NoticeCategory;
import com.maplemetric.notice.domain.exception.NoticeErrorCode;
import com.maplemetric.notice.domain.exception.NoticeException;
import com.maplemetric.notice.infrastructure.client.dto.CashshopNoticeItemResponse;
import com.maplemetric.notice.infrastructure.client.dto.CashshopNoticeListResponse;
import com.maplemetric.notice.infrastructure.client.dto.NoticeItemResponse;
import com.maplemetric.notice.infrastructure.client.dto.NoticeListResponse;
import com.maplemetric.notice.infrastructure.client.dto.UpdateNoticeListResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.util.StringUtils;

public record GetNoticeListResult(
        List<Notice> notices,
        int total,
        String category,
        String source
) {

    private static final String SOURCE =
            "NEXON_OPEN_API";

    public static GetNoticeListResult from(
            NoticeListResponse response,
            int limit
    ) {
        List<Notice> notices = response.notice()
                .stream()
                .limit(limit)
                .map(item -> createNotice(
                        item,
                        NoticeCategory.GENERAL
                ))
                .toList();

        return createResult(
                notices,
                NoticeCategory.GENERAL
        );
    }

    public static GetNoticeListResult from(
            UpdateNoticeListResponse response,
            int limit
    ) {
        List<Notice> notices = response.updateNotice()
                .stream()
                .limit(limit)
                .map(item -> createNotice(
                        item,
                        NoticeCategory.UPDATE
                ))
                .toList();

        return createResult(
                notices,
                NoticeCategory.UPDATE
        );
    }

    public static GetNoticeListResult from(
            CashshopNoticeListResponse response,
            int limit
    ) {
        List<Notice> notices = response.cashshopNotice()
                .stream()
                .limit(limit)
                .map(item -> new Notice(
                        item.noticeId(),
                        NoticeCategory.CASHSHOP.getValue(),
                        item.title(),
                        item.url(),
                        parseDate(item.date()),
                        parseDate(item.dateSaleStart()),
                        parseDate(item.dateSaleEnd()),
                        parseBoolean(item.ongoingFlag())
                ))
                .toList();

        return createResult(
                notices,
                NoticeCategory.CASHSHOP
        );
    }

    private static GetNoticeListResult createResult(
            List<Notice> notices,
            NoticeCategory category
    ) {
        return new GetNoticeListResult(
                notices,
                notices.size(),
                category.getValue(),
                SOURCE
        );
    }

    private static Notice createNotice(
            NoticeItemResponse item,
            NoticeCategory category
    ) {
        return new Notice(
                item.noticeId(),
                category.getValue(),
                item.title(),
                item.url(),
                parseDate(item.date()),
                null,
                null,
                null
        );
    }

    private static LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value)
                    .toLocalDate();
        } catch (DateTimeParseException exception) {
            throw new NoticeException(
                    NoticeErrorCode.NEXON_API_RESPONSE_INVALID
            );
        }
    }

    private static Boolean parseBoolean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        if ("true".equalsIgnoreCase(value)) {
            return true;
        }

        if ("false".equalsIgnoreCase(value)) {
            return false;
        }

        throw new NoticeException(
                NoticeErrorCode.NEXON_API_RESPONSE_INVALID
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
