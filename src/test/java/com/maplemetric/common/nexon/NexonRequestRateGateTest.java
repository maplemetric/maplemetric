package com.maplemetric.common.nexon;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * Nexon 요청이 초당 한도를 넘지 않는지 확인한다.
 *
 * 실제로 기다려 확인하면 느리고 결과가 환경에 따라 달라진다. 시간을 직접 넘기고
 * 대기 요청을 가로채 관찰한다.
 */
class NexonRequestRateGateTest {

    private static final long ONE_SECOND_NANOS = 1_000_000_000L;

    /** 직접 넘기는 단조 증가 시간이다. */
    private final AtomicLong ticker = new AtomicLong();

    /** 실제로 자지 않고 요청받은 대기 시간만 기록한다. */
    private final class RecordingSleeper
            implements NexonRequestRateGate.Sleeper {

        private final List<Long> waits = new ArrayList<>();

        @Override
        public void sleep(long nanos) {
            waits.add(nanos);

            // 기다린 만큼 시간이 흐른 것으로 본다.
            ticker.addAndGet(nanos);
        }
    }

    private NexonRequestRateGate gateOf(
            int limit,
            NexonRequestRateGate.Sleeper sleeper
    ) {
        return new NexonRequestRateGate(
                new NexonRateLimitProperties(limit),
                ticker::get,
                sleeper
        );
    }

    @Test
    void 한도안에서는기다리지않는다() throws Exception {
        RecordingSleeper sleeper = new RecordingSleeper();
        NexonRequestRateGate gate = gateOf(5, sleeper);

        for (int i = 0; i < 5; i++) {
            gate.acquire();
        }

        assertThat(sleeper.waits).isEmpty();
    }

    /**
     * 한도를 채운 뒤의 요청은 가장 오래된 허가가 1초를 지날 때까지 기다린다.
     */
    @Test
    void 한도를넘으면다음자리가빌때까지기다린다() throws Exception {
        RecordingSleeper sleeper = new RecordingSleeper();
        NexonRequestRateGate gate = gateOf(5, sleeper);

        for (int i = 0; i < 5; i++) {
            gate.acquire();
        }

        gate.acquire();

        assertThat(sleeper.waits).containsExactly(ONE_SECOND_NANOS);
    }

    @Test
    void 일초가지나면다시허가한다() throws Exception {
        RecordingSleeper sleeper = new RecordingSleeper();
        NexonRequestRateGate gate = gateOf(5, sleeper);

        for (int i = 0; i < 5; i++) {
            gate.acquire();
        }

        ticker.addAndGet(ONE_SECOND_NANOS);

        for (int i = 0; i < 5; i++) {
            gate.acquire();
        }

        assertThat(sleeper.waits).isEmpty();
    }

    @Test
    void 남은시간만기다린다() throws Exception {
        RecordingSleeper sleeper = new RecordingSleeper();
        NexonRequestRateGate gate = gateOf(1, sleeper);

        gate.acquire();

        ticker.addAndGet(400_000_000L);

        gate.acquire();

        assertThat(sleeper.waits).containsExactly(600_000_000L);
    }

    /**
     * Client마다 Requester를 따로 만들어도 관문이 같으면 한도가 합산된다.
     */
    @Test
    void 여러사용처가같은관문을쓰면한도가합산된다() throws Exception {
        RecordingSleeper sleeper = new RecordingSleeper();
        NexonRequestRateGate shared = gateOf(5, sleeper);

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
     * 잠에서 늦게 깨어나도 그 시점의 한도를 다시 확인한다.
     *
     * 자리를 미리 예약하고 한 번만 자면, 예약 시각이 모두 지난 대기자들이 함께
     * 깨어나 한꺼번에 나간다. 깨어난 시점에 자리가 없으면 다시 기다려야 한다.
     */
    @Test
    void 늦게깨어나면다시확인한다() throws Exception {
        List<Long> waits = new ArrayList<>();
        AtomicInteger sleepCount = new AtomicInteger();

        // 첫 대기에서 시간이 전혀 흐르지 않은 것처럼 만든다. 예약해 두고
        // 한 번만 자는 구현은 여기서 그대로 통과한다.
        NexonRequestRateGate gate = gateOf(1, nanos -> {
            waits.add(nanos);

            if (sleepCount.incrementAndGet() == 1) {
                return;
            }

            ticker.addAndGet(nanos);
        });

        gate.acquire();
        gate.acquire();

        // 첫 대기 후 자리가 없으므로 한 번 더 기다려야 한다.
        assertThat(waits).hasSize(2);
    }

    /**
     * 시간을 뒤로 돌려도 허가가 새어 나가지 않는다.
     *
     * 벽시계를 쓰면 NTP 보정으로 시각이 뒤로 갈 때 계산이 어긋난다. 단조 증가값을
     * 쓰면 그런 일이 없다.
     */
    @Test
    void 시간이뒤로가도한도를넘기지않는다() throws Exception {
        RecordingSleeper sleeper = new RecordingSleeper();
        NexonRequestRateGate gate = gateOf(1, sleeper);

        ticker.set(10 * ONE_SECOND_NANOS);

        gate.acquire();

        // 단조 시계라면 일어날 수 없는 일이다. 그래도 한도가 무너지지 않아야 한다.
        ticker.addAndGet(-5 * ONE_SECOND_NANOS);

        gate.acquire();

        assertThat(sleeper.waits).isNotEmpty();
    }

    /**
     * 여러 Thread가 동시에 들어와도 관문이 깨지거나 멈추지 않는다.
     *
     * 한도 자체는 위의 단일 Thread 테스트들이 결정적으로 고정한다. 여기서 시각까지
     * 함께 확인하려 하면, 허가를 받은 시점과 그것을 기록하는 시점 사이에 다른
     * Thread가 공유 시계를 밀어 구현이 맞아도 실패한다. 그래서 이 테스트는 자료구조
     * 안전성과 진행 보장만 본다.
     */
    @Test
    void 동시요청에서도관문이멈추지않는다() throws Exception {
        int limit = 5;
        AtomicInteger granted = new AtomicInteger();

        // 자고 나면 시간이 흘러 자리가 열리게 둔다.
        NexonRequestRateGate gate = gateOf(limit, ticker::addAndGet);

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
                    granted.incrementAndGet();
                    return null;
                });
            }

            ready.await(30, TimeUnit.SECONDS);
            go.countDown();

            executor.shutdown();

            // 멈추면 여기서 드러난다.
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS))
                    .isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(granted.get()).isEqualTo(callers);
    }
}
