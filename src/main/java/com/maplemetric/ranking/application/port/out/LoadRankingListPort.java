package com.maplemetric.ranking.application.port.out;

import com.maplemetric.ranking.api.OverallRankingCollectionRequestClass;
import com.maplemetric.ranking.application.result.GetDojangRankingResult;
import com.maplemetric.ranking.application.result.GetOverallRankingResult;
import com.maplemetric.ranking.application.result.GetUnionRankingResult;
import java.time.LocalDate;

public interface LoadRankingListPort {

    /**
     * 종합 랭킹 한 페이지를 외부에서 읽는다.
     *
     * {@code requestClass}는 이 호출이 어느 예산을 쓰는지다. 대량 수집이 정기
     * 수집과 사용자 조회의 하루 한도까지 먹지 않게 가른다.
     */
    GetOverallRankingResult loadOverallRanking(
            LocalDate date,
            String worldName,
            Integer worldType,
            String className,
            int page,
            OverallRankingCollectionRequestClass requestClass
    );

    GetUnionRankingResult loadUnionRanking(
            LocalDate date,
            String worldName,
            int page
    );

    GetDojangRankingResult loadDojangRanking(
            LocalDate date,
            String worldName,
            int difficulty,
            String className,
            int page
    );
}
