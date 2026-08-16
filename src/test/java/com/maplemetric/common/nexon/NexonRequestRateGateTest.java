package com.maplemetric.common.nexon;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * Nexon 요청이 초당 한도를 넘지 않는지 확인한다.
 *
 * 실제로 기다려 확인하면 느리고 결과가 환경에 따라 달라진다. 시간을 직접 넘기고
 * 대기 요청을 가로채 관찰한다.
 */
class NexonRequestRateGateTest {

    private static final Instant START =
            Instant.parse("2026-08-16T00:00:00Z");

    private static final long ONE_SECOND_NANOS = 1_000_000_000L;

    /** 흐르는 시간을 직접 조절한다. */
    private static final class MovableClock extends Clock {

        private Instant now;

        private MovableClock(Instant now) {
            this.now = now;
        }

        private void advance(long nanos) {
            now = now.plusNanos(nanos);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    /** 실제로 자지 않고 요청받은 대기 시간만 기록한다. */
    private static final class RecordingSleeper
            implements NexonRequestRateGate.Sleeper {

        private final List<Long> waits = new ArrayList<>();
        private final MovableClock clock;

        private RecordingSleeper(MovableClock clock) {
            this.clock = clock;
        }

        @Override
        public void sleep(long nanos) {
            waits.add(nanos);

            // 기다린 만큼 시간이 흐른 것으로 본다.
            clock.advance(nanos);
        }
    }

    @Test
    void 한도안에서는기다리지않는다() {
        MovableClock clock = new MovableClock(START);
        RecordingSleeper sleeper = new RecordingSleeper(clock);

        NexonRequestRateGate gate = new NexonRequestRateGate(
                new NexonRateLimitProperties(5),
                clock,
                sleeper
        );

        for (int i = 0; i < 5; i++) {
            gate.acquire();
        }

        assertThat(sleeper.waits).isEmpty();
    }

    /**
     * 한도를 채운 뒤의 요청은 가장 오래된 허가가 1초를 지날 때까지 기다린다.
     */
    @Test
    void 한도를넘으면다음자리가빌때까지기다린다() {
        MovableClock clock = new MovableClock(START);
        RecordingSleeper sleeper = new RecordingSleeper(clock);

        NexonRequestRateGate gate = new NexonRequestRateGate(
                new NexonRateLimitProperties(5),
                clock,
                sleeper
        );

        for (int i = 0; i < 5; i++) {
            gate.acquire();
        }

        // 아직 시간이 흐르지 않았다. 여섯 번째는 꼬박 1초를 기다려야 한다.
        gate.acquire();

        assertThat(sleeper.waits).containsExactly(ONE_SECOND_NANOS);
    }

    /**
     * 1초가 지나면 자리가 다시 열린다.
     */
    @Test
    void 일초가지나면다시허가한다() {
        MovableClock clock = new MovableClock(START);
        RecordingSleeper sleeper = new RecordingSleeper(clock);

        NexonRequestRateGate gate = new NexonRequestRateGate(
                new NexonRateLimitProperties(5),
                clock,
                sleeper
        );

        for (int i = 0; i < 5; i++) {
            gate.acquire();
        }

        clock.advance(ONE_SECOND_NANOS);

        for (int i = 0; i < 5; i++) {
            gate.acquire();
        }

        assertThat(sleeper.waits).isEmpty();
    }

    /**
     * 일부만 지났으면 남은 시간만 기다린다.
     */
    @Test
    void 남은시간만기다린다() {
        MovableClock clock = new MovableClock(START);
        RecordingSleeper sleeper = new RecordingSleeper(clock);

        NexonRequestRateGate gate = new NexonRequestRateGate(
                new NexonRateLimitProperties(1),
                clock,
                sleeper
        );

        gate.acquire();

        clock.advance(400_000_000L);

        gate.acquire();

        assertThat(sleeper.waits).containsExactly(600_000_000L);
    }

    /**
     * Client마다 Requester를 따로 만들어도 관문이 같으면 한도가 합산된다.
     *
     * 제한 상태를 Requester 안에 두면 Client 수만큼 한도를 쓰게 된다.
     */
    @Test
    void 여러사용처가같은관문을쓰면한도가합산된다() {
        MovableClock clock = new MovableClock(START);
        RecordingSleeper sleeper = new RecordingSleeper(clock);

        NexonRequestRateGate shared = new NexonRequestRateGate(
                new NexonRateLimitProperties(5),
                clock,
                sleeper
        );

        // 서로 다른 Client가 같은 관문을 나눠 쓴다.
        for (int i = 0; i < 3; i++) {
            shared.acquire();
        }

        for (int i = 0; i < 2; i++) {
            shared.acquire();
        }

        assertThat(sleeper.waits).isEmpty();

        shared.acquire();

        assertThat(sleeper.waits).hasSize(1);
    }

    /**
     * 동시에 들어와도 한도를 넘겨 허가하지 않는다.
     */
    @Test
    void 동시요청에서도한도를넘기지않는다() throws Exception {
        MovableClock clock = new MovableClock(START);
        AtomicLong waitCount = new AtomicLong();

        NexonRequestRateGate gate = new NexonRequestRateGate(
                new NexonRateLimitProperties(5),
                clock,
                nanos -> waitCount.incrementAndGet()
        );

        int callers = 20;
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(callers);

        try {
            for (int i = 0; i < callers; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    go.await(30, TimeUnit.SECONDS);
                    gate.acquire();
                    return null;
                });
            }

            ready.await(30, TimeUnit.SECONDS);
            go.countDown();

            executor.shutdown();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS))
                    .isTrue();
        } finally {
            executor.shutdownNow();
        }

        // 시간이 흐르지 않았으므로 한도 5건만 즉시 허가되고 나머지는 기다린다.
        assertThat(waitCount.get()).isEqualTo(callers - 5);
    }
}
