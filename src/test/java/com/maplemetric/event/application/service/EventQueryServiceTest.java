package com.maplemetric.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.maplemetric.event.application.result.GetOngoingEventListResult;
import com.maplemetric.event.domain.exception.EventErrorCode;
import com.maplemetric.event.domain.exception.EventException;
import com.maplemetric.event.infrastructure.client.EventClient;
import com.maplemetric.event.infrastructure.client.dto.EventNoticeListResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventQueryServiceTest {

    @Mock
    private EventClient eventClient;

    private EventQueryService eventQueryService;

    @BeforeEach
    void setUp() {
        eventQueryService =
                new EventQueryService(eventClient);
    }

    @Test
    void 진행중이벤트필드를매핑한다() {
        given(eventClient.getOngoingEvents())
                .willReturn(
                        new EventNoticeListResponse(
                                List.of(
                                        createEvent(
                                                "2026-07-20T16:00Z"
                                        )
                                )
                        )
                );

        GetOngoingEventListResult result =
                eventQueryService.getOngoingEvents();

        assertThat(result.events())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.eventId())
                            .isEqualTo(201L);
                    assertThat(event.date())
                            .isEqualTo(
                                    LocalDate.of(2026, 7, 21)
                            );
                    assertThat(event.startDate())
                            .isEqualTo(
                                    LocalDate.of(2026, 7, 21)
                            );
                    assertThat(event.endDate())
                            .isEqualTo(
                                    LocalDate.of(2026, 8, 20)
                            );
                    assertThat(event.status())
                            .isEqualTo("ongoing");
                });

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.source())
                .isEqualTo("NEXON_OPEN_API");

        verify(eventClient).getOngoingEvents();
    }

    @Test
    void 빈이벤트목록은정상빈결과로반환한다() {
        given(eventClient.getOngoingEvents())
                .willReturn(
                        new EventNoticeListResponse(List.of())
                );

        GetOngoingEventListResult result =
                eventQueryService.getOngoingEvents();

        assertThat(result.events()).isEmpty();
        assertThat(result.total()).isZero();
    }

    @Test
    void 이벤트날짜가올바르지않으면응답오류를반환한다() {
        given(eventClient.getOngoingEvents())
                .willReturn(
                        new EventNoticeListResponse(
                                List.of(
                                        createEvent("invalid-date")
                                )
                        )
                );

        EventException exception = catchThrowableOfType(
                () -> eventQueryService.getOngoingEvents(),
                EventException.class
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        EventErrorCode.NEXON_API_RESPONSE_INVALID
                );
    }

    private EventNoticeListResponse.EventNotice createEvent(
            String date
    ) {
        return new EventNoticeListResponse.EventNotice(
                201L,
                "여름 이벤트",
                "https://example.com/201",
                date,
                "2026-07-21T00:00+09:00",
                "2026-08-20T23:59+09:00"
        );
    }
}
