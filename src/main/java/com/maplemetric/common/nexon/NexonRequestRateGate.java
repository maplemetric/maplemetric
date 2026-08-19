package com.maplemetric.common.nexon;

import com.maplemetric.common.config.properties.NexonApiProperties;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.LongSupplier;
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
 */
@Component
public class NexonRequestRateGate {

    private static final long ONE_SECOND_NANOS =
            TimeUnit.SECONDS.toNanos(1);

    private final NexonRateLimitProperties properties;
    private final LongSupplier ticker;
    private final Sleeper sleeper;

    private final List<String> keys;

    /** Key마다 최근 1초 안에 나간 요청 시각이다. 오래된 것부터 버린다. */
    private final List<Deque<Long>> recentPermits;

    /**
     * 다음에 먼저 살펴볼 Key다.
     *
     * 늘 앞에서부터 보면 첫 Key만 한도까지 쓰고 나머지는 남는다. 시작 위치를 돌려
     * 고르게 쓴다.
     */
    private int nextKeyIndex;

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
    }

    /**
     * 요청 하나를 보낼 허가를 받는다.
     *
     * 여유가 있는 Key가 있으면 그 Key를 곧바로 돌려준다. 없으면 가장 먼저 자리가
     * 나는 Key를 기다렸다가 다시 확인한다.
     *
     * 대기에는 상한이 있다. 서로 다른 캐릭터를 동시에 조회하면 저장본 합치기가
     * 적용되지 않아 요청마다 21회를 쓴다. 상한이 없으면 처리 Thread가 언제 끝날지
     * 모르는 대기에 묶여 서비스 전체가 멈춘다.
     *
     * @return 이 요청에 써야 하는 Key
     */
    public String acquire() throws InterruptedException, TimeoutException {
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

                String granted = grantIfAvailable(now);

                if (granted != null) {
                    return granted;
                }

                waitNanos = earliestFreeNanos(now);

                if (now + waitNanos > deadline) {
                    throw new TimeoutException(
                            "넥슨 요청 허가를 상한 안에 받지 못했습니다."
                    );
                }
            }

            sleeper.sleep(waitNanos);
        }
    }

    /** 여유가 있는 Key를 찾아 허가를 기록하고 그 Key를 돌려준다. */
    private String grantIfAvailable(long now) {
        for (int offset = 0; offset < keys.size(); offset++) {
            int index = (nextKeyIndex + offset) % keys.size();
            Deque<Long> permits = recentPermits.get(index);

            discardExpired(permits, now);

            if (permits.size() < properties.requestsPerSecond()) {
                permits.addLast(now);
                nextKeyIndex = (index + 1) % keys.size();

                return keys.get(index);
            }
        }

        return null;
    }

    /** 어느 Key든 가장 먼저 자리가 날 때까지 남은 시간이다. */
    private long earliestFreeNanos(long now) {
        long earliest = Long.MAX_VALUE;

        for (Deque<Long> permits : recentPermits) {
            Long oldest = permits.peekFirst();

            if (oldest == null) {
                return 0L;
            }

            earliest = Math.min(earliest, oldest + ONE_SECOND_NANOS - now);
        }

        return Math.max(earliest, 0L);
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
