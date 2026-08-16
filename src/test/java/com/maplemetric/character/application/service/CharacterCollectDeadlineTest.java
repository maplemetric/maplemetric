package com.maplemetric.character.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import com.maplemetric.ranking.api.CharacterRankingQuery;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 수집 전체에 상한이 걸리는지 확인한다.
 *
 * 호출 하나하나에는 상한이 있지만 수집 전체에는 없었다. 느린 호출이 이어지면 요청
 * Thread가 그만큼 묶이고, 기다리던 요청이 이미 포기한 뒤에도 수집은 계속 Nexon을
 * 부른다.
 *
 * 실제로 기다려 확인하면 느리고 결과가 환경에 따라 달라진다. 시계를 직접 옮기고,
 * 조회 하나가 오래 걸린 상황은 그 조회의 응답에서 시계를 밀어 만든다.
 *
 * 상한은 설정값 10초로 둔다. 30초를 상수로 박은 구현은 여기서 드러난다.
 */
@ExtendWith(MockitoExtension.class)
class CharacterCollectDeadlineTest {

    private static final String CHARACTER_NAME = "감점";

    private static final String OCID = "test-ocid";

    private static final Instant NOW =
            Instant.parse("2026-08-01T12:00:00Z");

    /** 기본값 30초와 구별되는 값이어야 설정을 읽는지 판별할 수 있다. */
    private static final Duration MAX_COLLECT_DURATION =
            Duration.ofSeconds(10);

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

    private final MovableClock clock = new MovableClock(NOW);

    private CharacterQueryService service;

    /** 직접 옮기는 시계다. */
    private static final class MovableClock extends Clock {

        private Instant instant;

        private MovableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration amount) {
            instant = instant.plus(amount);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

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
                clock
        );
    }

    /**
     * 상한을 넘기면 남은 조회를 시작하지 않는다.
     *
     * 첫 조회가 상한을 넘겨 걸린 상황이다. 상한이 없으면 나머지 열두 번이 그대로
     * 나간다.
     */
    @Test
    void 상한을넘기면남은조회를시작하지않는다() {
        givenNoSnapshot();
        givenOcid();
        givenBasicTaking(MAX_COLLECT_DURATION.plusSeconds(1));

        assertThatThrownBy(() -> service.getCharacterSummary(CHARACTER_NAME))
                .isInstanceOf(CharacterException.class);

        verify(loadCharacterBasicPort).loadCharacterBasic(OCID);

        // 첫 조회 다음은 하나도 나가지 않아야 한다.
        verify(loadCharacterStatPort, never()).loadCharacterStat(any());
        verifyNoInteractions(
                loadCharacterAbilityPort,
                loadCharacterDojangPort,
                loadCharacterEquipmentPort,
                loadCharacterHexaPort,
                loadCharacterHyperStatPort,
                loadCharacterPopularityPort,
                loadCharacterSetEffectPort,
                loadCharacterSkillsPort,
                loadCharacterSymbolPort,
                loadCharacterUnionPort,
                characterRankingQuery
        );
    }

    /**
     * 시간 초과는 이미 있는 계약으로 알린다.
     *
     * 새 오류 코드를 만들면 소비 측이 알지 못하는 응답이 하나 늘어난다.
     */
    @Test
    void 상한을넘기면기존시간초과계약으로알린다() {
        givenNoSnapshot();
        givenOcid();
        givenBasicTaking(MAX_COLLECT_DURATION.plusSeconds(1));

        assertThatThrownBy(() -> service.getCharacterSummary(CHARACTER_NAME))
                .isInstanceOf(CharacterException.class)
                .extracting(exception ->
                        ((CharacterException) exception).getErrorCode())
                .isEqualTo(CharacterErrorCode.NEXON_API_TIMEOUT);
    }

    /**
     * 중간까지 받은 결과는 저장하지 않는다.
     *
     * 일부만 담긴 저장본을 남기면 다음 조회가 그것을 완성본으로 쓴다.
     */
    @Test
    void 상한을넘기면중간결과를저장하지않는다() {
        givenNoSnapshot();
        givenOcid();
        givenBasicTaking(MAX_COLLECT_DURATION.plusSeconds(1));

        assertThatThrownBy(() -> service.getCharacterSummary(CHARACTER_NAME))
                .isInstanceOf(CharacterException.class);

        verify(characterSnapshotStoreService, never())
                .store(any(), any(), any());
    }

    /**
     * 저장본이 있으면 상한을 넘겨도 그것을 돌려준다.
     *
     * 갱신이 늦었다고 이미 가지고 있던 데이터까지 잃게 하지 않는다.
     */
    @Test
    void 상한을넘겨도저장본이있으면돌려준다() {
        Instant fetchedAt = NOW.minus(Duration.ofHours(1));

        given(characterSnapshotStoreService
                .findByCharacterName(CHARACTER_NAME))
                .willReturn(Optional.of(new StoredSummary(
                        OCID,
                        fetchedAt,
                        storedSummary(fetchedAt)
                )));

        givenOcid();
        givenBasicTaking(MAX_COLLECT_DURATION.plusSeconds(1));

        GetCharacterSummaryResult result =
                service.getCharacterSummary(CHARACTER_NAME, true);

        assertThat(result.dataUpdatedAt()).isEqualTo(fetchedAt.toString());
    }

    /**
     * 상한 안에 끝나면 마지막 조회까지 간다.
     *
     * 상한을 잘못 걸면 정상 수집이 중간에 잘린다. 마지막 조회에 표식을 두어 거기까지
     * 도달했는지 본다. 앞선 조회들의 결과는 모든 조회가 끝난 뒤에야 쓰이므로 굳이
     * 값을 주지 않는다.
     */
    @Test
    void 상한안에끝나면마지막조회까지간다() {
        givenNoSnapshot();
        givenOcid();

        // 상한 10초에 1초를 남긴다. 여유가 아무리 적어도 남아 있으면 계속한다.
        givenBasicTaking(MAX_COLLECT_DURATION.minusSeconds(1));

        given(loadCharacterSetEffectPort.loadCharacterSetEffect(OCID))
                .willThrow(new Reached());

        assertThatThrownBy(() -> service.getCharacterSummary(CHARACTER_NAME))
                .isInstanceOf(Reached.class);
    }

    /**
     * 잘못된 캐릭터명은 상한을 넘긴 뒤에도 이름 오류로 알린다.
     *
     * 상한은 저장본을 찾기 전에 잡은 시각에서 시작한다. 저장본 조회가 상한만큼
     * 걸리면 이름 검사에 닿기 전에 이미 상한을 넘긴 상태가 된다. 그때 상한을 먼저
     * 보면 잘못 입력한 이름이 시간 초과로 보고되어, 고칠 수 있는 잘못을 외부 탓으로
     * 돌린다.
     */
    @Test
    void 상한을넘긴뒤에도잘못된캐릭터명은이름오류로알린다() {
        String invalidName = "가";

        given(characterSnapshotStoreService
                .findByCharacterName(invalidName))
                .willAnswer(invocation -> {
                    clock.advance(MAX_COLLECT_DURATION.plusSeconds(1));

                    return Optional.empty();
                });

        assertThatThrownBy(() -> service.getCharacterSummary(invalidName))
                .isInstanceOf(CharacterException.class)
                .extracting(exception ->
                        ((CharacterException) exception).getErrorCode())
                .isEqualTo(CharacterErrorCode.INVALID_CHARACTER_NAME);
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
                    clock.advance(elapsed);

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
