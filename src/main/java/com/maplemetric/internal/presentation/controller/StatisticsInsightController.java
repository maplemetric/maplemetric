package com.maplemetric.internal.presentation.controller;

import com.maplemetric.analysis.api.GenerateStatisticsInsightOutcome;
import com.maplemetric.analysis.api.GenerateStatisticsInsightUseCase;
import com.maplemetric.common.ApiResponse;
import com.maplemetric.internal.presentation.code.InternalSuccessCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 통계 설명 생성을 수동으로 실행한다.
 *
 * Scheduler를 두지 않는다. 유료 호출이라 승인 없이 자동으로 도는 경로가 있으면
 * 안 되고, 이 저장소에는 상시 실행되는 서버도 없다.
 *
 * 한 번의 실행은 설정된 한도까지만 생성한다. 응답의 hasMore가 참이면 한도 때문에
 * 멈춘 것이므로 다시 호출한다. 생성 건수를 한도와 비교해 짐작하지 않는다.
 */
@RestController
@RequestMapping("/internal/v1/insights/statistics")
public class StatisticsInsightController {

    private final GenerateStatisticsInsightUseCase generateUseCase;

    public StatisticsInsightController(
            GenerateStatisticsInsightUseCase generateUseCase
    ) {
        this.generateUseCase = generateUseCase;
    }

    @PostMapping
    public ApiResponse<GenerateStatisticsInsightOutcome>
            generateStatisticsInsight() {
        return ApiResponse.ok(
                InternalSuccessCode.STATISTICS_INSIGHT_GENERATED,
                generateUseCase.generate()
        );
    }
}
