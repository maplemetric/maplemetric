package com.maplemetric.character.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.maplemetric.character.application.calculator.AdditionalOptionCalculationPolicyV1;
import com.maplemetric.character.application.calculator.AdditionalOptionCalculator;
import com.maplemetric.character.application.port.out.LoadCharacterAbilityPort;
import com.maplemetric.character.application.port.out.LoadCharacterBasicPort;
import com.maplemetric.character.application.port.out.LoadCharacterBasicPort.CharacterBasic;
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
import com.maplemetric.character.application.result.GetCharacterBasicResult;
import com.maplemetric.character.application.result.GetCharacterSummaryResult;
import com.maplemetric.character.application.service.CharacterSnapshotStoreService.StoredSummary;
import com.maplemetric.ranking.api.CharacterRankingQuery;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 수집 소요를 재는지 확인한다.
 *
 * 상한 값에 근거가 없다. 재는 수단이 없으면 값을 정할 근거도 생기지 않는다. 그래서
 * 소요와 끝난 방식이 실제로 남는지 고정한다.
 *
 * 실제로 기다려 확인하면 느리다. 조회가 오래 걸린 상황은 그 조회의 응답에서 단조
 * 증가값을 밀어 만든다.
 */
@ExtendWith(MockitoExtension.class)
class CharacterCollectDurationTest {

    private static final String CHARACTER_NAME = "감점";

    private static final String OCID = "test-ocid";

    private static final Instant NOW =
            Instant.parse("2026-08-01T12:00:00Z");

    private static final Duration MAX_COLLECT_DURATION =
            Duration.ofSeconds(10);

    private static final String METRIC = "character.collect.duration";

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

    private final SimpleMeterRegistry meterRegistry =
            new SimpleMeterRegistry();

    /** 직접 옮기는 단조 증가값이다. */
    private final AtomicLong ticker = new AtomicLong();

    private CharacterQueryService service;

    /** 여기까지 왔다는 표식이다. */
    private static final class Reached extends RuntimeException {
    }

    @BeforeEach
    void setUp() {
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
                        Duration.ofSeconds(30),
                        MAX_COLLECT_DURATION
                ),
                Clock.fixed(NOW, ZoneId.of("UTC")),
                ticker::get,
                meterRegistry
        );
    }

    /**
     * 상한에 걸려 끝나면 그렇게 끝났다고 남긴다.
     *
     * 상한에 걸리는 요청이 실제로 있는지 알아야 값을 조정할 수 있다.
     */
    @Test
    void 상한에걸리면시간초과로남긴다() {
        givenNoSnapshot();
        givenOcid();
        givenBasicTaking(MAX_COLLECT_DURATION.plusSeconds(2));

        assertThatThrownBy(() ->
                service.getCharacterSummary(CHARACTER_NAME));

        assertThat(count("timeout")).isEqualTo(1);
        assertThat(count("success")).isZero();
        assertThat(count("failed")).isZero();

        // 잰 값이 실제로 흐른 시간이어야 한다.
        assertThat(totalMillis("timeout"))
                .isEqualTo(MAX_COLLECT_DURATION.plusSeconds(2).toMillis());
    }

    /**
     * 외부 조회가 실패해 끝나면 실패로 남긴다.
     *
     * 시간 초과와 섞이면 상한이 문제인지 외부가 문제인지 구별할 수 없다.
     */
    @Test
    void 외부조회가실패하면실패로남긴다() {
        givenNoSnapshot();
        givenOcid();

        given(loadCharacterBasicPort.loadCharacterBasic(OCID))
                .willAnswer(invocation -> {
                    ticker.addAndGet(Duration.ofSeconds(3).toNanos());

                    throw new Reached();
                });

        assertThatThrownBy(() ->
                service.getCharacterSummary(CHARACTER_NAME))
                .isInstanceOf(Reached.class);

        assertThat(count("failed")).isEqualTo(1);
        assertThat(count("timeout")).isZero();
        assertThat(totalMillis("failed")).isEqualTo(3_000L);
    }

    /**
     * 저장본을 돌려준 경로는 수집이 아니므로 재지 않는다.
     *
     * 재면 대부분의 조회가 0초 근처로 기록돼 실제 수집 소요가 묻힌다.
     */
    @Test
    void 저장본을돌려준경우에는재지않는다() {
        Instant fetchedAt = NOW.minus(Duration.ofHours(1));

        given(characterSnapshotStoreService
                .findByCharacterName(CHARACTER_NAME))
                .willReturn(Optional.of(new StoredSummary(
                        OCID,
                        fetchedAt,
                        storedSummary(fetchedAt)
                )));

        service.getCharacterSummary(CHARACTER_NAME);

        // 지표 자체는 미리 등록돼 있다. 기록이 없어야 한다.
        assertThat(count("success")).isZero();
        assertThat(count("timeout")).isZero();
        assertThat(count("failed")).isZero();
    }

    /**
     * 측정이 수집 결과를 바꾸지 않는다.
     *
     * 관측을 넣다가 동작이 달라지면 재는 값 자체를 믿을 수 없다.
     */
    @Test
    void 측정이수집결과를바꾸지않는다() {
        givenNoSnapshot();
        givenOcid();
        givenBasicTaking(Duration.ofSeconds(1));

        given(loadCharacterSetEffectPort.loadCharacterSetEffect(OCID))
                .willThrow(new Reached());

        // 상한 안이므로 마지막 조회까지 그대로 간다.
        assertThatThrownBy(() ->
                service.getCharacterSummary(CHARACTER_NAME))
                .isInstanceOf(Reached.class);
    }

    private long count(String outcome) {
        Timer timer = meterRegistry.find(METRIC)
                .tag("outcome", outcome)
                .timer();

        return timer == null ? 0L : timer.count();
    }

    private long totalMillis(String outcome) {
        return (long) meterRegistry.find(METRIC)
                .tag("outcome", outcome)
                .timer()
                .totalTime(TimeUnit.MILLISECONDS);
    }

    private void givenNoSnapshot() {
        given(characterSnapshotStoreService
                .findByCharacterName(CHARACTER_NAME))
                .willReturn(Optional.empty());
    }

    private void givenOcid() {
        given(loadCharacterBasicPort.resolveOcid(CHARACTER_NAME))
                .willReturn(OCID);
    }

    /** 첫 조회가 그만큼 걸린 것으로 만든다. */
    private void givenBasicTaking(Duration elapsed) {
        given(loadCharacterBasicPort.loadCharacterBasic(OCID))
                .willAnswer(invocation -> {
                    ticker.addAndGet(elapsed.toNanos());

                    return basic();
                });
    }

    private CharacterBasic basic() {
        return new CharacterBasic(
                CHARACTER_NAME,
                "루나",
                "남",
                "팬텀",
                "6",
                285,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private GetCharacterSummaryResult storedSummary(Instant fetchedAt) {
        return GetCharacterSummaryResult.of(
                GetCharacterBasicResult.from(basic()),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                fetchedAt.toString()
        );
    }
}
