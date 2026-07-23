package com.maplemetric.notice.infrastructure.client.nexon;

import com.maplemetric.notice.application.exception.NoticeException;
import com.maplemetric.notice.application.exception.NoticeFailure;
import com.maplemetric.notice.application.port.out.LoadNoticeListPort;
import com.maplemetric.notice.application.result.GetNoticeListResult;
import com.maplemetric.notice.domain.model.NoticeCategory;
import com.maplemetric.notice.infrastructure.client.nexon.response.CashshopNoticeItemResponse;
import com.maplemetric.notice.infrastructure.client.nexon.response.NoticeItemResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class NexonNoticeAdapter
        implements LoadNoticeListPort {

    private static final ZoneId KOREA_ZONE_ID =
            ZoneId.of("Asia/Seoul");

    private final NexonNoticeClient nexonNoticeClient;

    NexonNoticeAdapter(
            NexonNoticeClient nexonNoticeClient
    ) {
        this.nexonNoticeClient = nexonNoticeClient;
    }

    @Override
    public GetNoticeListResult loadNotices(
            NoticeCategory category,
            int limit
    ) {
        List<GetNoticeListResult.Notice> notices =
                switch (category) {
                    case GENERAL -> mapNotices(
                            nexonNoticeClient
                                    .getNotices()
                                    .notice(),
                            category,
                            limit
                    );
                    case UPDATE -> mapNotices(
                            nexonNoticeClient
                                    .getUpdateNotices()
                                    .updateNotice(),
                            category,
                            limit
                    );
                    case CASHSHOP -> mapCashshopNotices(
                            nexonNoticeClient
                                    .getCashshopNotices()
                                    .cashshopNotice(),
                            limit
                    );
                };

        return GetNoticeListResult.from(
                notices,
                category
        );
    }

    private List<GetNoticeListResult.Notice> mapNotices(
            List<NoticeItemResponse> response,
            NoticeCategory category,
            int limit
    ) {
        return response.stream()
                .limit(limit)
                .map(item -> GetNoticeListResult.Notice.of(
                        item.noticeId(),
                        category,
                        item.title(),
                        item.url(),
                        parseDate(item.date()),
                        null,
                        null,
                        null
                ))
                .toList();
    }

    private List<GetNoticeListResult.Notice> mapCashshopNotices(
            List<CashshopNoticeItemResponse> response,
            int limit
    ) {
        return response.stream()
                .limit(limit)
                .map(item -> GetNoticeListResult.Notice.of(
                        item.noticeId(),
                        NoticeCategory.CASHSHOP,
                        item.title(),
                        item.url(),
                        parseDate(item.date()),
                        parseDate(item.dateSaleStart()),
                        parseDate(item.dateSaleEnd()),
                        parseBoolean(item.ongoingFlag())
                ))
                .toList();
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value)
                    .atZoneSameInstant(KOREA_ZONE_ID)
                    .toLocalDate();
        } catch (DateTimeParseException exception) {
            throw new NoticeException(
                    NoticeFailure.RESPONSE_INVALID
            );
        }
    }

    private Boolean parseBoolean(String value) {
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
                NoticeFailure.RESPONSE_INVALID
        );
    }
}
