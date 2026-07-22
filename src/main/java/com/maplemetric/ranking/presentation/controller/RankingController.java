package com.maplemetric.ranking.presentation.controller;

import com.maplemetric.common.ApiResponse;
import com.maplemetric.ranking.application.result.GetDojangRankingResult;
import com.maplemetric.ranking.application.result.GetOverallRankingResult;
import com.maplemetric.ranking.application.result.GetUnionRankingResult;
import com.maplemetric.ranking.application.service.RankingQueryService;
import com.maplemetric.ranking.presentation.code.RankingSuccessCode;
import com.maplemetric.ranking.presentation.dto.GetDojangRankingResponse;
import com.maplemetric.ranking.presentation.dto.GetOverallRankingResponse;
import com.maplemetric.ranking.presentation.dto.GetUnionRankingResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rankings")
public class RankingController {

    private final RankingQueryService rankingQueryService;

    public RankingController(
            RankingQueryService rankingQueryService
    ) {
        this.rankingQueryService = rankingQueryService;
    }

    @GetMapping("/overall")
    public ApiResponse<GetOverallRankingResponse> getOverallRanking(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false)
            String worldName,
            @RequestParam(required = false)
            @Min(0) @Max(1)
            Integer worldType,
            @RequestParam(required = false)
            String className,
            @RequestParam(defaultValue = "1")
            @Min(1)
            int page
    ) {
        GetOverallRankingResult result =
                rankingQueryService.getOverallRanking(
                        date,
                        worldName,
                        worldType,
                        className,
                        page
                );

        return ApiResponse.ok(
                RankingSuccessCode
                        .OVERALL_RANKING_SEARCH_SUCCESS,
                GetOverallRankingResponse.from(result)
        );
    }

    @GetMapping("/union")
    public ApiResponse<GetUnionRankingResponse> getUnionRanking(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false)
            String worldName,
            @RequestParam(defaultValue = "1")
            @Min(1)
            int page
    ) {
        GetUnionRankingResult result =
                rankingQueryService.getUnionRanking(
                        date,
                        worldName,
                        page
                );

        return ApiResponse.ok(
                RankingSuccessCode
                        .UNION_RANKING_SEARCH_SUCCESS,
                GetUnionRankingResponse.from(result)
        );
    }

    @GetMapping("/dojang")
    public ApiResponse<GetDojangRankingResponse> getDojangRanking(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false)
            String worldName,
            @RequestParam
            @Min(0) @Max(1)
            int difficulty,
            @RequestParam(required = false)
            String className,
            @RequestParam(defaultValue = "1")
            @Min(1)
            int page
    ) {
        GetDojangRankingResult result =
                rankingQueryService.getDojangRanking(
                        date,
                        worldName,
                        difficulty,
                        className,
                        page
                );

        return ApiResponse.ok(
                RankingSuccessCode
                        .DOJANG_RANKING_SEARCH_SUCCESS,
                GetDojangRankingResponse.from(result)
        );
    }
}
