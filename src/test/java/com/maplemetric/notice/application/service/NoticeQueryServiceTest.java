package com.maplemetric.notice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.maplemetric.notice.application.port.out.LoadNoticeListPort;
import com.maplemetric.notice.application.result.GetNoticeListResult;
import com.maplemetric.notice.domain.exception.InvalidNoticeCategoryException;
import com.maplemetric.notice.domain.model.NoticeCategory;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoticeQueryServiceTest {

    @Mock
    private LoadNoticeListPort loadNoticeListPort;

    private NoticeQueryService noticeQueryService;

    @BeforeEach
    void setUp() {
        noticeQueryService =
                new NoticeQueryService(loadNoticeListPort);
    }

    @Test
    void 공지분류와제한을OutputPort에전달한다() {
        given(loadNoticeListPort.loadNotices(
                NoticeCategory.GENERAL,
                20
        )).willReturn(createEmptyResult(
                NoticeCategory.GENERAL
        ));

        given(loadNoticeListPort.loadNotices(
                NoticeCategory.UPDATE,
                10
        )).willReturn(createEmptyResult(
                NoticeCategory.UPDATE
        ));

        given(loadNoticeListPort.loadNotices(
                NoticeCategory.CASHSHOP,
                5
        )).willReturn(createEmptyResult(
                NoticeCategory.CASHSHOP
        ));

        noticeQueryService.getNotices("general", 20);
        noticeQueryService.getNotices("UPDATE", 10);
        noticeQueryService.getNotices("cashshop", 5);

        verify(loadNoticeListPort).loadNotices(
                NoticeCategory.GENERAL,
                20
        );
        verify(loadNoticeListPort).loadNotices(
                NoticeCategory.UPDATE,
                10
        );
        verify(loadNoticeListPort).loadNotices(
                NoticeCategory.CASHSHOP,
                5
        );
    }

    @Test
    void OutputPort결과를그대로반환한다() {
        GetNoticeListResult expected =
                createEmptyResult(
                        NoticeCategory.GENERAL
                );

        given(loadNoticeListPort.loadNotices(
                NoticeCategory.GENERAL,
                20
        )).willReturn(expected);

        GetNoticeListResult result =
                noticeQueryService.getNotices(
                        "general",
                        20
                );

        assertThat(result).isSameAs(expected);
    }

    @Test
    void 지원하지않는공지분류는외부조회를하지않는다() {
        catchThrowableOfType(
                () -> noticeQueryService.getNotices(
                        "event",
                        20
                ),
                InvalidNoticeCategoryException.class
        );

        verifyNoInteractions(loadNoticeListPort);
    }

    private GetNoticeListResult createEmptyResult(
            NoticeCategory category
    ) {
        return GetNoticeListResult.from(
                List.of(),
                category
        );
    }
}
