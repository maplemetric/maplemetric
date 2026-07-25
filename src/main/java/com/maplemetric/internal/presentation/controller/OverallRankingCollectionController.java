package com.maplemetric.internal.presentation.controller;

import com.maplemetric.common.ApiResponse;
import com.maplemetric.internal.infrastructure.properties.OverallRankingCollectionProperties;
import com.maplemetric.internal.presentation.code.InternalSuccessCode;
import com.maplemetric.internal.presentation.request.CollectOverallRankingSnapshotHttpRequest;
import com.maplemetric.internal.presentation.response.CollectOverallRankingSnapshotHttpResponse;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotOutcome;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotRequest;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotUseCase;
import com.maplemetric.ranking.api.OverallRankingCollectionStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/collections/rankings")
public class OverallRankingCollectionController {

    private final CollectOverallRankingSnapshotUseCase
            collectOverallRankingSnapshotUseCase;
    private final OverallRankingCollectionProperties properties;

    public OverallRankingCollectionController(
            CollectOverallRankingSnapshotUseCase
                    collectOverallRankingSnapshotUseCase,
            OverallRankingCollectionProperties properties
    ) {
        this.collectOverallRankingSnapshotUseCase =
                collectOverallRankingSnapshotUseCase;
        this.properties = properties;
    }

    @PostMapping("/overall")
    public ApiResponse<CollectOverallRankingSnapshotHttpResponse> collectOverallRanking(
            @RequestBody(required = false)
            @Valid
            CollectOverallRankingSnapshotHttpRequest request
    ) {
        CollectOverallRankingSnapshotHttpRequest httpRequest =
                request != null
                        ? request
                        : new CollectOverallRankingSnapshotHttpRequest(
                                null,
                                null
                        );

        int maxPages = httpRequest.maxPages() != null
                ? httpRequest.maxPages()
                : properties.maxPages();

        CollectOverallRankingSnapshotOutcome outcome =
                collectOverallRankingSnapshotUseCase.collect(
                        new CollectOverallRankingSnapshotRequest(
                                httpRequest.rankingDate(),
                                maxPages
                        )
                );

        return ApiResponse.ok(
                resolveSuccessCode(outcome.status()),
                CollectOverallRankingSnapshotHttpResponse.from(outcome)
        );
    }

    private InternalSuccessCode resolveSuccessCode(
            OverallRankingCollectionStatus status
    ) {
        return switch (status) {
            case COLLECTED ->
                    InternalSuccessCode.OVERALL_RANKING_COLLECTION_SUCCESS;
            case SKIPPED ->
                    InternalSuccessCode.OVERALL_RANKING_COLLECTION_SKIPPED;
        };
    }
}
