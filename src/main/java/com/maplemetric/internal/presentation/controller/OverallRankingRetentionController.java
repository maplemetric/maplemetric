package com.maplemetric.internal.presentation.controller;

import com.maplemetric.common.ApiResponse;
import com.maplemetric.internal.application.service.OverallRankingRetentionRunner;
import com.maplemetric.internal.presentation.code.InternalSuccessCode;
import com.maplemetric.internal.presentation.response.OverallRankingRetentionHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 보존 기간이 지난 수집 결과의 만료 대상을 확인하고 실행한다.
 *
 * 산정은 아무것도 바꾸지 않으므로 언제나 조회할 수 있다. 삭제는 설정이 켜져 있을
 * 때만 열린다.
 */
@RestController
@RequestMapping("/internal/v1/retentions/rankings/overall")
public class OverallRankingRetentionController {

    private final OverallRankingRetentionRunner retentionRunner;

    public OverallRankingRetentionController(
            OverallRankingRetentionRunner retentionRunner
    ) {
        this.retentionRunner = retentionRunner;
    }

    @GetMapping("/plan")
    public ApiResponse<OverallRankingRetentionHttpResponse> planRetention() {
        return ApiResponse.ok(
                InternalSuccessCode.OVERALL_RANKING_RETENTION_PLANNED,
                OverallRankingRetentionHttpResponse.from(
                        retentionRunner.plan()
                )
        );
    }

    @PostMapping("/expirations")
    public ApiResponse<OverallRankingRetentionHttpResponse> expireRetention() {
        return ApiResponse.ok(
                InternalSuccessCode.OVERALL_RANKING_RETENTION_EXPIRED,
                OverallRankingRetentionHttpResponse.from(
                        retentionRunner.expire()
                )
        );
    }
}
