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
    void 생성실행은건수를생성건너뜀실패로나누어반환한다() throws Exception {
        given(generateUseCase.generate()).willReturn(
                new GenerateStatisticsInsightOutcome(12, 3, 1)
        );

        mockMvc.perform(post(INSIGHT_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.code")
                                .value("STATISTICS_INSIGHT_GENERATED")
                )
                .andExpect(jsonPath("$.data.generated").value(12))
                .andExpect(jsonPath("$.data.skipped").value(3))
                .andExpect(jsonPath("$.data.failed").value(1));
    }

    /**
     * 한 번의 요청이 유료 호출을 한 번만 일으키는지 확인한다.
     */
    @Test
    void 요청한번은생성을한번만위임한다() throws Exception {
        given(generateUseCase.generate()).willReturn(
                new GenerateStatisticsInsightOutcome(0, 0, 0)
        );

        mockMvc.perform(post(INSIGHT_PATH))
                .andExpect(status().isOk());

        verify(generateUseCase, times(1)).generate();
    }

    /**
     * 생성 건수가 실행 한도와 같으면 남은 대상이 있다는 뜻이다.
     *
     * 소비자가 재호출 여부를 판단하는 근거가 generated 값이므로 그 값이
     * 응답에 그대로 실리는지 고정한다.
     */
    @Test
    void 한도까지생성하면그건수를그대로반환한다() throws Exception {
        given(generateUseCase.generate()).willReturn(
                new GenerateStatisticsInsightOutcome(70, 0, 0)
        );

        mockMvc.perform(post(INSIGHT_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generated").value(70))
                .andExpect(jsonPath("$.data.skipped").value(0))
                .andExpect(jsonPath("$.data.failed").value(0));
    }

    /**
     * 대상별 실패는 격리된다. 일부가 실패해도 요청 자체는 성공이다.
     */
    @Test
    void 일부대상이실패해도요청은성공으로응답한다() throws Exception {
        given(generateUseCase.generate()).willReturn(
                new GenerateStatisticsInsightOutcome(5, 0, 4)
        );

        mockMvc.perform(post(INSIGHT_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.generated").value(5))
                .andExpect(jsonPath("$.data.failed").value(4));
    }
}
