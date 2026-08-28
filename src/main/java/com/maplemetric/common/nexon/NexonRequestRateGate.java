package com.maplemetric.common.nexon;

import com.maplemetric.common.config.properties.NexonApiProperties;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Nexon 요청이 초당 한도를 넘지 않게 막는 관문이다.
 *
 * 모든 Client가 이 하나를 함께 쓴다. Client마다 따로 세면 합쳐서 한도를 넘긴다.
 *
 * 시간은 단조 증가값으로 잰다. 벽시계는 NTP 보정이나 관리자 변경으로 뒤로 갈 수
 * 있어 경과 시간 측정에 쓰면 과도한 대기나 이른 만료가 생긴다.
 *
 * Key가 여럿이면 한도도 Key마다 따로 붙는다. Nexon이 애플리케이션 단위로 세기
 * 때문이다. 그래서 창을 Key마다 따로 두고, 허가를 내줄 때 어느 Key로 보내야
 * 하는지를 함께 알려준다. 호출하는 쪽이 Key를 고르면 관문이 세는 창과 실제로
 * 쓰인 Key가 어긋난다.
 *
 * Key는 등급별로 갈라 둔다. Nexon은 하루 한도도 애플리케이션 단위로 세는데, 모두
 * 한 덩어리로 쓰면 대량 요청이 하루 한도를 다 먹었을 때 정기 수집까지 함께 멈춘다.
 * 갈라 두면 하루 사용량을 세지 않고도 예산이 지켜진다. 창은 Key마다 하나이고 등급은
 * 그 Key를 가리키기만 하므로, 한 Key를 두 등급이 함께 보더라도 초당 한도는 여전히
 * 한 번만 센다.
 *
 * 이 격리는 Key 목록과 순서가 같은 실행 인스턴스 하나를 전제로 한다. 창이 메모리에
 * 있어서, 여러 인스턴스가 같은 Key를 쓰면 인스턴스마다 따로 센다.
 */
@Component
public class NexonRequestRateGate {

    private static final Logger log =
            LoggerFactory.getLogger(NexonRequestRateGate.class);

    private static final long ONE_SECOND_NANOS =
            TimeUnit.SECONDS.toNanos(1);

    /** Key가 셋 이상일 때 실패하면 안 되는 요청에 떼어 둘 기본 Key 수다. */
    private static final int DEFAULT_RESERVED_CRITICAL_KEY_COUNT = 2;

    private final NexonRateLimitProperties properties;
    private final LongSupplier ticker;
    private final Sleeper sleeper;

    private final List<String> keys;

    /** Key마다 최근 1초 안에 나간 요청 시각이다. 오래된 것부터 버린다. */
    private final List<Deque<Long>> recentPermits;

    /** 등급이 쓸 수 있는 Key의 자리 번호다. */
    private final Map<NexonRequestClass, int[]> poolKeyIndexes;

    /**
     * 등급마다 다음에 먼저 살펴볼 자리다.
     *
     * 늘 앞에서부터 보면 첫 Key만 한도까지 쓰고 나머지는 남는다. 시작 위치를 돌려
     * 고르게 쓴다.
     */
    private final Map<NexonRequestClass, Integer> nextPoolOffset;

    @Autowired
    public NexonRequestRateGate(
            NexonRateLimitProperties properties,
            NexonApiProperties apiProperties
    ) {
        this(
                properties,
                apiProperties.keys(),
                System::nanoTime,
                NexonRequestRateGate::sleepNanos
        );
    }

    public NexonRequestRateGate(
            NexonRateLimitProperties properties,
            List<String> keys
    ) {
        this(
                properties,
                keys,
                System::nanoTime,
                NexonRequestRateGate::sleepNanos
        );
    }

    NexonRequestRateGate(
            NexonRateLimitProperties properties,
            List<String> keys,
            LongSupplier ticker,
            Sleeper sleeper
    ) {
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException(
                    "Nexon Open API Key는 하나 이상 필요합니다."
            );
        }

