package com.maplemetric.notice.presentation.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maplemetric.notice.application.result.GetNoticeListResult;
import com.maplemetric.notice.application.service.NoticeQueryService;
import com.maplemetric.notice.domain.exception.NoticeErrorCode;
import com.maplemetric.notice.domain.exception.NoticeException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NoticeController.class)
class NoticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoticeQueryService noticeQueryService;

    @Test
    void 기본분류와제한으로공지목록을반환한다()
            throws Exception {
        given(noticeQueryService.getNotices(
                "general",
                20
        )).willReturn(createNoticeResult());

        mockMvc.perform(
                        get("/api/v1/notices")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.code")
                                .value("NOTICE_SEARCH_SUCCESS")
                )
                .andExpect(
                        jsonPath("$.data.notices[0].noticeId")
                                .value(101)
                )
                .andExpect(
                        jsonPath("$.data.notices[0].category")
                                .value("general")
                )
                .andExpect(
                        jsonPath("$.data.notices[0].date")
                                .value("2026-07-21")
                )
                .andExpect(
                        jsonPath(
                                "$.data.notices[0].saleStartDate"
                        ).doesNotExist()
                )
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(
                        jsonPath("$.data.category")
                                .value("general")
                )
                .andExpect(
                        jsonPath("$.data.source")
                                .value("NEXON_OPEN_API")
                );

        verify(noticeQueryService).getNotices(
                "general",
                20
        );
    }

    @Test
    void 캐시샵공지판매정보를반환한다()
            throws Exception {
        given(noticeQueryService.getNotices(
                "cashshop",
                5
        )).willReturn(createCashshopResult());

        mockMvc.perform(
                        get("/api/v1/notices")
                                .param(
                                        "category",
                                        "cashshop"
                                )
                                .param("limit", "5")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.data.notices[0].saleStartDate"
                        ).value("2026-07-24")
                )
                .andExpect(
                        jsonPath(
                                "$.data.notices[0].saleEndDate"
                        ).value("2026-08-01")
                )
                .andExpect(
                        jsonPath("$.data.notices[0].ongoing")
                                .value(true)
                );

        verify(noticeQueryService).getNotices(
                "cashshop",
                5
        );
    }

    @Test
    void 제한이범위를벗어나면입력값오류를반환한다()
            throws Exception {
        mockMvc.perform(
                        get("/api/v1/notices")
                                .param("limit", "21")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.code")
                                .value("GLOBAL_001")
                );

        verifyNoInteractions(noticeQueryService);
    }

    @Test
    void 지원하지않는분류는입력값오류를반환한다()
            throws Exception {
        given(noticeQueryService.getNotices(
                "event",
                20
        )).willThrow(
                new NoticeException(
                        NoticeErrorCode.INVALID_CATEGORY
                )
        );

        mockMvc.perform(
                        get("/api/v1/notices")
                                .param("category", "event")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.code")
                                .value("NOTICE_001")
                );

        verify(noticeQueryService).getNotices(
                "event",
                20
        );
    }

    private GetNoticeListResult createNoticeResult() {
        return new GetNoticeListResult(
                List.of(
                        new GetNoticeListResult.Notice(
                                101L,
                                "general",
                                "일반 공지",
                                "https://example.com/101",
                                LocalDate.of(2026, 7, 21),
                                null,
                                null,
                                null
                        )
                ),
                1,
                "general",
                "NEXON_OPEN_API"
        );
    }

    private GetNoticeListResult createCashshopResult() {
        return new GetNoticeListResult(
                List.of(
                        new GetNoticeListResult.Notice(
                                103L,
                                "cashshop",
                                "캐시샵 공지",
                                "https://example.com/103",
                                LocalDate.of(2026, 7, 23),
                                LocalDate.of(2026, 7, 24),
                                LocalDate.of(2026, 8, 1),
                                true
                        )
                ),
                1,
                "cashshop",
                "NEXON_OPEN_API"
        );
    }
}
