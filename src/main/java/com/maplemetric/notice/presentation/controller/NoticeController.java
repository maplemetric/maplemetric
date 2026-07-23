package com.maplemetric.notice.presentation.controller;

import com.maplemetric.common.ApiResponse;
import com.maplemetric.notice.application.result.GetNoticeListResult;
import com.maplemetric.notice.application.service.NoticeQueryService;
import com.maplemetric.notice.presentation.code.NoticeSuccessCode;
import com.maplemetric.notice.presentation.dto.GetNoticeListResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController {

    private final NoticeQueryService noticeQueryService;

    public NoticeController(
            NoticeQueryService noticeQueryService
    ) {
        this.noticeQueryService = noticeQueryService;
    }

    @GetMapping
    public ApiResponse<GetNoticeListResponse> getNotices(
            @RequestParam(defaultValue = "general")
            String category,
            @RequestParam(defaultValue = "20")
            @Min(1) @Max(20)
            int limit
    ) {
        GetNoticeListResult result =
                noticeQueryService.getNotices(
                        category,
                        limit
                );

        return ApiResponse.ok(
                NoticeSuccessCode.NOTICE_SEARCH_SUCCESS,
                GetNoticeListResponse.from(result)
        );
    }
}
