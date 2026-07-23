package com.maplemetric.event.presentation.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maplemetric.event.application.exception.EventException;
import com.maplemetric.event.application.exception.EventFailure;
import com.maplemetric.event.application.result.GetOngoingEventListResult;
import com.maplemetric.event.application.service.EventQueryService;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventQueryService eventQueryService;

    @ParameterizedTest
    @MethodSource("eventFailures")
    void 이벤트조회오류를HTTP응답으로변환한다(
            EventFailure failure,
            int expectedStatus,
            String expectedCode
    ) throws Exception {
        given(eventQueryService.getOngoingEvents())
                .willThrow(new EventException(failure));

        mockMvc.perform(
                        get("/api/v1/events")
                )
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    @Test
    void 진행중이벤트목록을반환한다()
            throws Exception {
        given(eventQueryService.getOngoingEvents())
                .willReturn(createEventResult());

        mockMvc.perform(
                        get("/api/v1/events")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "ONGOING_EVENT_SEARCH_SUCCESS"
                                )
                )
                .andExpect(
                        jsonPath("$.data.events[0].eventId")
                                .value(201)
                )
                .andExpect(
                        jsonPath("$.data.events[0].title")
                                .value("여름 이벤트")
                )
                .andExpect(
                        jsonPath("$.data.events[0].date")
                                .value("2026-07-20")
                )
                .andExpect(
                        jsonPath("$.data.events[0].startDate")
                                .value("2026-07-21")
                )
                .andExpect(
                        jsonPath("$.data.events[0].endDate")
                                .value("2026-08-20")
                )
                .andExpect(
                        jsonPath("$.data.events[0].status")
                                .value("ongoing")
                )
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(
                        jsonPath("$.data.source")
                                .value("NEXON_OPEN_API")
                );

        verify(eventQueryService).getOngoingEvents();
    }

    private GetOngoingEventListResult createEventResult() {
        return new GetOngoingEventListResult(
                List.of(
                        new GetOngoingEventListResult.Event(
                                201L,
                                "여름 이벤트",
                                "https://example.com/201",
                                LocalDate.of(2026, 7, 20),
                                LocalDate.of(2026, 7, 21),
                                LocalDate.of(2026, 8, 20),
                                "ongoing"
                        )
                ),
                1,
                "NEXON_OPEN_API"
        );
    }

    private static Stream<Arguments> eventFailures() {
        return Stream.of(
                Arguments.of(
                        EventFailure.CLIENT_ERROR,
                        502,
                        "EVENT_001"
                ),
                Arguments.of(
                        EventFailure.SERVER_ERROR,
                        502,
                        "EVENT_002"
                ),
                Arguments.of(
                        EventFailure.TIMEOUT,
                        504,
                        "EVENT_003"
                ),
                Arguments.of(
                        EventFailure.RESPONSE_INVALID,
                        502,
                        "EVENT_004"
                )
        );
    }
}
