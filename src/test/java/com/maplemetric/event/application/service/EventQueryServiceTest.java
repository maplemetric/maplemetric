package com.maplemetric.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.maplemetric.event.application.port.out.LoadOngoingEventsPort;
import com.maplemetric.event.application.result.GetOngoingEventListResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventQueryServiceTest {

    @Mock
    private LoadOngoingEventsPort loadOngoingEventsPort;

    private EventQueryService eventQueryService;

    @BeforeEach
    void setUp() {
        eventQueryService =
                new EventQueryService(loadOngoingEventsPort);
    }

    @Test
    void 진행중이벤트조회포트결과를반환한다() {
        GetOngoingEventListResult expected =
                GetOngoingEventListResult.from(List.of());

        given(loadOngoingEventsPort.loadOngoingEvents())
                .willReturn(expected);

        GetOngoingEventListResult result =
                eventQueryService.getOngoingEvents();

        assertThat(result).isSameAs(expected);

        verify(loadOngoingEventsPort)
                .loadOngoingEvents();
    }
}
