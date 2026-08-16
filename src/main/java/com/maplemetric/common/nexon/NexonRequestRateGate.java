package com.maplemetric.common.nexon;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.LongSupplier;
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
 * 자리를 미리 예약하지 않는다. 예약해 두고 그 시각까지 한 번만 자면, JVM이 멈추거나
 * 스레드가 밀렸을 때 예약 시각이 모두 지난 대기자들이 한꺼번에 깨어나 함께 나간다.
 * 대신 확인하고, 자리가 없으면 자고, 깨어나 다시 확인한다. 실제로 나가는 시점에
 * 자리를 기록하므로 예약 시각과 전송 시각이 어긋나지 않는다.
 *
 * 시간은 단조 증가값으로 잰다. 벽시계는 NTP 보정이나 관리자 변경으로 뒤로 갈 수
 * 있어 경과 시간 측정에 쓰면 과도한 대기나 이른 만료가 생긴다.
 */
@Component
public class NexonRequestRateGate {

    private static final long ONE_SECOND_NANOS =
            TimeUnit.SECONDS.toNanos(1);

    private final NexonRateLimitProperties properties;
    private final LongSupplier ticker;
    private final Sleeper sleeper;

    /** 최근 1초 안에 실제로 나간 요청 시각이다. 오래된 것부터 버린다. */
    private final Deque<Long> recentPermits = new ArrayDeque<>();

    @Autowired
    public NexonRequestRateGate(NexonRateLimitProperties properties) {
        this(
                properties,
                System::nanoTime,
                NexonRequestRateGate::sleepNanos
        );
    }

    NexonRequestRateGate(
            NexonRateLimitProperties properties,
            LongSupplier ticker,
            Sleeper sleeper
    ) {
        this.properties = properties;
        this.ticker = ticker;
        this.sleeper = sleeper;
    }

    /**
     * 요청 하나를 보낼 허가를 받는다.
     *
     * 한도에 여유가 있으면 곧바로 돌아온다. 없으면 여유가 생길 때까지 기다렸다가
     * 다시 확인한다.
     *
     * 대기에는 상한이 있다. 서로 다른 캐릭터를 동시에 조회하면 저장본 합치기가
     * 적용되지 않아 요청마다 21회를 쓴다. 상한이 없으면 처리 Thread가 언제 끝날지
     * 모르는 대기에 묶여 서비스 전체가 멈춘다.
     */
    public void acquire() throws InterruptedException, TimeoutException {
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

                discardExpired(now);

                if (recentPermits.size() < properties.requestsPerSecond()) {
                    recentPermits.addLast(now);

                    return;
                }

                // 가장 오래된 허가가 1초를 지나야 한 자리가 빈다.
                waitNanos = recentPermits.peekFirst()
                        + ONE_SECOND_NANOS
                        - now;

                if (now + waitNanos > deadline) {
                    throw new TimeoutException(
                            "넥슨 요청 허가를 상한 안에 받지 못했습니다."
                    );
                }
            }

            // 기다리는 동안 잠금을 쥐고 있으면 뒤따르는 요청이 한도 계산조차
            // 못 한다. 대기는 잠금 밖에서 한다.
            sleeper.sleep(waitNanos);
        }
    }

    private void discardExpired(long now) {
        while (!recentPermits.isEmpty()
                && now - recentPermits.peekFirst() >= ONE_SECOND_NANOS) {
            recentPermits.removeFirst();
        }
    }

    private static void sleepNanos(long nanos) throws InterruptedException {
        TimeUnit.NANOSECONDS.sleep(nanos);
    }

    /** 대기를 테스트에서 관찰하기 위한 경계다. */
    @FunctionalInterface
    interface Sleeper {

        void sleep(long nanos) throws InterruptedException;
    }
}
