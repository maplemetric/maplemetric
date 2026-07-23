package com.maplemetric.notice.infrastructure.client.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.maplemetric.notice.application.exception.NoticeException;
import com.maplemetric.notice.application.exception.NoticeFailure;
import com.maplemetric.notice.application.result.GetNoticeListResult;
import com.maplemetric.notice.domain.model.NoticeCategory;
import com.maplemetric.notice.infrastructure.client.nexon.response.CashshopNoticeItemResponse;
import com.maplemetric.notice.infrastructure.client.nexon.response.CashshopNoticeListResponse;
import com.maplemetric.notice.infrastructure.client.nexon.response.NoticeItemResponse;
import com.maplemetric.notice.infrastructure.client.nexon.response.NoticeListResponse;
import com.maplemetric.notice.infrastructure.client.nexon.response.UpdateNoticeListResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NexonNoticeAdapterTest {

    @Mock
    private NexonNoticeClient nexonNoticeClient;

    private NexonNoticeAdapter nexonNoticeAdapter;

    @BeforeEach
    void setUp() {
        nexonNoticeAdapter =
                new NexonNoticeAdapter(
                        nexonNoticeClient
                );
    }

    @Test
    void 공지분류에맞는Nexon응답을조회한다() {
        given(nexonNoticeClient.getNotices())
                .willReturn(
                        new NoticeListResponse(List.of())
                );
        given(nexonNoticeClient.getUpdateNotices())
                .willReturn(
                        new UpdateNoticeListResponse(
                                List.of()
                        )
                );
        given(nexonNoticeClient.getCashshopNotices())
                .willReturn(
                        new CashshopNoticeListResponse(
                                List.of()
                        )
                );

        nexonNoticeAdapter.loadNotices(
                NoticeCategory.GENERAL,
                20
        );
        nexonNoticeAdapter.loadNotices(
                NoticeCategory.UPDATE,
                20
        );
        nexonNoticeAdapter.loadNotices(
                NoticeCategory.CASHSHOP,
                20
        );

        verify(nexonNoticeClient).getNotices();
        verify(nexonNoticeClient).getUpdateNotices();
        verify(nexonNoticeClient).getCashshopNotices();
    }

    @Test
    void 공지목록을요청제한만큼매핑한다() {
        given(nexonNoticeClient.getNotices())
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
                nexonNoticeAdapter.loadNotices(
                        NoticeCategory.GENERAL,
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
        given(nexonNoticeClient.getCashshopNotices())
                .willReturn(
                        new CashshopNoticeListResponse(
                                List.of(
                                        createCashshopNotice(
                                                "true"
                                        )
                                )
                        )
                );

        GetNoticeListResult result =
                nexonNoticeAdapter.loadNotices(
                        NoticeCategory.CASHSHOP,
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
    void 공지날짜가올바르지않으면응답오류를반환한다() {
        given(nexonNoticeClient.getUpdateNotices())
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
                () -> nexonNoticeAdapter.loadNotices(
                        NoticeCategory.UPDATE,
                        20
                ),
                NoticeException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(
                        NoticeFailure.RESPONSE_INVALID
                );
    }

    @Test
    void 캐시샵상시판매값이올바르지않으면응답오류를반환한다() {
        given(nexonNoticeClient.getCashshopNotices())
                .willReturn(
                        new CashshopNoticeListResponse(
                                List.of(
                                        createCashshopNotice(
                                                "unknown"
                                        )
                                )
                        )
                );

        NoticeException exception = catchThrowableOfType(
                () -> nexonNoticeAdapter.loadNotices(
                        NoticeCategory.CASHSHOP,
                        20
                ),
                NoticeException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(
                        NoticeFailure.RESPONSE_INVALID
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
                "2026-07-20T16:00Z"
        );
    }

    private CashshopNoticeItemResponse createCashshopNotice(
            String ongoingFlag
    ) {
        return new CashshopNoticeItemResponse(
                103L,
                "캐시샵 공지",
                "https://example.com/103",
                "2026-07-23T12:00+09:00",
                "2026-07-24T00:00+09:00",
                "2026-08-01T23:59+09:00",
                ongoingFlag
        );
    }
}
