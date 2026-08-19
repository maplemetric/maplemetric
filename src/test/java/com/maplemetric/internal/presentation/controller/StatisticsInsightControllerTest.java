package com.maplemetric.internal.presentation.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maplemetric.analysis.api.GenerateStatisticsInsightOutcome;
import com.maplemetric.analysis.api.GenerateStatisticsInsightUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StatisticsInsightController.class)
class StatisticsInsightControllerTest {

    private static final String INSIGHT_PATH =
            "/internal/v1/insights/statistics";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenerateStatisticsInsightUseCase generateUseCase;

    @Test
    void 생성실행결과를항목별로나누어반환한다() throws Exception {
        given(generateUseCase.generate()).willReturn(
                new GenerateStatisticsInsightOutcome(12, 3, 2, 1, false)
        );

        mockMvc.perform(post(INSIGHT_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.code")
                                .value("STATISTICS_INSIGHT_GENERATED")
                )
                .andExpect(jsonPath("$.data.generated").value(12))
                .andExpect(jsonPath("$.data.alreadyExists").value(3))
                .andExpect(jsonPath("$.data.noHistory").value(2))
                .andExpect(jsonPath("$.data.failed").value(1))
                .andExpect(jsonPath("$.data.hasMore").value(false));
    }

    /**
     * 건너뛴 이유가 합쳐지지 않는지 고정한다.
     *
     * 이미 있는 것은 넘어가도 되지만 기준일이 없는 것은 수집이 비었다는 뜻이라
     * 확인이 필요하다. 하나로 합치면 조치가 필요한 쪽이 응답에서 사라진다.
     */
    @Test
    void 건너뛴이유를합치지않는다() throws Exception {
        given(generateUseCase.generate()).willReturn(
                new GenerateStatisticsInsightOutcome(0, 3, 2, 0, false)
        );

        mockMvc.perform(post(INSIGHT_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alreadyExists").value(3))
                .andExpect(jsonPath("$.data.noHistory").value(2))
                .andExpect(jsonPath("$.data.skipped").doesNotExist());
    }

    /**
     * 한 번의 요청이 유료 호출을 한 번만 일으키는지 확인한다.
     */
    @Test
    void 요청한번은생성을한번만위임한다() throws Exception {
        given(generateUseCase.generate()).willReturn(
                new GenerateStatisticsInsightOutcome(0, 0, 0, 0, false)
        );

        mockMvc.perform(post(INSIGHT_PATH))
                .andExpect(status().isOk());

        verify(generateUseCase, times(1)).generate();
    }

    /**
     * 재호출 여부를 판단하는 근거가 응답에 그대로 실리는지 고정한다.
     *
     * 생성 건수가 한도와 같은 것만으로는 남았는지 알 수 없다. 정확히 마지막까지
     * 만들고 끝난 경우와 건수가 같기 때문이다.
     */
    @Test
    void 남은대상이있으면그사실을반환한다() throws Exception {
        given(generateUseCase.generate()).willReturn(
                new GenerateStatisticsInsightOutcome(70, 0, 0, 0, true)
        );

        mockMvc.perform(post(INSIGHT_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generated").value(70))
                .andExpect(jsonPath("$.data.hasMore").value(true));
    }

    /**
     * 대상별 실패는 격리된다. 일부가 실패해도 요청 자체는 성공이다.
     */
    @Test
    void 일부대상이실패해도요청은성공으로응답한다() throws Exception {
        given(generateUseCase.generate()).willReturn(
                new GenerateStatisticsInsightOutcome(5, 0, 0, 4, false)
        );

        mockMvc.perform(post(INSIGHT_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.generated").value(5))
                .andExpect(jsonPath("$.data.failed").value(4));
    }
}
