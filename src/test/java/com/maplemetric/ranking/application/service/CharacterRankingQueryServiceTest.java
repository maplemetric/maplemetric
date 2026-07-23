package com.maplemetric.ranking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.common.nexon.NexonApiFailure;
import com.maplemetric.ranking.CharacterRanking;
import com.maplemetric.ranking.CharacterRankingQueryException;
import com.maplemetric.ranking.application.port.out.LoadCharacterRankingPort;
import com.maplemetric.ranking.application.port.out.LoadCharacterRankingPort.RankingEntry;
import com.maplemetric.ranking.domain.exception.RankingException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CharacterRankingQueryServiceTest {

    private static final String CHARACTER_NAME = "감점";
    private static final String OCID = "test-ocid";
    private static final String WORLD_NAME = "루나";

    @Mock
    private LoadCharacterRankingPort loadCharacterRankingPort;

    @Test
    void 랭킹기준일은KST오전9시29분이면전일이며네번의요청에동일하게전달한다() {
        CharacterRankingQueryService service = createService(
                "2026-07-21T00:29:00Z"
        );

        LocalDate rankingDate = LocalDate.of(2026, 7, 20);

        givenRankingResponses(
                rankingDate,
                "팬텀",
                null
        );

        CharacterRanking result = service.getCharacterRanking(
                OCID,
                CHARACTER_NAME,
                WORLD_NAME
        );

        assertThat(result)
                .isEqualTo(
                        new CharacterRanking(
                                58333,
                                10244,
                                1588,
                                321
                        )
                );

        verifyRankingCalls(
                rankingDate,
                "팬텀-전체 전직"
        );
    }

    @Test
    void 랭킹기준일은KST오전9시30분이면당일이다() {
        CharacterRankingQueryService service = createService(
                "2026-07-21T00:30:00Z"
        );

        LocalDate rankingDate = LocalDate.of(2026, 7, 21);

        givenRankingResponses(
                rankingDate,
                "팬텀",
                null
        );

        service.getCharacterRanking(
                OCID,
                CHARACTER_NAME,
                WORLD_NAME
        );

        verifyRankingCalls(
                rankingDate,
                "팬텀-전체 전직"
        );
    }

    @Test
    void 대상캐릭터의직업과전직으로랭킹필터를생성한다() {
        CharacterRankingQueryService service = createService(
                "2026-07-21T00:30:00Z"
        );

        LocalDate rankingDate = LocalDate.of(2026, 7, 21);

        List<RankingEntry> overallRanking =
                List.of(
                        createRanking(
                                "다른캐릭터",
                                1,
                                "팬텀",
                                null
                        ),
                        createRanking(
                                CHARACTER_NAME,
                                58333,
                                "모험가",
                                "마법사"
                        )
                );

        given(loadCharacterRankingPort.loadOverallRanking(
                OCID,
                rankingDate
        )).willReturn(overallRanking);

        given(loadCharacterRankingPort.loadWorldRanking(
                OCID,
                WORLD_NAME,
                rankingDate
        )).willReturn(createRankingResponse(
                CHARACTER_NAME,
                10244,
                "모험가",
                "마법사"
        ));

        given(loadCharacterRankingPort.loadClassRanking(
                OCID,
                "모험가-마법사",
                rankingDate
        )).willReturn(createRankingResponse(
                CHARACTER_NAME,
                1588,
                "모험가",
                "마법사"
        ));

        given(loadCharacterRankingPort.loadWorldClassRanking(
                OCID,
                WORLD_NAME,
                "모험가-마법사",
                rankingDate
        )).willReturn(createRankingResponse(
                CHARACTER_NAME,
                321,
                "모험가",
                "마법사"
        ));

        CharacterRanking result = service.getCharacterRanking(
                OCID,
                CHARACTER_NAME,
                WORLD_NAME
        );

        assertThat(result.overallRank()).isEqualTo(58333);

        verifyRankingCalls(
                rankingDate,
                "모험가-마법사"
        );
    }

    @Test
    void 직업랭킹필터를구할수없으면직업랭킹두종을조회하지않는다() {
        CharacterRankingQueryService service = createService(
                "2026-07-21T00:30:00Z"
        );

        LocalDate rankingDate = LocalDate.of(2026, 7, 21);
        List<RankingEntry> emptyRanking = List.of();

        given(loadCharacterRankingPort.loadOverallRanking(
                OCID,
                rankingDate
        )).willReturn(emptyRanking);

        given(loadCharacterRankingPort.loadWorldRanking(
                OCID,
                WORLD_NAME,
                rankingDate
        )).willReturn(emptyRanking);

        CharacterRanking result = service.getCharacterRanking(
                OCID,
                CHARACTER_NAME,
                WORLD_NAME
        );

        assertThat(result)
                .isEqualTo(
                        new CharacterRanking(
                                null,
                                null,
                                null,
                                null
                        )
                );

        verify(loadCharacterRankingPort, never())
                .loadClassRanking(
                        anyString(),
                        anyString(),
                        any(LocalDate.class)
                );

        verify(loadCharacterRankingPort, never())
                .loadWorldClassRanking(
                        anyString(),
                        anyString(),
                        anyString(),
                        any(LocalDate.class)
                );

        verify(loadCharacterRankingPort).loadOverallRanking(
                OCID,
                rankingDate
        );

        verify(loadCharacterRankingPort).loadWorldRanking(
                OCID,
                WORLD_NAME,
                rankingDate
        );

        verifyNoMoreInteractions(loadCharacterRankingPort);
    }

    @Test
    void null랭킹항목은제외하고대상캐릭터순위를찾는다() {
        CharacterRankingQueryService service = createService(
                "2026-07-21T00:30:00Z"
        );

        LocalDate rankingDate = LocalDate.of(2026, 7, 21);
        List<RankingEntry> nullItemRanking =
                Collections.singletonList(null);

        given(loadCharacterRankingPort.loadOverallRanking(
                OCID,
                rankingDate
        )).willReturn(nullItemRanking);

        given(loadCharacterRankingPort.loadWorldRanking(
                OCID,
                WORLD_NAME,
                rankingDate
        )).willReturn(nullItemRanking);

        CharacterRanking result = service.getCharacterRanking(
                OCID,
                CHARACTER_NAME,
                WORLD_NAME
        );

        assertThat(result.overallRank()).isNull();
        assertThat(result.worldRank()).isNull();
        assertThat(result.classRank()).isNull();
        assertThat(result.worldClassRank()).isNull();
    }

    @Test
    void 이백응답의빈목록은해당순위만null로반환한다() {
        CharacterRankingQueryService service = createService(
                "2026-07-21T00:30:00Z"
        );

        LocalDate rankingDate = LocalDate.of(2026, 7, 21);

        given(loadCharacterRankingPort.loadOverallRanking(
                OCID,
                rankingDate
        )).willReturn(createRankingResponse(
                CHARACTER_NAME,
                58333,
                "팬텀",
                null
        ));

        given(loadCharacterRankingPort.loadWorldRanking(
                OCID,
                WORLD_NAME,
                rankingDate
        )).willReturn(List.of());

        given(loadCharacterRankingPort.loadClassRanking(
                OCID,
                "팬텀-전체 전직",
                rankingDate
        )).willReturn(createRankingResponse(
                CHARACTER_NAME,
                1588,
                "팬텀",
                null
        ));

        given(loadCharacterRankingPort.loadWorldClassRanking(
                OCID,
                WORLD_NAME,
                "팬텀-전체 전직",
                rankingDate
        )).willReturn(createRankingResponse(
                CHARACTER_NAME,
                321,
                "팬텀",
                null
        ));

        CharacterRanking result = service.getCharacterRanking(
                OCID,
                CHARACTER_NAME,
                WORLD_NAME
        );

        assertThat(result)
                .isEqualTo(
                        new CharacterRanking(
                                58333,
                                null,
                                1588,
                                321
                        )
                );
    }

    @ParameterizedTest
    @MethodSource("rankingFailures")
    void 랭킹외부API오류를공개실패유형으로변환한다(
            NexonApiFailure failure
    ) {
        CharacterRankingQueryService service = createService(
                "2026-07-21T00:30:00Z"
        );

        LocalDate rankingDate = LocalDate.of(2026, 7, 21);

        given(loadCharacterRankingPort.loadOverallRanking(
                OCID,
                rankingDate
        )).willThrow(new RankingException(failure));

        CharacterRankingQueryException exception =
                catchThrowableOfType(
                        () -> service.getCharacterRanking(
                                OCID,
                                CHARACTER_NAME,
                                WORLD_NAME
                        ),
                        CharacterRankingQueryException.class
                );

        assertThat(exception.getFailure())
                .isEqualTo(failure);
    }

    private static Stream<NexonApiFailure> rankingFailures() {
        return Stream.of(
                NexonApiFailure.CLIENT_ERROR,
                NexonApiFailure.SERVER_ERROR,
                NexonApiFailure.TIMEOUT,
                NexonApiFailure.RESPONSE_INVALID
        );
    }

    private CharacterRankingQueryService createService(
            String instant
    ) {
        return new CharacterRankingQueryService(
                loadCharacterRankingPort,
                Clock.fixed(
                        Instant.parse(instant),
                        ZoneId.of("Asia/Seoul")
                )
        );
    }

    private void givenRankingResponses(
            LocalDate rankingDate,
            String className,
            String subClassName
    ) {
        given(loadCharacterRankingPort.loadOverallRanking(
                OCID,
                rankingDate
        )).willReturn(createRankingResponse(
                CHARACTER_NAME,
                58333,
                className,
                subClassName
        ));

        given(loadCharacterRankingPort.loadWorldRanking(
                OCID,
                WORLD_NAME,
                rankingDate
        )).willReturn(createRankingResponse(
                CHARACTER_NAME,
                10244,
                className,
                subClassName
        ));

        String classRankingFilter =
                subClassName == null
                        ? className + "-전체 전직"
                        : className + "-" + subClassName;

        given(loadCharacterRankingPort.loadClassRanking(
                OCID,
                classRankingFilter,
                rankingDate
        )).willReturn(createRankingResponse(
                CHARACTER_NAME,
                1588,
                className,
                subClassName
        ));

        given(loadCharacterRankingPort.loadWorldClassRanking(
                OCID,
                WORLD_NAME,
                classRankingFilter,
                rankingDate
        )).willReturn(createRankingResponse(
                CHARACTER_NAME,
                321,
                className,
                subClassName
        ));
    }

    private void verifyRankingCalls(
            LocalDate rankingDate,
            String classRankingFilter
    ) {
        verify(loadCharacterRankingPort).loadOverallRanking(
                OCID,
                rankingDate
        );

        verify(loadCharacterRankingPort).loadWorldRanking(
                OCID,
                WORLD_NAME,
                rankingDate
        );

        verify(loadCharacterRankingPort).loadClassRanking(
                OCID,
                classRankingFilter,
                rankingDate
        );

        verify(loadCharacterRankingPort).loadWorldClassRanking(
                OCID,
                WORLD_NAME,
                classRankingFilter,
                rankingDate
        );

        verifyNoMoreInteractions(loadCharacterRankingPort);
    }

    private List<RankingEntry> createRankingResponse(
            String characterName,
            int ranking,
            String className,
            String subClassName
    ) {
        return List.of(
                createRanking(
                        characterName,
                        ranking,
                        className,
                        subClassName
                )
        );
    }

    private RankingEntry createRanking(
            String characterName,
            int ranking,
            String className,
            String subClassName
    ) {
        return new RankingEntry(
                ranking,
                characterName,
                className,
                subClassName
        );
    }
}
