package com.maplemetric.common.nexon;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Nexon 요청이 초당 한도를 넘지 않도록 전송 직전에 허가를 준다.
 *
 * Nexon은 애플리케이션 단위로 초당 호출 수를 제한한다. 넘기면 429가 돌아오고,
 * 그 응답도 호출로 집계될 가능성이 커서 한도를 넘기는 것이 곧 손해다.
 *
 * 관문은 하나만 존재해야 한다. Client가 각자 {@link NexonApiRequester}를 만들기
 * 때문에 제한 상태를 Requester 안에 두면 Client 수만큼 한도를 쓰게 된다.
 *
 * 최근 1초 안에 나간 요청 시각을 들고 있다가, 한도를 채웠으면 가장 오래된 요청이
 * 1초를 지날 때까지 기다린다. 고정 구간으로 세지 않는 이유는 구간 경계에서 한도의
 * 두 배가 몰려 나갈 수 있기 때문이다.
 */
@Component
public class NexonRequestRateGate {

    private static final long ONE_SECOND_NANOS =
            TimeUnit.SECONDS.toNanos(1);

    private final NexonRateLimitProperties properties;
    private final Clock clock;
    private final Sleeper sleeper;

    /** 최근 1초 구간에 허가한 요청 시각이다. 오래된 것부터 버린다. */
    private final Deque<Long> recentPermits = new ArrayDeque<>();

    @Autowired
    public NexonRequestRateGate(NexonRateLimitProperties properties) {
        this(
                properties,
                Clock.systemUTC(),
                NexonRequestRateGate::sleepNanos
        );
    }

    NexonRequestRateGate(
            NexonRateLimitProperties properties,
            Clock clock,
            Sleeper sleeper
    ) {
        this.properties = properties;
        this.clock = clock;
        this.sleeper = sleeper;
    }

    /**
     * 요청 하나를 보낼 허가를 받는다.
     *
     * 한도에 여유가 있으면 곧바로 돌아온다. 없으면 여유가 생길 때까지 기다린다.
     */
    public void acquire() {
        long waitNanos;

        synchronized (this) {
            waitNanos = reserve();
        }

        if (waitNanos > 0) {
            sleeper.sleep(waitNanos);
        }
    }

    /**
     * 자리를 잡고 기다려야 할 시간을 돌려준다.
     *
     * 기다리는 동안 잠금을 쥐고 있으면 뒤따르는 요청이 한도 계산조차 못 한다.
     * 자리는 잡되 대기는 잠금 밖에서 한다.
     */
    private long reserve() {
        long now = nanosOf(clock.instant());

        discardExpired(now);

        int limit = properties.requestsPerSecond();

        if (recentPermits.size() < limit) {
            recentPermits.addLast(now);

            return 0L;
        }

        // 가장 오래된 허가가 1초를 지나야 한 자리가 빈다.
        long oldest = recentPermits.peekFirst();
        long availableAt = oldest + ONE_SECOND_NANOS;

        recentPermits.removeFirst();
        recentPermits.addLast(availableAt);

        return Math.max(0L, availableAt - now);
    }

    private void discardExpired(long now) {
        while (!recentPermits.isEmpty()
                && now - recentPermits.peekFirst() >= ONE_SECOND_NANOS) {
            recentPermits.removeFirst();
        }
    }

    private long nanosOf(Instant instant) {
        return instant.getEpochSecond() * 1_000_000_000L
                + instant.getNano();
    }

    private static void sleepNanos(long nanos) {
        try {
            TimeUnit.NANOSECONDS.sleep(nanos);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "넥슨 요청 허가를 기다리는 중 중단되었습니다.",
                    exception
            );
        }
    }

    /** 대기를 테스트에서 관찰하기 위한 경계다. */
    @FunctionalInterface
    interface Sleeper {

        void sleep(long nanos);
    }
}