        this.properties = properties;
        this.keys = List.copyOf(keys);
        this.ticker = ticker;
        this.sleeper = sleeper;
        this.recentPermits = new ArrayList<>();

        for (int index = 0; index < this.keys.size(); index++) {
            this.recentPermits.add(new ArrayDeque<>());
        }

        this.poolKeyIndexes = splitPools(
                this.keys.size(),
                properties.reservedCriticalKeyCount()
        );
        this.nextPoolOffset = new EnumMap<>(NexonRequestClass.class);

        for (NexonRequestClass requestClass : NexonRequestClass.values()) {
            this.nextPoolOffset.put(requestClass, 0);
        }
    }

    /**
     * 실패하면 안 되는 요청으로 보고 허가를 받는다.
     *
     * 등급을 적지 않은 호출은 지금까지와 같은 몫을 쓴다. 대량 요청만 따로 밝힌다.
     */
    public String acquire() throws InterruptedException, TimeoutException {
        return acquire(NexonRequestClass.CRITICAL);
    }

    /**
     * 요청 하나를 보낼 허가를 받는다.
     *
     * 등급에 배정된 Key 중 여유가 있는 것을 곧바로 돌려준다. 없으면 그 등급의 Key
     * 가운데 가장 먼저 자리가 나는 것을 기다렸다가 다시 확인한다. 다른 등급의 Key가
     * 비는 것은 기다릴 이유가 없다. 어차피 그 Key로는 보내지 않는다.
     *
     * 대기에는 상한이 있다. 서로 다른 캐릭터를 동시에 조회하면 저장본 합치기가
     * 적용되지 않아 요청마다 21회를 쓴다. 상한이 없으면 처리 Thread가 언제 끝날지
     * 모르는 대기에 묶여 서비스 전체가 멈춘다.
     *
     * @return 이 요청에 써야 하는 Key
     */
    public String acquire(NexonRequestClass requestClass)
            throws InterruptedException, TimeoutException {
        if (requestClass == null) {
            throw new IllegalArgumentException(
                    "넥슨 요청 등급은 필수입니다."
            );
        }

        long deadline = ticker.getAsLong() + properties.maxWait().toNanos();

        while (true) {
            long waitNanos;

            synchronized (this) {
                long now = ticker.getAsLong();

                // 늦게 깨어나 상한을 넘겼다면 자리가 비었더라도 보내지 않는다.
                // 그렇지 않으면 상한을 지난 뒤에 외부 호출이 한 건 더 나간다.
                if (now > deadline) {
                    throw new TimeoutException(
                            "넥슨 요청 허가를 상한 안에 받지 못했습니다."
                    );
                }

                String granted = grantIfAvailable(now, requestClass);

                if (granted != null) {
                    return granted;
                }

                waitNanos = earliestFreeNanos(now, requestClass);

                if (now + waitNanos > deadline) {
                    throw new TimeoutException(
                            "넥슨 요청 허가를 상한 안에 받지 못했습니다."
                    );
                }
            }

            sleeper.sleep(waitNanos);
        }
    }

    /** 등급에 배정된 Key 중 여유가 있는 것을 찾아 허가를 기록한다. */
    private String grantIfAvailable(
            long now,
            NexonRequestClass requestClass
    ) {
        int[] pool = poolKeyIndexes.get(requestClass);
        int startOffset = nextPoolOffset.get(requestClass);

        for (int offset = 0; offset < pool.length; offset++) {
            int poolPosition = (startOffset + offset) % pool.length;
            int keyIndex = pool[poolPosition];
            Deque<Long> permits = recentPermits.get(keyIndex);

            discardExpired(permits, now);

            if (permits.size() < properties.requestsPerSecond()) {
                permits.addLast(now);
                nextPoolOffset.put(
                        requestClass,
                        (poolPosition + 1) % pool.length
                );

                return keys.get(keyIndex);
            }
        }

        return null;
    }

    /** 등급에 배정된 Key 중 가장 먼저 자리가 날 때까지 남은 시간이다. */
    private long earliestFreeNanos(
            long now,
            NexonRequestClass requestClass
    ) {
        long earliest = Long.MAX_VALUE;

        for (int keyIndex : poolKeyIndexes.get(requestClass)) {
            Long oldest = recentPermits.get(keyIndex).peekFirst();

            if (oldest == null) {
                return 0L;
            }

            earliest = Math.min(earliest, oldest + ONE_SECOND_NANOS - now);
        }

        return Math.max(earliest, 0L);
    }

    /**
     * Key 자리를 등급별로 나눈다.
     *
     * 앞에서부터 정해진 수만큼이 실패하면 안 되는 요청 몫이고 나머지가 대량 요청
     * 몫이다. Key가 하나뿐이면 둘로 나눌 수 없어 같은 자리를 함께 본다. 창은 Key마다
     * 하나이므로 초당 한도는 그대로 지켜지지만 하루 예산은 갈리지 않는다.
     */
    private static Map<NexonRequestClass, int[]> splitPools(
            int keyCount,
            Integer reservedCriticalKeyCount
    ) {
        if (keyCount == 1) {
            if (reservedCriticalKeyCount != null
                    && reservedCriticalKeyCount != 1) {
                throw new IllegalArgumentException(
                        "Key가 하나면 떼어 둘 Key 수는 1이어야 합니다."
                );
            }

            log.warn(
                    "Nexon Open API Key가 하나라 하루 호출 예산을 등급별로 "
                            + "가르지 못합니다. 대량 요청이 정기 수집의 하루 "
                            + "한도까지 씁니다."
            );

            int[] onlyKey = {0};

            return Map.of(
                    NexonRequestClass.CRITICAL, onlyKey,
                    NexonRequestClass.BULK, onlyKey
            );
        }

        int reserved = resolveReservedCount(
                keyCount,
                reservedCriticalKeyCount
        );

        int[] critical = new int[reserved];
        int[] bulk = new int[keyCount - reserved];

        for (int index = 0; index < reserved; index++) {
            critical[index] = index;
        }

        for (int index = reserved; index < keyCount; index++) {
            bulk[index - reserved] = index;
        }

        return Map.of(
                NexonRequestClass.CRITICAL, critical,
                NexonRequestClass.BULK, bulk
        );
    }

    /**
     * 떼어 둘 Key 수를 정한다.
     *
     * 적어 두었으면 그대로 쓰되 남는 Key가 없으면 시작을 막는다. 대량 요청 몫이
     * 비면 그 요청은 영영 나가지 못한다. 조용히 다른 몫을 빌려 쓰게 하면 갈라 놓았다고
     * 적어 두고 실제로는 하지 않는 것이 된다.
     */
    private static int resolveReservedCount(
            int keyCount,
            Integer reservedCriticalKeyCount
    ) {
        if (reservedCriticalKeyCount == null) {
            return Math.min(
                    DEFAULT_RESERVED_CRITICAL_KEY_COUNT,
                    keyCount - 1
            );
        }

        if (reservedCriticalKeyCount >= keyCount) {
            throw new IllegalArgumentException(
                    "떼어 둘 Key 수는 전체 Key 수보다 적어야 합니다."
            );
        }

        return reservedCriticalKeyCount;
    }

    private void discardExpired(Deque<Long> permits, long now) {
        while (!permits.isEmpty()
                && now - permits.peekFirst() >= ONE_SECOND_NANOS) {
            permits.pollFirst();
        }
    }

    private static void sleepNanos(long nanos) throws InterruptedException {
        TimeUnit.NANOSECONDS.sleep(nanos);
    }

    /** 실제로 자지 않고 확인할 수 있도록 분리한다. */
    interface Sleeper {

        void sleep(long nanos) throws InterruptedException;
    }
}
