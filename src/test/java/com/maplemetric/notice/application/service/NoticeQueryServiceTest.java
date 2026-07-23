package com.maplemetric.notice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.maplemetric.notice.application.result.GetNoticeListResult;
import com.maplemetric.notice.domain.exception.NoticeErrorCode;
import com.maplemetric.notice.domain.exception.NoticeException;
import com.maplemetric.notice.infrastructure.client.NoticeClient;
import com.maplemetric.notice.infrastructure.client.dto.CashshopNoticeItemResponse;
import com.maplemetric.notice.infrastructure.client.dto.CashshopNoticeListResponse;
import com.maplemetric.notice.infrastructure.client.dto.NoticeItemResponse;
import com.maplemetric.notice.infrastructure.client.dto.NoticeListResponse;
import com.maplemetric.notice.infrastructure.client.dto.UpdateNoticeListResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoticeQueryServiceTest {

    @Mock
    private NoticeClient noticeClient;

    private NoticeQueryService noticeQueryService;

    @BeforeEach
    void setUp() {
        noticeQueryService =
                new NoticeQueryService(noticeClient);
    }

    @Test
    void 공지분류에맞는외부API를조회한다() {
        given(noticeClient.getNotices())
                .willReturn(new NoticeListResponse(List.of()));

        given(noticeClient.getUpdateNotices())
                .willReturn(
                        new UpdateNoticeListResponse(List.of())
                );

        given(noticeClient.getCashshopNotices())
                .willReturn(
                        new CashshopNoticeListResponse(List.of())
                );

        noticeQueryService.getNotices("general", 20);
        noticeQueryService.getNotices("UPDATE", 20);
        noticeQueryService.getNotices("cashshop", 20);

        verify(noticeClient).getNotices();
        verify(noticeClient).getUpdateNotices();
        verify(noticeClient).getCashshopNotices();
    }

    @Test
    void 공지목록을요청제한만큼매핑한다() {
        given(noticeClient.getNotices())
                .willReturn(
                        new NoticeListResponse(
                                List.of(
                                        createNoticeItem(
                                                101L,
                                                "첫 번째 공지"
                                        ),
                                        createNoticeItem(
                                                102L,
                                                "두 번째 공지"
                                        )
                                )
                        )
                );

        GetNoticeListResult result =
                noticeQueryService.getNotices(
                        "general",
                        1
                );

        assertThat(result.notices())
                .singleElement()
                .satisfies(notice -> {
                    assertThat(notice.noticeId())
                            .isEqualTo(101L);
                    assertThat(notice.category())
                            .isEqualTo("general");
                    assertThat(notice.date())
                            .isEqualTo(
                                    LocalDate.of(2026, 7, 21)
                            );
                });

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.category())
                .isEqualTo("general");
        assertThat(result.source())
                .isEqualTo("NEXON_OPEN_API");
    }

    @Test
    void 캐시샵판매정보를매핑한다() {
        given(noticeClient.getCashshopNotices())
                .willReturn(
                        new CashshopNoticeListResponse(
                                List.of(
                                        new CashshopNoticeItemResponse(
                                                103L,
                                                "캐시샵 공지",
                                                "https://example.com/103",
                                                "2026-07-23T12:00+09:00",
                                                "2026-07-24T00:00+09:00",
                                                "2026-08-01T23:59+09:00",
                                                "true"
                                        )
                                )
                        )
                );

        GetNoticeListResult result =
                noticeQueryService.getNotices(
                        "cashshop",
                        20
                );

        assertThat(result.notices())
                .singleElement()
                .satisfies(notice -> {
                    assertThat(notice.saleStartDate())
                            .isEqualTo(
                                    LocalDate.of(2026, 7, 24)
                            );
                    assertThat(notice.saleEndDate())
                            .isEqualTo(
                                    LocalDate.of(2026, 8, 1)
                            );
                    assertThat(notice.ongoing()).isTrue();
                });
    }

    @Test
    void 지원하지않는공지분류는입력값오류를반환한다() {
        NoticeException exception = catchThrowableOfType(
                () -> noticeQueryService.getNotices(
                        "event",
                        20
                ),
                NoticeException.class
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        NoticeErrorCode.INVALID_CATEGORY
                );

        verifyNoInteractions(noticeClient);
    }

    @Test
    void 공지날짜가올바르지않으면응답오류를반환한다() {
        given(noticeClient.getUpdateNotices())
                .willReturn(
                        new UpdateNoticeListResponse(
                                List.of(
                                        new NoticeItemResponse(
                                                102L,
                                                "업데이트 공지",
                                                "https://example.com/102",
                                                "invalid-date"
                                        )
                                )
                        )
                );

        NoticeException exception = catchThrowableOfType(
                () -> noticeQueryService.getNotices(
                        "update",
                        20
                ),
                NoticeException.class
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        NoticeErrorCode.NEXON_API_RESPONSE_INVALID
                );
    }

    @Test
    void 캐시샵상시판매값이올바르지않으면응답오류를반환한다() {
        given(noticeClient.getCashshopNotices())
                .willReturn(
                        new CashshopNoticeListResponse(
                                List.of(
                                        new CashshopNoticeItemResponse(
                                                103L,
                                                "캐시샵 공지",
                                                "https://example.com/103",
                                                "2026-07-23T12:00+09:00",
                                                null,
                                                null,
                                                "unknown"
                                        )
                                )
                        )
                );

        NoticeException exception = catchThrowableOfType(
                () -> noticeQueryService.getNotices(
                        "cashshop",
                        20
                ),
                NoticeException.class
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        NoticeErrorCode.NEXON_API_RESPONSE_INVALID
                );
    }

    private NoticeItemResponse createNoticeItem(
            long noticeId,
            String title
    ) {
        return new NoticeItemResponse(
                noticeId,
                title,
                "https://example.com/" + noticeId,
                "2026-07-21T10:00+09:00"
        );
    }
}
