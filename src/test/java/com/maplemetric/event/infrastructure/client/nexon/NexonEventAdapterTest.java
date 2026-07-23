package com.maplemetric.event.infrastructure.client.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.maplemetric.event.application.exception.EventException;
import com.maplemetric.event.application.exception.EventFailure;
import com.maplemetric.event.application.result.GetOngoingEventListResult;
import com.maplemetric.event.infrastructure.client.nexon.response.EventNoticeListResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NexonEventAdapterTest {

    @Mock
    private NexonEventClient nexonEventClient;

    private NexonEventAdapter nexonEventAdapter;

    @BeforeEach
    void setUp() {
        nexonEventAdapter =
                new NexonEventAdapter(nexonEventClient);
    }

    @Test
    void 진행중이벤트필드를애플리케이션결과로변환한다() {
        given(nexonEventClient.getOngoingEvents())
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
                nexonEventAdapter.loadOngoingEvents();

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

        verify(nexonEventClient).getOngoingEvents();
    }

    @Test
    void 빈이벤트목록은정상빈결과로변환한다() {
        given(nexonEventClient.getOngoingEvents())
                .willReturn(
                        new EventNoticeListResponse(List.of())
                );

        GetOngoingEventListResult result =
                nexonEventAdapter.loadOngoingEvents();

        assertThat(result.events()).isEmpty();
        assertThat(result.total()).isZero();
    }

    @Test
    void 이벤트날짜가올바르지않으면응답오류로변환한다() {
        given(nexonEventClient.getOngoingEvents())
                .willReturn(
                        new EventNoticeListResponse(
                                List.of(
                                        createEvent("invalid-date")
                                )
                        )
                );

        EventException exception = catchThrowableOfType(
                () -> nexonEventAdapter
                        .loadOngoingEvents(),
                EventException.class
        );

        assertThat(exception.getFailure())
                .isEqualTo(EventFailure.RESPONSE_INVALID);
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
