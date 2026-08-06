package com.maplemetric.statistics.application.service;

import com.maplemetric.ranking.api.JobCatalogQuery;
import com.maplemetric.statistics.api.StatisticsDetailFact;
import com.maplemetric.statistics.api.StatisticsFactQuery;
import com.maplemetric.statistics.api.StatisticsHistoryFact;
import com.maplemetric.statistics.api.StatisticsSubject;
import com.maplemetric.statistics.api.StatisticsSubjectType;
import com.maplemetric.statistics.application.result.GetJobStatisticsDetailResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsHistoryResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsDetailResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsHistoryResult;
import com.maplemetric.world.api.WorldCatalogQuery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 기존 조회 결과를 Fact로 옮긴다.
 *
 * 여기서 수치를 만들지 않는다. Statistics가 이미 반올림까지 마친 값을 그대로 옮기며,
 * 옮기는 과정에 계산이 들어가면 화면과 설명이 서로 다른 숫자를 말하게 된다.
 *
 * 조회 자체는 기존 Service가 한다. 이 Service는 그 결과의 모양만 바꾼다.
 */
@Service
public class StatisticsFactQueryService implements StatisticsFactQuery {

    private final JobStatisticsDetailQueryService jobQueryService;
    private final WorldStatisticsDetailQueryService worldQueryService;
    private final JobCatalogQuery jobCatalogQuery;
    private final WorldCatalogQuery worldCatalogQuery;

    public StatisticsFactQueryService(
            JobStatisticsDetailQueryService jobQueryService,
            WorldStatisticsDetailQueryService worldQueryService,
            JobCatalogQuery jobCatalogQuery,
            WorldCatalogQuery worldCatalogQuery
    ) {
        this.jobQueryService = jobQueryService;
        this.worldQueryService = worldQueryService;
        this.jobCatalogQuery = jobCatalogQuery;
        this.worldCatalogQuery = worldCatalogQuery;
    }

    @Override
    public List<StatisticsSubject> listSubjects() {
        List<StatisticsSubject> subjects = new ArrayList<>();

        jobCatalogQuery.findAll().forEach(job -> subjects.add(
                new StatisticsSubject(
                        StatisticsSubjectType.JOB,
                        job.jobSlug(),
                        job.jobName()
                )
        ));

        worldCatalogQuery.findAll().forEach(world -> subjects.add(
                new StatisticsSubject(
                        StatisticsSubjectType.WORLD,
                        world.worldSlug(),
                        world.worldName()
                )
        ));

        return List.copyOf(subjects);
    }

    @Override
    public StatisticsDetailFact getJobDetailFact(String jobSlug) {
        GetJobStatisticsDetailResult result =
                jobQueryService.getJobStatisticsDetail(jobSlug);

        return new StatisticsDetailFact(
                new StatisticsSubject(
                        StatisticsSubjectType.JOB,
                        result.job().jobSlug(),
                        result.job().jobName()
                ),
                result.dataAvailability(),
                toLatest(result.latest()),
                toComparison(result.comparison()),
                toSourceMeta(result.sourceMeta())
        );
    }

    @Override
    public StatisticsDetailFact getWorldDetailFact(String worldSlug) {
        GetWorldStatisticsDetailResult result =
                worldQueryService.getWorldStatisticsDetail(worldSlug);

        return new StatisticsDetailFact(
                new StatisticsSubject(
                        StatisticsSubjectType.WORLD,
                        result.world().worldSlug(),
                        result.world().worldName()
                ),
                result.dataAvailability(),
                toLatest(result.latest()),
                toComparison(result.comparison()),
                toSourceMeta(result.sourceMeta())
        );
    }

    @Override
    public StatisticsHistoryFact getJobHistoryFact(
            String jobSlug,
            String range,
            LocalDate from,
            LocalDate to
    ) {
        GetJobStatisticsHistoryResult result =
                jobQueryService.getJobStatisticsHistory(
                        jobSlug,
                        range,
                        from,
                        to
                );

        return new StatisticsHistoryFact(
                new StatisticsSubject(
                        StatisticsSubjectType.JOB,
                        result.job().jobSlug(),
                        result.job().jobName()
                ),
                result.dataAvailability(),
                toRange(result.range()),
                toRangeComparison(result.rangeComparison()),
                toJobPoints(result.points()),
                result.limitations()
        );
    }

    @Override
    public StatisticsHistoryFact getWorldHistoryFact(
            String worldSlug,
            String range,
            LocalDate from,
            LocalDate to
    ) {
        GetWorldStatisticsHistoryResult result =
                worldQueryService.getWorldStatisticsHistory(
                        worldSlug,
                        range,
                        from,
                        to
                );

        return new StatisticsHistoryFact(
                new StatisticsSubject(
                        StatisticsSubjectType.WORLD,
                        result.world().worldSlug(),
                        result.world().worldName()
                ),
                result.dataAvailability(),
                toRange(result.range()),
                toRangeComparison(result.rangeComparison()),
                toWorldPoints(result.points()),
                result.limitations()
        );
    }

    private StatisticsDetailFact.LatestFact toLatest(
            GetJobStatisticsDetailResult.LatestResult latest
    ) {
        if (latest == null) {
            return null;
        }

        return new StatisticsDetailFact.LatestFact(
                latest.asOf(),
                latest.count(),
                latest.percentage(),
                latest.averageLevel()
        );
    }

