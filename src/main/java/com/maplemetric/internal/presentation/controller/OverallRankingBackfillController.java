package com.maplemetric.internal.presentation.controller;

import com.maplemetric.common.ApiResponse;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillJob;
import com.maplemetric.internal.application.service.OverallRankingBackfillJobNotFoundException;
import com.maplemetric.internal.application.service.OverallRankingBackfillRunner;
import com.maplemetric.internal.application.service.OverallRankingBackfillStateService;
import com.maplemetric.internal.presentation.code.InternalSuccessCode;
import com.maplemetric.internal.presentation.request.CreateOverallRankingBackfillHttpRequest;
import com.maplemetric.internal.presentation.response.OverallRankingBackfillJobHttpResponse;
import com.maplemetric.internal.presentation.response.RunOverallRankingBackfillHttpResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 승인된 Backfill을 수동으로 개시한다.
 *
 * Scheduler를 두지 않는다. Backfill은 과거 기준일마다 Nexon을 호출하므로 승인 없이
 * 자동으로 도는 경로가 있으면 안 된다.
 *
 * 실행은 한 번에 {@code max-dates-per-run}개 기준일까지만 처리하고 돌아온다. 한 번의
 * 호출이 Quota를 무제한으로 쓰지 않게 하고, 남은 기준일은 다시 호출해 이어간다.
 */
@RestController
@RequestMapping("/internal/v1/backfills/rankings/overall")
public class OverallRankingBackfillController {

    private final OverallRankingBackfillRunner backfillRunner;
    private final OverallRankingBackfillStateService backfillStateService;

    public OverallRankingBackfillController(
            OverallRankingBackfillRunner backfillRunner,
            OverallRankingBackfillStateService backfillStateService
    ) {
        this.backfillRunner = backfillRunner;
        this.backfillStateService = backfillStateService;
    }

    @PostMapping
    public ApiResponse<OverallRankingBackfillJobHttpResponse> createBackfillJob(
            @RequestBody @Valid CreateOverallRankingBackfillHttpRequest request
    ) {
        BackfillJob job = backfillRunner.createJob(
                request.from(),
                request.to()
        );

        return ApiResponse.ok(
                InternalSuccessCode.OVERALL_RANKING_BACKFILL_JOB_CREATED,
                toResponse(job)
        );
    }

    @PostMapping("/{backfillJobId}/runs")
    public ApiResponse<RunOverallRankingBackfillHttpResponse> runBackfill(
            @PathVariable UUID backfillJobId
    ) {
        // 없는 Job으로 실행하면 처리한 기준일 0으로 조용히 성공해 버린다.
        requireJob(backfillJobId);

        int processedDateCount = backfillRunner.run(backfillJobId);

        return ApiResponse.ok(
                InternalSuccessCode.OVERALL_RANKING_BACKFILL_RUN_SUCCESS,
                new RunOverallRankingBackfillHttpResponse(
                        processedDateCount,
                        toResponse(backfillJobId)
                )
        );
    }

    @GetMapping("/{backfillJobId}")
    public ApiResponse<OverallRankingBackfillJobHttpResponse> getBackfillJob(
            @PathVariable UUID backfillJobId
    ) {
        return ApiResponse.ok(
                InternalSuccessCode.OVERALL_RANKING_BACKFILL_JOB_FOUND,
                toResponse(backfillJobId)
        );
    }

    private OverallRankingBackfillJobHttpResponse toResponse(
            UUID backfillJobId
    ) {
        return toResponse(requireJob(backfillJobId));
    }

    private OverallRankingBackfillJobHttpResponse toResponse(
            BackfillJob job
    ) {
        return OverallRankingBackfillJobHttpResponse.from(
                job,
                backfillStateService.findDates(job.id())
        );
    }

    private BackfillJob requireJob(UUID backfillJobId) {
        return backfillStateService.findJob(backfillJobId)
                .orElseThrow(
                        () -> new OverallRankingBackfillJobNotFoundException()
                );
    }
}
