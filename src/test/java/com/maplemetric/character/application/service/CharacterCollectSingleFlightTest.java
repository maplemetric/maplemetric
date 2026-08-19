package com.maplemetric.character.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.maplemetric.character.application.calculator.AdditionalOptionCalculationPolicyV1;
import com.maplemetric.character.application.calculator.AdditionalOptionCalculator;
import com.maplemetric.character.application.port.out.LoadCharacterAbilityPort;
import com.maplemetric.character.application.port.out.LoadCharacterBasicPort;
import com.maplemetric.character.application.port.out.LoadCharacterDojangPort;
import com.maplemetric.character.application.port.out.LoadCharacterEquipmentPort;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort;
import com.maplemetric.character.application.port.out.LoadCharacterHyperStatPort;
import com.maplemetric.character.application.port.out.LoadCharacterPopularityPort;
import com.maplemetric.character.application.port.out.LoadCharacterSetEffectPort;
import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort;
import com.maplemetric.character.application.port.out.LoadCharacterStatPort;
import com.maplemetric.character.application.port.out.LoadCharacterSymbolPort;
import com.maplemetric.character.application.port.out.LoadCharacterUnionPort;
import com.maplemetric.character.application.properties.CharacterSnapshotProperties;
import com.maplemetric.character.application.result.GetCharacterSummaryResult;
import com.maplemetric.character.application.service.CharacterSnapshotStoreService.StoredSummary;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import com.maplemetric.ranking.api.CharacterRankingQuery;
import org.springframework.http.HttpStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 같은 캐릭터를 동시에 조회해도 Nexon 수집이 한 번만 일어나는지 확인한다.
 *
 * 저장본은 수집이 끝난 뒤에 생긴다. 그래서 수집 중에 도착한 요청은 저장본을 보지
 * 못하고 각자 21회를 쓴다. 셋이 동시에 열면 63회다.
 *
 * 단순히 여러 요청을 동시에 던지고 호출 횟수만 세는 방식은 판별력이 없다. 순차로
 * 실행돼도 통과하기 때문이다. 여기서는 모든 요청이 저장본 없음을 확인한 지점에
 * {@link CyclicBarrier}로 모아 실제로 겹치게 만들고, 두 번째 수집 시도가 오면
 * 그 자리에서 실패시킨다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CharacterCollectSingleFlightTest {

    private static final String CHARACTER_NAME = "감점";

    private static final String OCID = "test-ocid";

    private static final Instant NOW =
            Instant.parse("2026-08-01T12:00:00Z");

    private static final int CALLER_COUNT = 8;

    @Mock
    private LoadCharacterAbilityPort loadCharacterAbilityPort;

    @Mock
    private LoadCharacterBasicPort loadCharacterBasicPort;

    @Mock
    private LoadCharacterDojangPort loadCharacterDojangPort;

    @Mock
    private LoadCharacterEquipmentPort loadCharacterEquipmentPort;

    @Mock
    private LoadCharacterHexaPort loadCharacterHexaPort;

    @Mock
    private LoadCharacterHyperStatPort loadCharacterHyperStatPort;

    @Mock
    private LoadCharacterPopularityPort loadCharacterPopularityPort;

    @Mock
    private LoadCharacterSetEffectPort loadCharacterSetEffectPort;

    @Mock
    private LoadCharacterSkillsPort loadCharacterSkillsPort;

    @Mock
    private LoadCharacterStatPort loadCharacterStatPort;

    @Mock
    private LoadCharacterSymbolPort loadCharacterSymbolPort;

    @Mock
    private LoadCharacterUnionPort loadCharacterUnionPort;

    @Mock
    private CharacterRankingQuery characterRankingQuery;

    @Mock
    private CharacterSnapshotStoreService characterSnapshotStoreService;

    private CharacterQueryService service;

    private ExecutorService executor;

    private final AtomicInteger collectAttempts = new AtomicInteger();

    private final AtomicInteger snapshotLookups = new AtomicInteger();

    /** 대기 상태를 확인하려면 요청을 수행하는 Thread를 알아야 한다. */
    private final List<Thread> callerThreads =
            Collections.synchronizedList(new ArrayList<>());

    private void createService(Duration collectWaitTimeout) {
        service = new CharacterQueryService(
                loadCharacterAbilityPort,
                loadCharacterBasicPort,
                loadCharacterDojangPort,
                loadCharacterEquipmentPort,
                loadCharacterHexaPort,
                loadCharacterHyperStatPort,
                loadCharacterPopularityPort,
                loadCharacterSetEffectPort,
                loadCharacterSkillsPort,
                loadCharacterStatPort,
                loadCharacterSymbolPort,
                loadCharacterUnionPort,
                new AdditionalOptionCalculator(
                        new AdditionalOptionCalculationPolicyV1()
                ),
                characterRankingQuery,
                characterSnapshotStoreService,
                new CharacterSnapshotProperties(
                        Duration.ofMinutes(5),
                        collectWaitTimeout,
                        Duration.ofSeconds(30)
                ),
                Clock.fixed(NOW, ZoneId.of("Asia/Seoul")),
                System::nanoTime,
                new SimpleMeterRegistry()
        );
    }

    @AfterEach
    void 실행기를정리한다() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 겹친 요청 중 하나만 수집한다.
     *
     * 두 번째 수집 시도가 오면 그 자리에서 실패시키므로, 합치기가 없으면 반드시
     * 실패한다. 모든 요청을 저장본 조회 지점에 모아 실제로 겹치게 만든다.
     */
    @Test
    void 동시에같은캐릭터를조회하면수집은한번만한다() throws Exception {
        createService(Duration.ofSeconds(30));

        CyclicBarrier allSawEmpty = new CyclicBarrier(CALLER_COUNT);
        CountDownLatch releaseLeader = new CountDownLatch(1);

        // 저장본은 없다. 모든 요청이 이 지점에서 만난 뒤에야 다음으로 간다.
        given(characterSnapshotStoreService.findByCharacterName(CHARACTER_NAME))
                .willAnswer(invocation -> {
                    // 수집을 맡은 쪽은 시작 직전에 저장본을 다시 확인한다. 그 조회까지
                    // 장벽에 세우면 짝이 없어 멈추므로 최초 요청분만 모은다.
                    if (snapshotLookups.incrementAndGet() <= CALLER_COUNT) {
                        awaitAll(allSawEmpty);
                    }

                    return Optional.empty();
                });

        // 수집의 첫 호출이다. 두 번째부터는 합치기가 깨진 것이다.
        given(loadCharacterBasicPort.resolveOcid(CHARACTER_NAME))
                .willAnswer(invocation -> {
                    if (collectAttempts.incrementAndGet() > 1) {
                        throw new AssertionError(
                                "같은 캐릭터를 두 번 수집했습니다."
                        );
                    }

                    // 수집을 붙들어 나머지가 합류할 시간을 준다.
                    releaseLeader.await(30, TimeUnit.SECONDS);

                    throw new CollectMarker();
                });

        List<Future<GetCharacterSummaryResult>> callers =
                submitAll(CALLER_COUNT, () ->
                        service.getCharacterSummary(CHARACTER_NAME)
                );

        // 수집을 맡은 쪽과 합류한 쪽이 모두 자리를 잡은 뒤에 풀어준다.
        awaitFollowersWaiting(CALLER_COUNT - 1);
        releaseLeader.countDown();

        for (Future<GetCharacterSummaryResult> caller : callers) {
            assertThatThrownBy(() -> caller.get(30, TimeUnit.SECONDS))
                    .hasRootCauseInstanceOf(CollectMarker.class);
        }

        assertThat(collectAttempts.get()).isEqualTo(1);
    }

    /**
     * 수집을 맡은 쪽이 {@link Error}로 죽어도 기다리던 요청이 멈추지 않는다.
     *
     * {@code RuntimeException}만 잡으면 약속이 끝나지 않은 채 지도에서만 지워져
     * 기다리던 요청이 한도까지 매달린다.
     */
    @Test
    void 수집이Error로끝나도기다리던요청이멈추지않는다() throws Exception {
        createService(Duration.ofSeconds(5));

        CyclicBarrier allSawEmpty = new CyclicBarrier(CALLER_COUNT);
        CountDownLatch releaseLeader = new CountDownLatch(1);

        given(characterSnapshotStoreService.findByCharacterName(CHARACTER_NAME))
                .willAnswer(invocation -> {
                    // 수집을 맡은 쪽은 시작 직전에 저장본을 다시 확인한다. 그 조회까지
                    // 장벽에 세우면 짝이 없어 멈추므로 최초 요청분만 모은다.
                    if (snapshotLookups.incrementAndGet() <= CALLER_COUNT) {
                        awaitAll(allSawEmpty);
                    }

                    return Optional.empty();
                });

        given(loadCharacterBasicPort.resolveOcid(CHARACTER_NAME))
                .willAnswer(invocation -> {
                    collectAttempts.incrementAndGet();
                    releaseLeader.await(30, TimeUnit.SECONDS);

                    throw new StackOverflowError("수집 중 Error");
                });

        List<Future<GetCharacterSummaryResult>> callers =
                submitAll(CALLER_COUNT, () ->
                        service.getCharacterSummary(CHARACTER_NAME)
                );

        awaitFollowersWaiting(CALLER_COUNT - 1);
        releaseLeader.countDown();

        // 대기 한도(5초)보다 넉넉히 잡되, 한도에 걸려 끝나면 아래 단정이 실패한다.
        for (Future<GetCharacterSummaryResult> caller : callers) {
            assertThatThrownBy(() -> caller.get(20, TimeUnit.SECONDS))
                    .rootCause()
                    .isInstanceOf(StackOverflowError.class);
        }
    }

    /**
     * 기다린 요청도 수집을 맡은 쪽의 예외를 그대로 받는다.
     *
     * 감싸서 던지면 예외 타입이 달라져 호출부의 저장본 대체 경로가 어긋난다.
     */
    @Test
    void 기다린요청도같은예외를받는다() throws Exception {
        createService(Duration.ofSeconds(30));

        CyclicBarrier allSawEmpty = new CyclicBarrier(CALLER_COUNT);
        CountDownLatch releaseLeader = new CountDownLatch(1);

        given(characterSnapshotStoreService.findByCharacterName(CHARACTER_NAME))
                .willAnswer(invocation -> {
                    // 수집을 맡은 쪽은 시작 직전에 저장본을 다시 확인한다. 그 조회까지
                    // 장벽에 세우면 짝이 없어 멈추므로 최초 요청분만 모은다.
                    if (snapshotLookups.incrementAndGet() <= CALLER_COUNT) {
                        awaitAll(allSawEmpty);
                    }

                    return Optional.empty();
                });

        given(loadCharacterBasicPort.resolveOcid(CHARACTER_NAME))
                .willAnswer(invocation -> {
                    collectAttempts.incrementAndGet();
                    releaseLeader.await(30, TimeUnit.SECONDS);

                    throw new CollectMarker();
                });

        List<Future<GetCharacterSummaryResult>> callers =
                submitAll(CALLER_COUNT, () ->
                        service.getCharacterSummary(CHARACTER_NAME)
                );

        awaitFollowersWaiting(CALLER_COUNT - 1);
        releaseLeader.countDown();

        for (Future<GetCharacterSummaryResult> caller : callers) {
            assertThatThrownBy(() -> caller.get(30, TimeUnit.SECONDS))
                    .rootCause()
                    .isInstanceOf(CollectMarker.class);
        }

        assertThat(collectAttempts.get()).isEqualTo(1);
    }

    /**
     * 수집을 맡기 직전에 저장본을 다시 확인한다.
     *
     * 저장본 확인과 수집 시작 사이에 앞선 수집이 끝날 수 있다. 다시 보지 않으면
     * 이미 있는 저장본을 두고 21회를 또 쓴다.
     *
     * 이 경계는 시간에 의존하지 않는다. 첫 조회는 없음, 두 번째 조회는 있음을
     * 돌려주면 재확인이 없는 구현은 즉시 수집으로 간다.
     */
    @Test
    void 수집직전에저장본을다시확인한다() {
        createService(Duration.ofSeconds(30));

        StoredSummary stored = storedAt(NOW.minus(Duration.ofMinutes(1)));

        given(characterSnapshotStoreService.findByCharacterName(CHARACTER_NAME))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(stored));

        given(loadCharacterBasicPort.resolveOcid(CHARACTER_NAME))
                .willAnswer(invocation -> {
                    throw new AssertionError(
                            "재확인 없이 수집했습니다."
                    );
                });

        GetCharacterSummaryResult result =
                service.getCharacterSummary(CHARACTER_NAME);

        assertThat(result).isSameAs(stored.summary());
    }

    /**
     * 기다리다 한도를 넘기면 시간 초과 계약으로 알린다.
     *
     * 일반 예외를 던지면 전역 처리기가 500으로 바꾼다. 이 저장소는 시간 초과에
     * 504를 쓰기로 했으므로 이 경로만 다른 상태를 주면 계약이 어긋난다.
     */
    @Test
    void 대기한도를넘기면시간초과로알린다() throws Exception {
        createService(Duration.ofMillis(200));

        CyclicBarrier allSawEmpty = new CyclicBarrier(2);
        CountDownLatch releaseLeader = new CountDownLatch(1);

        given(characterSnapshotStoreService.findByCharacterName(CHARACTER_NAME))
                .willAnswer(invocation -> {
                    if (snapshotLookups.incrementAndGet() <= 2) {
                        awaitAll(allSawEmpty);
                    }

                    return Optional.empty();
                });

        given(loadCharacterBasicPort.resolveOcid(CHARACTER_NAME))
                .willAnswer(invocation -> {
                    collectAttempts.incrementAndGet();

                    // 기다리는 쪽이 한도를 넘기도록 충분히 붙든다.
                    releaseLeader.await(30, TimeUnit.SECONDS);

                    throw new CollectMarker();
                });

        List<Future<GetCharacterSummaryResult>> callers =
                submitAll(2, () -> service.getCharacterSummary(CHARACTER_NAME));

        // 둘 중 하나는 수집을 맡고 하나는 기다리다 한도를 넘긴다.
        //
        // 수집을 맡은 쪽은 걸쇠에 붙들려 있으므로 순서대로 결과를 회수하면 그쪽에서
        // 멈춘다. 끝난 것부터 찾아 확인하고, 그 뒤에 걸쇠를 푼다.
        Future<GetCharacterSummaryResult> finished = awaitAnyDone(callers);

        Throwable waiterFailure = null;

        try {
            finished.get();
        } catch (Exception exception) {
            waiterFailure = rootCauseOf(exception);
        }

        releaseLeader.countDown();

        assertThat(waiterFailure)
                .isInstanceOf(CharacterException.class);

        assertThat(((CharacterException) waiterFailure).getErrorCode())
                .isEqualTo(CharacterErrorCode.NEXON_API_TIMEOUT);

        assertThat(CharacterErrorCode.NEXON_API_TIMEOUT.getHttpStatus())
                .isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    /**
     * 1밀리초 미만 대기 한도도 설정으로 받아들인다.
     *
     * 밀리초로 잘라 쓰면 이런 값이 0이 되어 설정한 적 없는 즉시 시간 초과가 된다.
     * 그 결함을 "1밀리초 미만을 금지한다"로 막으면 설정이 표현하던 범위가 줄어든다.
     * 값을 그대로 쓰는 쪽을 택했으므로 여기서는 그 범위가 유지되는지 고정한다.
     *
     * 나노초 변환 자체는 이 테스트로 판별하지 못한다. 잘림으로 잃는 값이 1밀리초
     * 미만이라, 동작으로 구별하려면 그만큼의 시간을 재야 하고 그 측정은 불안정하다.
     */
    @Test
    void 일밀리초미만대기한도도설정할수있다() {
        Duration subMillisecond = Duration.ofNanos(500_000);

        assertThat(subMillisecond.toMillis()).isZero();
        assertThat(subMillisecond.toNanos()).isPositive();

        CharacterSnapshotProperties properties =
                new CharacterSnapshotProperties(
                        Duration.ofMinutes(5),
                        subMillisecond,
                        Duration.ofSeconds(30)
                );

        assertThat(properties.collectWaitTimeout()).isEqualTo(subMillisecond);
    }

    /**
     * 다른 캐릭터의 조회는 서로 기다리지 않는다.
     */
    @Test
    void 다른캐릭터는서로기다리지않는다() throws Exception {
        createService(Duration.ofSeconds(30));

        CountDownLatch holdFirst = new CountDownLatch(1);

        given(characterSnapshotStoreService.findByCharacterName(any()))
                .willReturn(Optional.empty());

        given(loadCharacterBasicPort.resolveOcid("이름1"))
                .willAnswer(invocation -> {
                    holdFirst.await(30, TimeUnit.SECONDS);
                    throw new CollectMarker();
                });

        given(loadCharacterBasicPort.resolveOcid("이름2"))
                .willAnswer(invocation -> {
                    throw new CollectMarker();
                });

        executor = Executors.newFixedThreadPool(2);

        Future<?> first = executor.submit(() ->
                service.getCharacterSummary("이름1")
        );

        // 첫 번째가 붙들려 있어도 두 번째는 곧바로 끝나야 한다.
        Future<?> second = executor.submit(() ->
                service.getCharacterSummary("이름2")
        );

        assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS))
                .hasRootCauseInstanceOf(CollectMarker.class);

        holdFirst.countDown();

        assertThatThrownBy(() -> first.get(30, TimeUnit.SECONDS))
                .hasRootCauseInstanceOf(CollectMarker.class);
    }

    /**
     * 수집이 끝난 뒤에는 다시 수집할 수 있다.
     *
     * 진행 표시가 남으면 그 캐릭터는 영영 갱신되지 않는다.
     */
    @Test
    void 수집이끝나면다시수집할수있다() {
        createService(Duration.ofSeconds(30));

        given(characterSnapshotStoreService.findByCharacterName(CHARACTER_NAME))
                .willReturn(Optional.empty());

        given(loadCharacterBasicPort.resolveOcid(CHARACTER_NAME))
                .willAnswer(invocation -> {
                    collectAttempts.incrementAndGet();
                    throw new CollectMarker();
                });

        assertThatThrownBy(() ->
                service.getCharacterSummary(CHARACTER_NAME)
        ).isInstanceOf(CollectMarker.class);

        assertThatThrownBy(() ->
                service.getCharacterSummary(CHARACTER_NAME)
        ).isInstanceOf(CollectMarker.class);

        assertThat(collectAttempts.get()).isEqualTo(2);
    }

    private List<Future<GetCharacterSummaryResult>> submitAll(
            int count,
            Callable<GetCharacterSummaryResult> work
    ) {
        executor = Executors.newFixedThreadPool(count, runnable -> {
            Thread thread = new Thread(runnable);
            callerThreads.add(thread);
            return thread;
        });

        List<Future<GetCharacterSummaryResult>> callers = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            callers.add(executor.submit(work));
        }

        return callers;
    }

    /**
     * 합류한 요청이 모두 결과를 기다리는 지점에 들어갈 때까지 둔다.
     *
     * 수집을 맡은 쪽은 합류자가 아니므로 세지 않는다.
     *
     * 정해진 시간만 쉬고 넘어가면 느린 환경에서 후속 요청이 아직 합류하지 못한 채
     * 수집이 끝나고, 그 요청이 새로 수집을 맡아 구현이 옳아도 실패한다.
     * 시간이 아니라 실제 상태를 기다린다.
     *
     * 대기 상태를 세는 것으로는 안 된다. 장벽에 묶인 Thread도 대기 상태이고,
     * 장벽이 열린 뒤에도 각자 락을 다시 잡는 동안 여전히 대기 상태다. 그 사이에
     * 조건이 참이 되면 아무도 합류하기 전에 걸쇠가 풀려 위와 똑같은 실패가 난다.
     *
     * 그래서 상태가 아니라 위치를 본다. 진행 중인 수집을 기다리는 지점에 실제로
     * 들어간 Thread만 센다. 거기 있다는 것은 합류가 끝났다는 뜻이다.
     */
    private void awaitFollowersWaiting(int expected) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);

        while (System.nanoTime() < deadline) {
            long waiting = callerThreads.stream()
                    .filter(thread -> thread != Thread.currentThread())
                    .filter(this::isAwaitingCollected)
                    .count();

            if (waiting >= expected) {
                return;
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        }

        throw new IllegalStateException(
                "합류한 요청이 대기 지점에 도달하지 않았습니다."
        );
    }

    /**
     * 진행 중인 수집을 기다리는 지점에 들어가 있는지 본다.
     *
     * 대기 상태만으로는 장벽에 묶인 것과 구별되지 않는다. 실제로 어느 지점에
     * 있는지를 봐야 합류가 끝났다고 말할 수 있다.
     *
     * 이 지점의 이름이 바뀌면 합류를 하나도 세지 못해 대기가 상한까지 가고
     * 위 메시지로 끝난다. 조용히 통과하지 않는다.
     */
    private boolean isAwaitingCollected(Thread thread) {
        for (StackTraceElement frame : thread.getStackTrace()) {
            if (frame.getClassName()
                    .endsWith("CharacterQueryService")
                    && "awaitCollected".equals(frame.getMethodName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 먼저 끝난 요청을 찾는다.
     *
     * 수집을 맡은 쪽은 걸쇠에 붙들려 있어 결과가 없다. 순서대로 회수하면 그쪽에서
     * 걸쇠가 풀릴 때까지 멈추므로, 빌드가 순서에 따라 길어진다.
     */
    private Future<GetCharacterSummaryResult> awaitAnyDone(
            List<Future<GetCharacterSummaryResult>> callers
    ) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);

        while (System.nanoTime() < deadline) {
            for (Future<GetCharacterSummaryResult> caller : callers) {
                if (caller.isDone()) {
                    return caller;
                }
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        }

        throw new IllegalStateException("끝난 요청이 없습니다.");
    }

    private Throwable rootCauseOf(Throwable throwable) {
        Throwable cause = throwable;

        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        return cause;
    }

    private void awaitAll(CyclicBarrier barrier) {
        try {
            barrier.await(30, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "모든 요청이 저장본 조회 지점에 모이지 않았습니다.",
                    exception
            );
        }
    }

    private StoredSummary storedAt(Instant fetchedAt) {
        return new StoredSummary(
                OCID,
                fetchedAt,
                GetCharacterSummaryResult.of(
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null,
                        fetchedAt.toString()
                )
        );
    }

    /** 수집 경로에 들어갔음을 알리는 표식이다. */
    private static final class CollectMarker extends RuntimeException {

        private CollectMarker() {
            super("수집 경로 진입");
        }
    }
}
