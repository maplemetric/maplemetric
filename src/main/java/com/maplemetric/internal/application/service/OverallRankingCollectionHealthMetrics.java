package com.maplemetric.internal.application.service;

import com.maplemetric.internal.application.properties.OverallRankingGapRecoveryProperties;
import com.maplemetric.ranking.api.FindMissingOverallRankingDatesUseCase;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 랭킹 수집이 제때 되고 있는지를 밖에서 볼 수 있게 한다.
 *
 * 지금은 며칠째 빠졌는지 알려면 사람이 저장소를 직접 뒤져야 한다. 그래서 8일 공백을
 * 한참 뒤에 알아챈 적이 있다. 값으로 내보내 두면 그 판단을 사람이 매번 하지 않아도
 * 된다.
 *
 * 값을 긁어 갈 때마다 집계하지 않는다. 수집 상태는 하루 단위로 움직이는데 긁는 주기는
 * 그보다 훨씬 짧아서, 매번 세면 그 자체가 저장소 부하가 된다. 짧게 담아 두고 만료되면
 * 다시 센다.
 */
@Component
public class OverallRankingCollectionHealthMetrics {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OverallRankingCollectionHealthMetrics.class
            );

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private static final String MISSING_DAYS_METRIC_NAME =
            "ranking.collection.missing.days";

    private static final String STALENESS_METRIC_NAME =
            "ranking.collection.staleness.days";

    /** 담아 두는 시간이다. 수집 상태는 하루 단위로 움직인다. */
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    /** 셀 수 없을 때 쓰는 값이다. 0으로 두면 "빠진 날이 없다"로 읽힌다. */
    private static final long UNKNOWN = -1L;

    private final FindMissingOverallRankingDatesUseCase findMissingDatesUseCase;
    private final OverallRankingGapRecoveryProperties gapRecoveryProperties;
    private final Clock clock;

    private final AtomicLong missingDays = new AtomicLong(UNKNOWN);
    private final AtomicLong stalenessDays = new AtomicLong(UNKNOWN);

    /** 마지막으로 센 시각이다. 단조 증가값으로 잰다. */
    private volatile long lastMeasuredNanos;

    private volatile boolean measured;

    @Autowired
    public OverallRankingCollectionHealthMetrics(
            FindMissingOverallRankingDatesUseCase findMissingDatesUseCase,
            OverallRankingGapRecoveryProperties gapRecoveryProperties,
            MeterRegistry meterRegistry
    ) {
        this(
                findMissingDatesUseCase,
                gapRecoveryProperties,
                meterRegistry,
                Clock.system(KOREA_ZONE_ID)
        );
    }

    OverallRankingCollectionHealthMetrics(
            FindMissingOverallRankingDatesUseCase findMissingDatesUseCase,
            OverallRankingGapRecoveryProperties gapRecoveryProperties,
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        this.findMissingDatesUseCase = findMissingDatesUseCase;
        this.gapRecoveryProperties = gapRecoveryProperties;
        this.clock = clock;

        Gauge.builder(
                        MISSING_DAYS_METRIC_NAME,
                        this,
                        metrics -> metrics.missingDays()
                )
                .description(
                        "되짚어 보는 기간 안에서 아직 수집하지 못한 기준일 수다. "
                                + "셀 수 없으면 -1이다."
                )
                .register(meterRegistry);

        Gauge.builder(
                        STALENESS_METRIC_NAME,
                        this,
                        metrics -> metrics.stalenessDays()
                )
                .description(
                        "마지막으로 수집한 기준일이 어제로부터 며칠 전인지다. "
                                + "어제까지 받았으면 0이고, 셀 수 없으면 -1이다."
                )
                .register(meterRegistry);
    }

    double missingDays() {
        refreshIfStale();

        return missingDays.get();
    }

    double stalenessDays() {
        refreshIfStale();

        return stalenessDays.get();
    }

    private void refreshIfStale() {
        if (measured
                && System.nanoTime() - lastMeasuredNanos
                        < CACHE_TTL.toNanos()) {
            return;
        }

        refresh();
    }

    /**
     * 값을 다시 센다.
     *
     * 실패해도 예외를 올리지 않는다. 지표를 긁는 요청이 저장소 사정으로 500이 되면,
     * 정작 무엇이 잘못됐는지 보려던 경로가 함께 막힌다. 셀 수 없었다는 것을 값으로
     * 알린다.
     */
    private synchronized void refresh() {
        if (measured
                && System.nanoTime() - lastMeasuredNanos
                        < CACHE_TTL.toNanos()) {
            return;
        }

        try {
            Optional<LocalDate> latestCollected =
                    findMissingDatesUseCase.findLatestCollectedDate();

            if (latestCollected.isEmpty()) {
                // 한 번도 받지 않은 상태다. 빠진 날이 없는 것과 다르다.
                missingDays.set(UNKNOWN);
                stalenessDays.set(UNKNOWN);

                return;
            }

            LocalDate today = LocalDate.now(clock);
            LocalDate yesterday = today.minusDays(1);
            LocalDate from =
                    today.minusDays(gapRecoveryProperties.lookbackDays());

            List<LocalDate> missing =
                    findMissingDatesUseCase.findMissingDates(from, yesterday);

            missingDays.set(missing.size());
            stalenessDays.set(
                    stalenessOf(latestCollected.get(), yesterday)
            );
        } catch (RuntimeException exception) {
            missingDays.set(UNKNOWN);
            stalenessDays.set(UNKNOWN);

            log.warn("수집 상태 지표를 세지 못했습니다.", exception);
        } finally {
            lastMeasuredNanos = System.nanoTime();
            measured = true;
        }
    }

    /**
     * 마지막으로 수집한 기준일이 어제로부터 며칠 전인지 센다.
     *
     * 어제까지 받았으면 0이다. 빈 날 목록에서 거꾸로 세지 않는다. 되짚는 기간 밖은
     * 그 목록에 없어서, 몇 달을 멈춰 있어도 그 기간만큼만 밀린 것으로 보인다.
     */
    private long stalenessOf(
            LocalDate latestCollected,
            LocalDate yesterday
    ) {
        if (!latestCollected.isBefore(yesterday)) {
            return 0L;
        }

        return ChronoUnit.DAYS.between(latestCollected, yesterday);
    }
}
