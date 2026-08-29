package com.maplemetric.internal.application.service;

import com.maplemetric.internal.application.properties.OverallRankingCollectionProperties;
import com.maplemetric.internal.application.properties.OverallRankingGapRecoveryProperties;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotRequest;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotUseCase;
import com.maplemetric.ranking.api.FindMissingOverallRankingDatesUseCase;
import com.maplemetric.ranking.api.OverallRankingCollectionAlreadyRunningException;
import com.maplemetric.ranking.api.OverallRankingCollectionException;
import com.maplemetric.ranking.api.OverallRankingCollectionFailure;
import com.maplemetric.ranking.api.OverallRankingCollectionRequestClass;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 비어 있는 기준일을 메운다.
 *
 * 정기 수집은 앱이 떠 있는 동안에만 돈다. 앱이 꺼져 있던 날은 그대로 빈다. 사람이
 * 주기적으로 기억해 메우는 절차로 버티면, 잊힌 사이 외부의 제공 기간이 지나 그
 * 기준일은 영영 받을 수 없게 된다.
 *
 * 대상을 좁게 잡는다. 비어 있는 기준일만 보고, 한 번에 정해진 수까지만 메우며, 미뤄도
 * 되는 요청 몫으로만 호출한다. 임의 기간을 자동으로 받아 오는 경로가 아니다. 그런
 * 수집은 지금처럼 사람이 눌러 실행한다.
 *
 * 앱이 꺼져 있던 시간을 되돌리지는 못한다. 다시 떠 있는 첫 실행에서 그동안의 공백을
 * 줄일 뿐이다. 상시 실행을 대신하는 장치가 아니다.
 */
@Service
public class OverallRankingGapRecoveryRunner {

    private static final Logger log =
            LoggerFactory.getLogger(OverallRankingGapRecoveryRunner.class);

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final FindMissingOverallRankingDatesUseCase findMissingDatesUseCase;
    private final CollectOverallRankingSnapshotUseCase collectUseCase;
    private final OverallRankingGapRecoveryProperties properties;
    private final OverallRankingCollectionProperties collectionProperties;
    private final Clock clock;

    @Autowired
    public OverallRankingGapRecoveryRunner(
            FindMissingOverallRankingDatesUseCase findMissingDatesUseCase,
            CollectOverallRankingSnapshotUseCase collectUseCase,
            OverallRankingGapRecoveryProperties properties,
            OverallRankingCollectionProperties collectionProperties
    ) {
        this(
                findMissingDatesUseCase,
                collectUseCase,
                properties,
                collectionProperties,
                Clock.system(KOREA_ZONE_ID)
        );
    }

    OverallRankingGapRecoveryRunner(
            FindMissingOverallRankingDatesUseCase findMissingDatesUseCase,
            CollectOverallRankingSnapshotUseCase collectUseCase,
            OverallRankingGapRecoveryProperties properties,
            OverallRankingCollectionProperties collectionProperties,
            Clock clock
    ) {
        this.findMissingDatesUseCase = findMissingDatesUseCase;
        this.collectUseCase = collectUseCase;
        this.properties = properties;
        this.collectionProperties = collectionProperties;
        this.clock = clock;
    }

    /**
     * 되짚어 볼 기간 안에서 비어 있는 기준일을 메운다.
     *
     * 오래된 것부터 메운다. 외부가 이력을 주는 기간이 정해져 있어 오래된 날이 먼저
     * 사라지기 때문이다.
     *
     * @return 이번에 새로 채운 기준일 수
     */
    public int run() {
        if (!properties.enabled()) {
            return 0;
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate to = today.minusDays(1);
        LocalDate from = today.minusDays(properties.lookbackDays());

        List<LocalDate> missing =
                findMissingDatesUseCase.findMissingDates(from, to);

        if (missing.isEmpty()) {
            return 0;
        }

        List<LocalDate> targets = missing.subList(
                0,
                Math.min(missing.size(), properties.maxDatesPerRun())
        );

        log.info(
                "비어 있는 기준일을 메웁니다. 전체={}, 이번 대상={}, 기간={}~{}",
                missing.size(),
                targets.size(),
                from,
                to
        );

        int recovered = 0;
        LocalDate oldestRemaining = null;

        for (LocalDate date : targets) {
            RecoveryOutcome outcome = recover(date);

            if (outcome == RecoveryOutcome.RECOVERED) {
                recovered++;
            } else if (oldestRemaining == null) {
                oldestRemaining = date;
            }

            if (outcome == RecoveryOutcome.STOP) {
                break;
            }
        }

        int remaining = missing.size() - recovered;

        if (remaining > 0) {
            // 채운 날을 "남은 것 중 가장 오래된 날"로 적지 않는다. 이 값을 보고
            // 사람이 무엇이 막혀 있는지 판단하므로, 이미 지나간 날을 가리키면
            // 실제로 걸려 있는 날을 놓친다.
            log.info(
                    "아직 비어 있는 기준일이 남았습니다. 남은 수={}, "
                            + "가장 오래된 날={}",
                    remaining,
                    oldestRemaining != null
                            ? oldestRemaining
                            : missing.get(targets.size())
            );
        }

        return recovered;
    }

    /**
     * 기준일 하나를 메운다.
     *
     * 한 기준일에서 실패해도 나머지를 멈추지 않는다. 어제 하루가 안 받아진다고 그 앞의
     * 빈 날들까지 못 메울 이유가 없다.
     *
     * 한도 초과는 다르다. 한도는 기준일이 아니라 호출 몫 전체에 걸리므로, 다음 기준일을
     * 시도해도 같은 벽에 부딪힌다. 재시도까지 되풀이하며 남은 몫만 더 쓴다.
     */
    private RecoveryOutcome recover(LocalDate date) {
        try {
            collectUseCase.collect(
                    new CollectOverallRankingSnapshotRequest(
                            date,
                            collectionProperties.maxPages(),
                            OverallRankingCollectionRequestClass.BULK
                    )
            );

            return RecoveryOutcome.RECOVERED;
        } catch (OverallRankingCollectionAlreadyRunningException exception) {
            // 다른 수집과 겹쳤다. 이 기준일의 문제가 아니므로 다음 실행에서 다시 본다.
            log.info(
                    "다른 수집과 겹쳐 이번에는 메우지 않습니다. 기준일={}",
                    date
            );

            return RecoveryOutcome.SKIPPED;
        } catch (OverallRankingCollectionException exception) {
            if (exception.getFailure()
                    == OverallRankingCollectionFailure
                            .EXTERNAL_API_RATE_LIMITED) {
                log.warn(
                        "한도 초과로 메우기를 멈춥니다. 기준일={}",
                        date
                );

                return RecoveryOutcome.STOP;
            }

            log.warn(
                    "비어 있는 기준일을 메우지 못했습니다. 기준일={}, 원인={}",
                    date,
                    exception.getFailure()
            );

            return RecoveryOutcome.SKIPPED;
        }
    }

    /** 기준일 하나를 시도한 결과다. */
    private enum RecoveryOutcome {

        RECOVERED,

        SKIPPED,

        /** 이번 실행을 여기서 멈춰야 한다. */
        STOP
    }
}
