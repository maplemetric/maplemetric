package com.maplemetric.character.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.maplemetric.character.application.result.GetCharacterBasicResult;
import com.maplemetric.character.application.result.GetCharacterSummaryResult;
import com.maplemetric.character.application.service.CharacterSnapshotStoreService.StoredSummary;
import com.maplemetric.character.application.properties.CharacterSnapshotProperties;
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
 * 저장본이 있을 때 Nexon을 다시 부르지 않는지 확인한다.
 *
 * 종합 조회 한 번이 Nexon을 21회 호출한다. 이 동작이 깨지면 호출량이 조용히 21배가
 * 된다. 여기서는 Nexon Port를 하나도 호출하지 않는 것으로 그것을 고정한다.
 */
@ExtendWith(MockitoExtension.class)
class CharacterSummaryStoreBehaviorTest {

    private static final String CHARACTER_NAME = "감점";

    private static final String OCID = "test-ocid";

    private static final Instant NOW =
            Instant.parse("2026-08-01T12:00:00Z");

    private static final Duration MIN_REFRESH_INTERVAL =
            Duration.ofMinutes(5);

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
                        MIN_REFRESH_INTERVAL,
                        Duration.ofSeconds(30),
                        Duration.ofSeconds(30)
                ),
                Clock.fixed(NOW, ZoneId.of("Asia/Seoul")),
                System::nanoTime
        );
    }

    @Test
    void 저장본이있으면Nexon을부르지않는다() {
        givenStored(NOW.minus(Duration.ofDays(1)));

        GetCharacterSummaryResult result =
                service.getCharacterSummary(CHARACTER_NAME);

        assertThat(result.basic().characterName())
                .isEqualTo(CHARACTER_NAME);

        verifyNoNexonCall();
        verify(characterSnapshotStoreService, never())
                .store(any(), any(), any());
    }

    @Test
    void 저장본이없으면수집경로로간다() {
        given(characterSnapshotStoreService
                .findByCharacterName(CHARACTER_NAME))
                .willReturn(Optional.empty());
        given(loadCharacterBasicPort.resolveOcid(CHARACTER_NAME))
                .willThrow(new IllegalStateException("수집 경로 진입"));

        assertThatEntersCollectPath();

        verify(loadCharacterBasicPort).resolveOcid(CHARACTER_NAME);
    }

    @Test
    void 갱신수집이실패하면저장본을돌려준다() {
        Instant fetchedAt = NOW.minus(Duration.ofHours(1));

        givenStored(fetchedAt);

        given(loadCharacterBasicPort.resolveOcid(CHARACTER_NAME))
                .willThrow(new IllegalStateException("수집 실패"));

        GetCharacterSummaryResult result =
                service.getCharacterSummary(CHARACTER_NAME, true);

        // 갱신에 실패했다고 이미 가지고 있던 데이터까지 잃지 않는다.
        assertThat(result.basic().characterName())
                .isEqualTo(CHARACTER_NAME);
        assertThat(result.dataUpdatedAt())
                .isEqualTo(fetchedAt.toString());
    }

    @Test
    void 저장본이없는데수집이실패하면예외를올린다() {
        given(characterSnapshotStoreService
                .findByCharacterName(CHARACTER_NAME))
                .willReturn(Optional.empty());
        given(loadCharacterBasicPort.resolveOcid(CHARACTER_NAME))
                .willThrow(new IllegalStateException("수집 실패"));

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() ->
                        service.getCharacterSummary(CHARACTER_NAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("수집 실패");
    }

    @Test
    void 갱신최소간격안이면저장본을유지한다() {
        // 4분 전에 갱신했다. 최소 간격 5분 안이다.
        givenStored(NOW.minus(Duration.ofMinutes(4)));

        service.getCharacterSummary(CHARACTER_NAME, true);

        verifyNoNexonCall();
    }

    @Test
    void 갱신최소간격이지나면다시수집한다() {
        givenStored(NOW.minus(Duration.ofMinutes(5)));

        given(loadCharacterBasicPort.resolveOcid(CHARACTER_NAME))
                .willThrow(new IllegalStateException("수집 경로 진입"));

        service.getCharacterSummary(CHARACTER_NAME, true);

        // 수집을 시도했다. 실패해서 저장본으로 되돌아온 것은 별도 테스트가 다룬다.
        verify(loadCharacterBasicPort).resolveOcid(CHARACTER_NAME);
    }

    private void givenStored(Instant fetchedAt) {
        given(characterSnapshotStoreService
                .findByCharacterName(CHARACTER_NAME))
                .willReturn(Optional.of(new StoredSummary(
                        OCID,
                        fetchedAt,
                        storedSummary(fetchedAt)
                )));
    }

    /**
     * 수집 경로에 들어갔는지만 확인한다.
     *
     * 21개 Port를 모두 Stub하지 않고, 첫 호출인 ocid 조회에서 표식 예외를 던져
     * 저장본을 쓰지 않고 수집으로 갔음을 확인한다.
     */
    private void assertThatEntersCollectPath() {
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() ->
                        service.getCharacterSummary(CHARACTER_NAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("수집 경로 진입");
    }

    private void verifyNoNexonCall() {
        verifyNoInteractions(
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
                characterRankingQuery
        );
    }

    private GetCharacterSummaryResult storedSummary(Instant fetchedAt) {
        return GetCharacterSummaryResult.of(
                GetCharacterBasicResult.from(new CharacterBasic(
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
                )),
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

    @Test
    void 저장본의조회시각을응답에그대로쓴다() {
        Instant fetchedAt = NOW.minus(Duration.ofHours(3));

        givenStored(fetchedAt);

        assertThat(service.getCharacterSummary(CHARACTER_NAME)
                .dataUpdatedAt())
                .isEqualTo(fetchedAt.toString());
    }

    @Test
    void 갱신요청이아니면오래된저장본도그대로쓴다() {
        givenStored(NOW.minus(Duration.ofDays(30)));

        service.getCharacterSummary(CHARACTER_NAME, false);

        verifyNoNexonCall();
        verify(characterSnapshotStoreService)
                .findByCharacterName(eq(CHARACTER_NAME));
    }
}