    private StatisticsDetailFact.LatestFact toLatest(
            GetWorldStatisticsDetailResult.LatestResult latest
    ) {
        if (latest == null) {
            return null;
        }

        return new StatisticsDetailFact.LatestFact(
                latest.asOf(),
                latest.count(),
                latest.percentage(),
                latest.averageLevel()
        );
    }

    private StatisticsDetailFact.ComparisonFact toComparison(
            GetJobStatisticsDetailResult.ComparisonResult comparison
    ) {
        if (comparison == null) {
            return null;
        }

        return new StatisticsDetailFact.ComparisonFact(
                comparison.previousAsOf(),
                comparison.previousCount(),
                comparison.countChange(),
                comparison.countChangeRate(),
                comparison.previousPercentage(),
                comparison.percentageChangeRate(),
                comparison.percentagePointChange(),
                comparison.trend()
        );
    }

    private StatisticsDetailFact.ComparisonFact toComparison(
            GetWorldStatisticsDetailResult.ComparisonResult comparison
    ) {
        if (comparison == null) {
            return null;
        }

        return new StatisticsDetailFact.ComparisonFact(
                comparison.previousAsOf(),
                comparison.previousCount(),
                comparison.countChange(),
                comparison.countChangeRate(),
                comparison.previousPercentage(),
                comparison.percentageChangeRate(),
                comparison.percentagePointChange(),
                comparison.trend()
        );
    }

    private StatisticsDetailFact.SourceMetaFact toSourceMeta(
            GetJobStatisticsDetailResult.SourceMetaResult sourceMeta
    ) {
        if (sourceMeta == null) {
            return null;
        }

        return new StatisticsDetailFact.SourceMetaFact(
                sourceMeta.source(),
                sourceMeta.collectedAt(),
                sourceMeta.sampleSize(),
                sourceMeta.pageCount(),
                sourceMeta.requestedMaxPages(),
                sourceMeta.truncated()
        );
    }

    private StatisticsDetailFact.SourceMetaFact toSourceMeta(
            GetWorldStatisticsDetailResult.SourceMetaResult sourceMeta
    ) {
        if (sourceMeta == null) {
            return null;
        }

        return new StatisticsDetailFact.SourceMetaFact(
                sourceMeta.source(),
                sourceMeta.collectedAt(),
                sourceMeta.sampleSize(),
                sourceMeta.pageCount(),
                sourceMeta.requestedMaxPages(),
                sourceMeta.truncated()
        );
    }

    private StatisticsHistoryFact.RangeFact toRange(
            GetJobStatisticsHistoryResult.RangeResult range
    ) {
        if (range == null) {
            return null;
        }

        return new StatisticsHistoryFact.RangeFact(
                range.preset(),
                range.requestedFrom(),
                range.requestedTo(),
                range.firstAsOf(),
                range.lastAsOf(),
                range.pointCount(),
                range.missingDateCount()
        );
    }

    private StatisticsHistoryFact.RangeFact toRange(
            GetWorldStatisticsHistoryResult.RangeResult range
    ) {
        if (range == null) {
            return null;
        }

        return new StatisticsHistoryFact.RangeFact(
                range.preset(),
                range.requestedFrom(),
                range.requestedTo(),
                range.firstAsOf(),
                range.lastAsOf(),
                range.pointCount(),
                range.missingDateCount()
        );
    }

    private StatisticsHistoryFact.RangeComparisonFact toRangeComparison(
            GetJobStatisticsHistoryResult.RangeComparisonResult comparison
    ) {
        if (comparison == null) {
            return null;
        }

        return new StatisticsHistoryFact.RangeComparisonFact(
                comparison.fromAsOf(),
                comparison.toAsOf(),
                comparison.previousCount(),
                comparison.currentCount(),
                comparison.countChange(),
                comparison.countChangeRate(),
                comparison.previousPercentage(),
                comparison.currentPercentage(),
                comparison.percentageChangeRate(),
                comparison.percentagePointChange()
        );
    }

    private StatisticsHistoryFact.RangeComparisonFact toRangeComparison(
            GetWorldStatisticsHistoryResult.RangeComparisonResult comparison
    ) {
        if (comparison == null) {
            return null;
        }

        return new StatisticsHistoryFact.RangeComparisonFact(
                comparison.fromAsOf(),
                comparison.toAsOf(),
                comparison.previousCount(),
                comparison.currentCount(),
                comparison.countChange(),
                comparison.countChangeRate(),
                comparison.previousPercentage(),
                comparison.currentPercentage(),
                comparison.percentageChangeRate(),
                comparison.percentagePointChange()
        );
    }

    private List<StatisticsHistoryFact.PointFact> toJobPoints(
            List<GetJobStatisticsHistoryResult.PointResult> points
    ) {
        return points.stream()
                .map(point -> new StatisticsHistoryFact.PointFact(
                        point.asOf(),
                        point.count(),
                        point.percentage(),
                        point.averageLevel(),
                        point.sampleSize(),
                        point.pageCount(),
                        point.requestedMaxPages(),
                        point.truncated(),
                        point.collectedAt()
                ))
                .toList();
    }

    private List<StatisticsHistoryFact.PointFact> toWorldPoints(
            List<GetWorldStatisticsHistoryResult.PointResult> points
    ) {
        return points.stream()
                .map(point -> new StatisticsHistoryFact.PointFact(
                        point.asOf(),
                        point.count(),
                        point.percentage(),
                        point.averageLevel(),
                        point.sampleSize(),
                        point.pageCount(),
                        point.requestedMaxPages(),
                        point.truncated(),
                        point.collectedAt()
                ))
                .toList();
    }
}
