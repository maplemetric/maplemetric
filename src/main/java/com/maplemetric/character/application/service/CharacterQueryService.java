package com.maplemetric.character.application.service;

import com.maplemetric.character.application.calculator.AdditionalOptionCalculator;
import com.maplemetric.character.application.port.out.LoadCharacterAbilityPort;
import com.maplemetric.character.application.port.out.LoadCharacterAbilityPort.CharacterAbility;
import com.maplemetric.character.application.port.out.LoadCharacterBasicPort;
import com.maplemetric.character.application.port.out.LoadCharacterBasicPort.CharacterBasic;
import com.maplemetric.character.application.port.out.LoadCharacterDojangPort;
import com.maplemetric.character.application.port.out.LoadCharacterDojangPort.CharacterDojang;
import com.maplemetric.character.application.port.out.LoadCharacterEquipmentPort;
import com.maplemetric.character.application.port.out.LoadCharacterEquipmentPort.CharacterEquipment;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.CharacterHexa;
import com.maplemetric.character.application.port.out.LoadCharacterHyperStatPort;
import com.maplemetric.character.application.port.out.LoadCharacterHyperStatPort.CharacterHyperStat;
import com.maplemetric.character.application.port.out.LoadCharacterPopularityPort;
import com.maplemetric.character.application.port.out.LoadCharacterPopularityPort.CharacterPopularity;
import com.maplemetric.character.application.port.out.LoadCharacterSetEffectPort;
import com.maplemetric.character.application.port.out.LoadCharacterSetEffectPort.CharacterSetEffect;
import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort;
import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort.CharacterSkills;
import com.maplemetric.character.application.port.out.LoadCharacterStatPort;
import com.maplemetric.character.application.port.out.LoadCharacterStatPort.CharacterStat;
import com.maplemetric.character.application.port.out.LoadCharacterSymbolPort;
import com.maplemetric.character.application.port.out.LoadCharacterSymbolPort.CharacterSymbol;
import com.maplemetric.character.application.port.out.LoadCharacterUnionPort;
import com.maplemetric.character.application.port.out.LoadCharacterUnionPort.CharacterUnion;
import com.maplemetric.character.application.result.GetCharacterAbilityResult;
import com.maplemetric.character.application.result.GetCharacterBasicResult;
import com.maplemetric.character.application.result.GetCharacterDojangResult;
import com.maplemetric.character.application.result.GetCharacterEquipmentResult;
import com.maplemetric.character.application.result.GetCharacterHexaResult;
import com.maplemetric.character.application.result.GetCharacterHyperStatResult;
import com.maplemetric.character.application.result.GetCharacterPopularityResult;
import com.maplemetric.character.application.result.GetCharacterRankingResult;
import com.maplemetric.character.application.result.GetCharacterSetEffectResult;
import com.maplemetric.character.application.result.GetCharacterSkillsResult;
import com.maplemetric.character.application.result.GetCharacterStatResult;
import com.maplemetric.character.application.result.GetCharacterSummaryResult;
import com.maplemetric.character.application.result.GetCharacterSymbolResult;
import com.maplemetric.character.application.result.GetCharacterUnionResult;
import com.maplemetric.character.application.service.CharacterSnapshotStoreService.StoredSummary;
import com.maplemetric.character.application.properties.CharacterSnapshotProperties;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import com.maplemetric.ranking.api.CharacterRanking;
import com.maplemetric.ranking.api.CharacterRankingQuery;
import com.maplemetric.ranking.api.CharacterRankingQueryException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class CharacterQueryService {

    private static final Pattern CHARACTER_NAME_PATTERN = Pattern.compile("^[가-힣A-Za-z0-9]+$");

    private static final int MIN_CHARACTER_NAME_LENGTH = 4;
    private static final int MAX_CHARACTER_NAME_LENGTH = 12;

    private static final int KOREAN_CHARACTER_WEIGHT = 2;
    private static final int ENGLISH_NUMBER_CHARACTER_WEIGHT = 1;

    private final LoadCharacterAbilityPort loadCharacterAbilityPort;
    private final LoadCharacterBasicPort loadCharacterBasicPort;
    private final LoadCharacterDojangPort loadCharacterDojangPort;
    private final LoadCharacterEquipmentPort loadCharacterEquipmentPort;
    private final LoadCharacterHexaPort loadCharacterHexaPort;
    private final LoadCharacterHyperStatPort loadCharacterHyperStatPort;
    private final LoadCharacterPopularityPort loadCharacterPopularityPort;
    private final LoadCharacterSetEffectPort loadCharacterSetEffectPort;
    private final LoadCharacterSkillsPort loadCharacterSkillsPort;
    private final LoadCharacterStatPort loadCharacterStatPort;
    private final LoadCharacterSymbolPort loadCharacterSymbolPort;
    private final LoadCharacterUnionPort loadCharacterUnionPort;
    private final AdditionalOptionCalculator additionalOptionCalculator;
    private final CharacterRankingQuery characterRankingQuery;
    private final CharacterSnapshotStoreService characterSnapshotStoreService;
    private final CharacterSnapshotProperties characterSnapshotProperties;
    private final Clock clock;

    /**
     * 캐릭터명별로 진행 중인 수집이다.
     *
     * 수집이 끝나면 성공·실패와 무관하게 지운다. 남아 있으면 그 캐릭터는 다시
     * 수집되지 않는다.
     */
    private final ConcurrentMap<String, CompletableFuture<GetCharacterSummaryResult>>
            collecting = new ConcurrentHashMap<>();

    @Autowired
    public CharacterQueryService(
            LoadCharacterAbilityPort loadCharacterAbilityPort,
            LoadCharacterBasicPort loadCharacterBasicPort,
            LoadCharacterDojangPort loadCharacterDojangPort,
            LoadCharacterEquipmentPort loadCharacterEquipmentPort,
            LoadCharacterHexaPort loadCharacterHexaPort,
            LoadCharacterHyperStatPort loadCharacterHyperStatPort,
            LoadCharacterPopularityPort loadCharacterPopularityPort,
            LoadCharacterSetEffectPort loadCharacterSetEffectPort,
            LoadCharacterSkillsPort loadCharacterSkillsPort,
            LoadCharacterStatPort loadCharacterStatPort,
            LoadCharacterSymbolPort loadCharacterSymbolPort,
            LoadCharacterUnionPort loadCharacterUnionPort,
            AdditionalOptionCalculator additionalOptionCalculator,
            CharacterRankingQuery characterRankingQuery,
            CharacterSnapshotStoreService characterSnapshotStoreService,
            CharacterSnapshotProperties characterSnapshotProperties
    ) {
        this(
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
                additionalOptionCalculator,
                characterRankingQuery,
                characterSnapshotStoreService,
                characterSnapshotProperties,
                Clock.systemUTC()
        );
    }

    CharacterQueryService(
            LoadCharacterAbilityPort loadCharacterAbilityPort,
            LoadCharacterBasicPort loadCharacterBasicPort,
            LoadCharacterDojangPort loadCharacterDojangPort,
            LoadCharacterEquipmentPort loadCharacterEquipmentPort,
            LoadCharacterHexaPort loadCharacterHexaPort,
            LoadCharacterHyperStatPort loadCharacterHyperStatPort,
            LoadCharacterPopularityPort loadCharacterPopularityPort,
            LoadCharacterSetEffectPort loadCharacterSetEffectPort,
            LoadCharacterSkillsPort loadCharacterSkillsPort,
            LoadCharacterStatPort loadCharacterStatPort,
            LoadCharacterSymbolPort loadCharacterSymbolPort,
            LoadCharacterUnionPort loadCharacterUnionPort,
            AdditionalOptionCalculator additionalOptionCalculator,
            CharacterRankingQuery characterRankingQuery,
            CharacterSnapshotStoreService characterSnapshotStoreService,
            CharacterSnapshotProperties characterSnapshotProperties,
            Clock clock
    ) {
        this.loadCharacterAbilityPort = loadCharacterAbilityPort;
        this.loadCharacterBasicPort =
                loadCharacterBasicPort;
        this.loadCharacterDojangPort = loadCharacterDojangPort;
        this.loadCharacterEquipmentPort =
                loadCharacterEquipmentPort;
        this.loadCharacterHexaPort = loadCharacterHexaPort;
        this.loadCharacterHyperStatPort = loadCharacterHyperStatPort;
        this.loadCharacterPopularityPort =
                loadCharacterPopularityPort;
        this.loadCharacterSetEffectPort = loadCharacterSetEffectPort;
        this.loadCharacterSkillsPort = loadCharacterSkillsPort;
        this.loadCharacterStatPort = loadCharacterStatPort;
        this.loadCharacterSymbolPort = loadCharacterSymbolPort;
        this.loadCharacterUnionPort = loadCharacterUnionPort;
        this.additionalOptionCalculator =
                additionalOptionCalculator;
        this.characterRankingQuery = characterRankingQuery;
        this.characterSnapshotStoreService = characterSnapshotStoreService;
        this.characterSnapshotProperties = characterSnapshotProperties;
        this.clock = clock;
    }

    public GetCharacterBasicResult getCharacterBasic(
            String characterName
    ) {
        String ocid = getValidatedOcid(characterName);

        CharacterBasic basic =
                loadCharacterBasicPort.loadCharacterBasic(ocid);

        return GetCharacterBasicResult.from(basic);
    }

    public GetCharacterEquipmentResult getCharacterEquipment(
            String characterName
    ) {
        String ocid = getValidatedOcid(characterName);

        CharacterEquipment equipment =
                loadCharacterEquipmentPort
                        .loadCharacterEquipment(ocid);

        return GetCharacterEquipmentResult.from(
                equipment,
                equipment.characterClass(),
                additionalOptionCalculator
        );
    }

    public GetCharacterSummaryResult getCharacterSummary(
            String characterName
    ) {
        return getCharacterSummary(characterName, false);
    }

    /**
     * 캐릭터 종합 정보를 조회한다.
     *
     * 저장본이 있으면 그것을 돌려준다. 한 번의 종합 조회가 Nexon을 21회 호출하므로
     * 같은 캐릭터를 다시 보는 것만으로 호출량을 쓰지 않게 한다.
     *
     * {@code refresh}는 사용자가 갱신을 요청한 경우다. 저장본이 있어도 다시 수집한다.
     * 다만 최근에 이미 갱신했다면 저장본을 그대로 돌려준다. 연타로 21회씩 소비하지
     * 않게 하려는 것이며, 응답의 {@code dataUpdatedAt}이 그대로라 소비 측이 갱신되지
     * 않았음을 알 수 있다.
     */
    public GetCharacterSummaryResult getCharacterSummary(
            String characterName,
            boolean refresh
    ) {
        Instant now = Instant.now(clock);

        Optional<StoredSummary> stored =
                characterSnapshotStoreService
                        .findByCharacterName(characterName);

        if (stored.isPresent() && !shouldFetch(stored.get(), refresh, now)) {
            return stored.get().summary();
        }

        try {
            return collectOnce(characterName, refresh, now);
        } catch (RuntimeException exception) {
            if (stored.isEmpty()) {
                throw exception;
            }

            // 갱신에 실패했다고 이미 가지고 있던 데이터까지 잃게 하지 않는다.
            // 응답의 dataUpdatedAt이 그대로라 갱신되지 않았음을 알 수 있다.
            log.warn(
                    "갱신 수집이 실패해 저장본을 돌려줍니다. characterName={}",
                    characterName,
                    exception
            );

            return stored.get().summary();
        }
    }

    /**
     * 같은 캐릭터를 동시에 조회해도 한 번만 수집한다.
     *
     * 저장본은 수집이 끝난 뒤에 생긴다. 그래서 수집 중에 도착한 요청은 저장본을 보지
     * 못하고 각자 21회를 쓴다. 같은 캐릭터를 셋이 동시에 열면 63회다.
     *
     * 먼저 도착한 요청이 수집하고 나머지는 그 결과를 함께 쓴다.
     */
    private GetCharacterSummaryResult collectOnce(
            String characterName,
            boolean refresh,
            Instant now
    ) {
        CompletableFuture<GetCharacterSummaryResult> mine =
                new CompletableFuture<>();

        CompletableFuture<GetCharacterSummaryResult> ongoing =
                collecting.putIfAbsent(characterName, mine);

        if (ongoing != null) {
            return awaitCollected(ongoing, characterName);
        }

        try {
            GetCharacterSummaryResult result =
                    collectAfterRecheck(characterName, refresh, now);

            mine.complete(result);

            return result;
        } catch (RuntimeException | Error throwable) {
            // Error까지 잡지 않으면 기다리던 요청이 끝나지 않는 약속을 붙들고
            // 영원히 멈춘다. 잡은 뒤 그대로 다시 던진다.
            mine.completeExceptionally(throwable);

            throw throwable;
        } finally {
            collecting.remove(characterName, mine);
        }
    }

    /**
     * 수집을 시작하기 직전에 저장본을 다시 확인한다.
     *
     * 저장본 확인과 수집 시작 사이에는 틈이 있다. 그 사이에 앞선 수집이 끝났다면
     * 이 요청은 이미 있는 저장본을 못 본 채 21회를 다시 쓰게 된다.
     */
    private GetCharacterSummaryResult collectAfterRecheck(
            String characterName,
            boolean refresh,
            Instant now
    ) {
        Optional<StoredSummary> stored =
                characterSnapshotStoreService
                        .findByCharacterName(characterName);

        if (stored.isPresent() && !shouldFetch(stored.get(), refresh, now)) {
            return stored.get().summary();
        }

        return collectCharacterSummary(characterName, now);
    }

    /**
     * 진행 중인 수집의 결과를 기다린다.
     *
     * 기다리는 쪽이 한도를 넘겨도 진행 중인 수집 자체는 건드리지 않는다. 공유하는
     * 약속에 한도를 걸면 한 요청 때문에 나머지까지 실패한다.
     */
    private GetCharacterSummaryResult awaitCollected(
            CompletableFuture<GetCharacterSummaryResult> ongoing,
            String characterName
    ) {
        try {
            return ongoing.get(
                    characterSnapshotProperties.collectWaitTimeout().toMillis(),
                    TimeUnit.MILLISECONDS
            );
        } catch (InterruptedException exception) {
            // 중단은 시간 초과가 아니라 서버 쪽 사정이다. 504로 바꾸면 외부가 늦은
            // 것처럼 보이므로 처리하지 못한 예외로 남긴다.
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "캐릭터 수집을 기다리는 중 중단되었습니다.",
                    exception
            );
        } catch (TimeoutException exception) {
            // 수집이 늦어 기다리지 못한 것이므로 시간 초과다. 여기서 일반 예외를
            // 던지면 이미 504를 쓰기로 한 시간 초과가 이 경로에서만 500이 된다.
            log.warn(
                    "진행 중인 캐릭터 수집을 기다리다 한도를 넘겼습니다. "
                            + "characterName={}",
                    characterName
            );

            throw new CharacterException(CharacterErrorCode.NEXON_API_TIMEOUT);
        } catch (ExecutionException exception) {
            throw rethrow(exception.getCause());
        }
    }

    /**
     * 수집을 맡은 쪽의 실패를 기다린 쪽에도 그대로 전한다.
     *
     * 감싸서 던지면 예외 타입이 달라져 호출부의 처리 경로가 바뀐다.
     */
    private RuntimeException rethrow(Throwable cause) {
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }

        if (cause instanceof Error error) {
            throw error;
        }

        return new IllegalStateException("캐릭터 수집이 실패했습니다.", cause);
    }

    /**
     * 저장본이 있는데도 다시 수집할지 판단한다.
     *
     * 갱신 요청이 아니면 다시 수집하지 않는다. 갱신 요청이라도 최소 간격 안이면
     * 저장본을 유지한다.
     */
    private boolean shouldFetch(
            StoredSummary stored,
            boolean refresh,
            Instant now
    ) {
        if (!refresh) {
            return false;
        }

        Instant refreshableFrom = stored.fetchedAt()
                .plus(characterSnapshotProperties.minRefreshInterval());

        return !now.isBefore(refreshableFrom);
    }

    private GetCharacterSummaryResult collectCharacterSummary(
            String characterName,
            Instant fetchedAt
    ) {
        String ocid = getValidatedOcid(characterName);

        CharacterBasic basic =
                loadCharacterBasicPort.loadCharacterBasic(ocid);

        CharacterStat stat =
                loadCharacterStatPort.loadCharacterStat(ocid);

        CharacterRanking characterRanking =
                getCharacterRanking(
                        ocid,
                        basic
                );

        CharacterDojang dojang =
                loadCharacterDojangPort.loadCharacterDojang(ocid);

        CharacterPopularity popularity =
                loadCharacterPopularityPort
                        .loadCharacterPopularity(ocid);

        CharacterHyperStat hyperStat =
                loadCharacterHyperStatPort
                        .loadCharacterHyperStat(ocid);

        CharacterAbility ability =
                loadCharacterAbilityPort
                        .loadCharacterAbility(ocid);

        CharacterUnion union =
                loadCharacterUnionPort.loadCharacterUnion(ocid);

        CharacterSymbol characterSymbol =
                loadCharacterSymbolPort.loadCharacterSymbol(ocid);

        CharacterSkills skills =
                loadCharacterSkillsPort
                        .loadCharacterSkills(ocid);

        CharacterHexa hexa =
                loadCharacterHexaPort
                        .loadCharacterHexa(ocid);

        CharacterEquipment equipment =
                loadCharacterEquipmentPort
                        .loadCharacterEquipment(ocid);

        CharacterSetEffect setEffect =
                loadCharacterSetEffectPort
                        .loadCharacterSetEffect(ocid);

        GetCharacterSummaryResult summary = GetCharacterSummaryResult.of(
                GetCharacterBasicResult.from(basic),
                GetCharacterStatResult.from(stat),
                GetCharacterRankingResult.of(
                        characterRanking,
                        dojang
                ),
                GetCharacterUnionResult.from(union),
                GetCharacterSymbolResult.from(characterSymbol),
                GetCharacterSkillsResult.from(
                        skills
                ),
                GetCharacterHexaResult.from(
                        hexa
                ),
                GetCharacterEquipmentResult.from(
                        equipment,
                        basic.characterClass(),
                        additionalOptionCalculator
                ),
                GetCharacterSetEffectResult.from(setEffect),
                GetCharacterPopularityResult.from(
                        popularity
                ),
                GetCharacterHyperStatResult.from(
                        hyperStat
                ),
                GetCharacterAbilityResult.from(
                        ability
                ),
                GetCharacterDojangResult.from(
                        dojang
                ),
                fetchedAt.toString()
        );

        // 저장은 다음 조회를 아끼기 위한 것이지 응답의 일부가 아니다. 저장이 실패해도
        // 이미 성공한 21회 조회를 버리지 않는다.
        try {
            characterSnapshotStoreService.store(
                    ocid,
                    summary,
                    fetchedAt
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "캐릭터 조회 결과를 저장하지 못했습니다. characterName={}",
                    characterName,
                    exception
            );
        }

        return summary;
    }

    private CharacterRanking getCharacterRanking(
            String ocid,
            CharacterBasic basic
    ) {
        try {
            return characterRankingQuery.getCharacterRanking(
                    ocid,
                    basic.characterName(),
                    basic.worldName()
            );
        } catch (CharacterRankingQueryException exception) {
            throw new CharacterException(
                    resolveCharacterErrorCode(
                            exception
                    )
            );
        }
    }

    private CharacterErrorCode resolveCharacterErrorCode(
            CharacterRankingQueryException exception
    ) {
        return switch (exception.getFailure()) {
            case NOT_FOUND, CLIENT_ERROR ->
                    CharacterErrorCode.NEXON_API_CLIENT_ERROR;
            case SERVER_ERROR ->
                    CharacterErrorCode.NEXON_API_SERVER_ERROR;
            case TIMEOUT ->
                    CharacterErrorCode.NEXON_API_TIMEOUT;
            case RESPONSE_INVALID ->
                    CharacterErrorCode.NEXON_API_RESPONSE_INVALID;
        };
    }

    private String getValidatedOcid(
            String characterName
    ) {
        validateCharacterName(characterName);

        return loadCharacterBasicPort.resolveOcid(characterName);
    }

    private void validateCharacterName(
            String characterName
    ) {
        if (!StringUtils.hasText(characterName)
                || !CHARACTER_NAME_PATTERN
                .matcher(characterName)
                .matches()) {
            throw new CharacterException(
                    CharacterErrorCode.INVALID_CHARACTER_NAME
            );
        }

        int characterNameLength =
                calculateCharacterNameLength(
                        characterName
                );

        if (characterNameLength
                < MIN_CHARACTER_NAME_LENGTH
                || characterNameLength
                > MAX_CHARACTER_NAME_LENGTH) {
            throw new CharacterException(
                    CharacterErrorCode.INVALID_CHARACTER_NAME
            );
        }
    }

    private int calculateCharacterNameLength(
            String characterName
    ) {
        return characterName.codePoints()
                .map(codePoint ->
                        isKoreanCharacter(codePoint)
                                ? KOREAN_CHARACTER_WEIGHT
                                : ENGLISH_NUMBER_CHARACTER_WEIGHT
                )
                .sum();
    }

    private boolean isKoreanCharacter(
            int codePoint
    ) {
        return codePoint >= '가'
                && codePoint <= '힣';
    }
}
