package com.maplemetric.event.presentation.controller;

import com.maplemetric.common.ApiResponse;
import com.maplemetric.event.application.result.GetOngoingEventListResult;
import com.maplemetric.event.application.service.EventQueryService;
import com.maplemetric.event.presentation.code.EventSuccessCode;
import com.maplemetric.event.presentation.dto.GetOngoingEventListResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventQueryService eventQueryService;

    public EventController(
            EventQueryService eventQueryService
    ) {
        this.eventQueryService = eventQueryService;
    }

    @GetMapping
    public ApiResponse<GetOngoingEventListResponse>
    getOngoingEvents() {
        GetOngoingEventListResult result =
                eventQueryService.getOngoingEvents();

        return ApiResponse.ok(
                EventSuccessCode
                        .ONGOING_EVENT_SEARCH_SUCCESS,
                GetOngoingEventListResponse.from(result)
        );
    }
}
