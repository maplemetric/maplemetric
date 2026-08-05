package com.maplemetric.ranking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.maplemetric.ranking.api.OverallRankingWorldStatisticsHistoryQueryException;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsHistoryQueryFailure;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.LatestCollection;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.WorldNameAggregateByCollection;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class OverallRankingWorldStatisticsHistoryQueryServiceTest {

    private static final UUID LATEST_COLLECTION_ID = UUID.randomUUID();
    private static final UUID PREVIOUS_COLLECTION_ID = UUID.randomUUID();

    private static final LocalDate FROM = LocalDate.of(2026, 7, 18);
    private static final LocalDate TO = LocalDate.of(2026, 7, 24);
    private static final LocalDate LATEST_SNAPSHOT_DATE = TO;
    private static final LocalDate PREVIOUS_SNAPSHOT_DATE =
            LocalDate.of(2026, 7, 21);

    private static final Instant COLLECTED_AT =
            Instant.parse("2026-07-24T01:00:00Z");

    @Mock
    private LoadOverallRankingStatisticsPort
            loadOverallRankingStatisticsPort;

    @Test
    void 기간내Collection이없으면빈목록을반환하고집계하지않는다() {
        OverallRankingWorldStatisticsHistoryQueryService service =
                createService();
        givenCollectionsBetween();

        List<OverallRankingWorldStatisticsSnapshot> history =
                service.getWorldStatisticsHistory(FROM, TO);

        assertThat(history).isEmpty();
        verify(loadOverallRankingStatisticsPort, never())
                .aggregateByWorldName(anyList());
    }

    @Test
    void 포인트를asOf오름차순으로정렬한다() {
        OverallRankingWorldStatisticsHistoryQueryService service =
                createService();
        givenCollectionsBetween(
                collection(LATEST_COLLECTION_ID, LATEST_SNAPSHOT_DATE, 1),
                collection(PREVIOUS_COLLECTION_ID, PREVIOUS_SNAPSHOT_DATE, 1)
        );

        given(loadOverallRankingStatisticsPort.aggregateByWorldName(
                List.of(LATEST_COLLECTION_ID, PREVIOUS_COLLECTION_ID)
        )).willReturn(List.of(
                worldNameAggregate(LATEST_COLLECTION_ID, "루나", 1L, 200),
                worldNameAggregate(PREVIOUS_COLLECTION_ID, "베라", 1L, 190)
        ));

        List<OverallRankingWorldStatisticsSnapshot> history =
                service.getWorldStatisticsHistory(FROM, TO);

        assertThat(history)
                .extracting(snapshot -> snapshot.asOf())
                .containsExactly(PREVIOUS_SNAPSHOT_DATE, LATEST_SNAPSHOT_DATE);
    }

    @Test
    void 각포인트가자신의collectionId집계만포함한다() {
        OverallRankingWorldStatisticsHistoryQueryService service =
                createService();
        givenCollectionsBetween(
                collection(LATEST_COLLECTION_ID, LATEST_SNAPSHOT_DATE, 1),
                collection(PREVIOUS_COLLECTION_ID, PREVIOUS_SNAPSHOT_DATE, 1)
        );

        given(loadOverallRankingStatisticsPort.aggregateByWorldName(
                List.of(LATEST_COLLECTION_ID, PREVIOUS_COLLECTION_ID)
        )).willReturn(List.of(
                worldNameAggregate(LATEST_COLLECTION_ID, "루나", 1L, 200),
                worldNameAggregate(PREVIOUS_COLLECTION_ID, "베라", 1L, 190)
        ));

        List<OverallRankingWorldStatisticsSnapshot> history =
                service.getWorldStatisticsHistory(FROM, TO);

        OverallRankingWorldStatisticsSnapshot latestPoint = history.stream()
                .filter(snapshot -> snapshot.asOf().equals(LATEST_SNAPSHOT_DATE))
                .findFirst()
                .orElseThrow();

        OverallRankingWorldStatisticsSnapshot previousPoint = history.stream()
                .filter(snapshot -> snapshot.asOf().equals(PREVIOUS_SNAPSHOT_DATE))
                .findFirst()
                .orElseThrow();

        assertThat(latestPoint.worldCounts())
                .extracting(worldCount -> worldCount.worldName())
                .containsExactly("루나");
        assertThat(previousPoint.worldCounts())
                .extracting(worldCount -> worldCount.worldName())
                .containsExactly("베라");
    }

    @Test
    void Collection1건이면Meta를유지한포인트1건을반환한다() {
        OverallRankingWorldStatisticsHistoryQueryService service =
                createService();
        givenCollectionsBetween(
                collection(LATEST_COLLECTION_ID, LATEST_SNAPSHOT_DATE, 1)
        );

        given(loadOverallRankingStatisticsPort.aggregateByWorldName(
                List.of(LATEST_COLLECTION_ID)
        )).willReturn(List.of(
                worldNameAggregate(LATEST_COLLECTION_ID, "루나", 1L, 200)
        ));

        List<OverallRankingWorldStatisticsSnapshot> history =
                service.getWorldStatisticsHistory(FROM, TO);

        assertThat(history).hasSize(1);
        OverallRankingWorldStatisticsSnapshot point = history.get(0);
        assertThat(point.asOf()).isEqualTo(LATEST_SNAPSHOT_DATE);
        assertThat(point.source()).isEqualTo("NEXON_OPEN_API");
        assertThat(point.collectedAt()).isEqualTo(COLLECTED_AT);
        assertThat(point.sampleSize()).isEqualTo(1);
        assertThat(point.pageCount()).isEqualTo(1);
        assertThat(point.requestedMaxPages()).isEqualTo(100);
        assertThat(point.truncated()).isFalse();
    }

    @Test
    void 어느포인트든집계합계가sampleSize와다르면DATA_INVALID예외를던진다() {
        OverallRankingWorldStatisticsHistoryQueryService service =
                createService();
        givenCollectionsBetween(
                collection(LATEST_COLLECTION_ID, LATEST_SNAPSHOT_DATE, 5)
        );

        given(loadOverallRankingStatisticsPort.aggregateByWorldName(
                List.of(LATEST_COLLECTION_ID)
        )).willReturn(List.of(
                worldNameAggregate(LATEST_COLLECTION_ID, "루나", 2L, 200)
        ));

        OverallRankingWorldStatisticsHistoryQueryException exception =
                catchThrowableOfType(
                        () -> service.getWorldStatisticsHistory(FROM, TO),
                        OverallRankingWorldStatisticsHistoryQueryException.class
                );

        assertThat(exception.getFailure())
                .isEqualTo(
                        OverallRankingWorldStatisticsHistoryQueryFailure
                                .DATA_INVALID
                );
    }

    @Test
    void sampleSize가0이고집계가비어있으면정상결과다() {
        OverallRankingWorldStatisticsHistoryQueryService service =
                createService();
        givenCollectionsBetween(
                collection(LATEST_COLLECTION_ID, LATEST_SNAPSHOT_DATE, 0)
        );

        given(loadOverallRankingStatisticsPort.aggregateByWorldName(
                List.of(LATEST_COLLECTION_ID)
        )).willReturn(List.of());

        List<OverallRankingWorldStatisticsSnapshot> history =
                service.getWorldStatisticsHistory(FROM, TO);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).worldCounts()).isEmpty();
    }

    @Test
    void 시작일이나종료일이null이면거부한다() {
        OverallRankingWorldStatisticsHistoryQueryService service =
                createService();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.getWorldStatisticsHistory(null, TO));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.getWorldStatisticsHistory(FROM, null));

        verifyNoInteractions(loadOverallRankingStatisticsPort);
    }

    @Test
    void 시작일이종료일보다늦으면거부한다() {
        OverallRankingWorldStatisticsHistoryQueryService service =
                createService();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.getWorldStatisticsHistory(
                        TO.plusDays(1),
                        TO
                ));

        verifyNoInteractions(loadOverallRankingStatisticsPort);
    }

    @Test
    void 포함달력일365일범위는허용한다() {
        OverallRankingWorldStatisticsHistoryQueryService service =
                createService();
        LocalDate from = TO.minusDays(364);

        given(loadOverallRankingStatisticsPort
                .loadAllConditionCollectionsBetween(from, TO))
                .willReturn(List.of());

        assertThat(service.getWorldStatisticsHistory(from, TO)).isEmpty();
    }

    @Test
    void 포함달력일366일범위는거부한다() {
        OverallRankingWorldStatisticsHistoryQueryService service =
                createService();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.getWorldStatisticsHistory(
                        TO.minusDays(365),
                        TO
                ));

        verifyNoInteractions(loadOverallRankingStatisticsPort);
    }

    @Test
    void 집계조회를포인트수와무관하게한번만호출한다() {
        OverallRankingWorldStatisticsHistoryQueryService service =
                createService();
        givenCollectionsBetween(
                collection(LATEST_COLLECTION_ID, LATEST_SNAPSHOT_DATE, 1),
                collection(PREVIOUS_COLLECTION_ID, PREVIOUS_SNAPSHOT_DATE, 1)
        );

        given(loadOverallRankingStatisticsPort.aggregateByWorldName(
                List.of(LATEST_COLLECTION_ID, PREVIOUS_COLLECTION_ID)
        )).willReturn(List.of(
                worldNameAggregate(LATEST_COLLECTION_ID, "루나", 1L, 200),
                worldNameAggregate(PREVIOUS_COLLECTION_ID, "베라", 1L, 190)
        ));

        service.getWorldStatisticsHistory(FROM, TO);

        verify(loadOverallRankingStatisticsPort, times(1))
                .aggregateByWorldName(List.of(
                        LATEST_COLLECTION_ID,
                        PREVIOUS_COLLECTION_ID
                ));
        verify(loadOverallRankingStatisticsPort, never())
                .aggregateByWorldName(LATEST_COLLECTION_ID);
    }

    @Test
    void 조회메서드는readOnlyTransaction경계다() throws Exception {
        Transactional transactional =
                OverallRankingWorldStatisticsHistoryQueryService.class
                        .getMethod(
                                "getWorldStatisticsHistory",
                                LocalDate.class,
                                LocalDate.class
                        )
                        .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }

    @Test
    void 전체기간조회는보존된모든Collection을돌려준다() {
        OverallRankingWorldStatisticsHistoryQueryService service =
                createService();

        given(loadOverallRankingStatisticsPort.loadAllConditionCollections())
                .willReturn(List.of(
                        collection(
                                LATEST_COLLECTION_ID,
                                LATEST_SNAPSHOT_DATE,
                                1
                        ),
                        collection(
                                PREVIOUS_COLLECTION_ID,
                                PREVIOUS_SNAPSHOT_DATE,
                                1
                        )
                ));
        given(loadOverallRankingStatisticsPort.aggregateByWorldName(
                List.of(LATEST_COLLECTION_ID, PREVIOUS_COLLECTION_ID)
        )).willReturn(List.of(
                worldNameAggregate(LATEST_COLLECTION_ID, "스카니아", 1L, 200),
                worldNameAggregate(PREVIOUS_COLLECTION_ID, "스카니아", 1L, 190)
        ));

        assertThat(service.getWorldStatisticsAllHistory())
                .extracting(snapshot -> snapshot.asOf())
                .containsExactly(PREVIOUS_SNAPSHOT_DATE, LATEST_SNAPSHOT_DATE);

        // 기간을 계산해 넘기지 않는다. 시작점은 DB에 남아 있는 최초 Collection이다.
        verify(loadOverallRankingStatisticsPort, never())
                .loadAllConditionCollectionsBetween(any(), any());

        // 기준일이 늘어도 Query 수는 2회로 고정이다.
        verify(loadOverallRankingStatisticsPort, times(1))
                .aggregateByWorldName(anyList());
        verify(loadOverallRankingStatisticsPort, never())
                .aggregateByWorldName(any(UUID.class));
    }

    @Test
    void 보존된Collection이없으면빈목록을반환하고집계하지않는다() {
        OverallRankingWorldStatisticsHistoryQueryService service =
                createService();

        given(loadOverallRankingStatisticsPort.loadAllConditionCollections())
                .willReturn(List.of());

        assertThat(service.getWorldStatisticsAllHistory()).isEmpty();

        verify(loadOverallRankingStatisticsPort, never())
                .aggregateByWorldName(anyList());
    }

    @Test
    void 랭킹통계조회Port에만의존하고외부API를호출하지않는다() {
        assertThat(
                OverallRankingWorldStatisticsHistoryQueryService.class
                        .getDeclaredConstructors()
        ).hasSize(1);

        assertThat(
                OverallRankingWorldStatisticsHistoryQueryService.class
                        .getDeclaredConstructors()[0]
                        .getParameterTypes()
        ).containsExactly(LoadOverallRankingStatisticsPort.class);
    }

    private OverallRankingWorldStatisticsHistoryQueryService createService() {
        return new OverallRankingWorldStatisticsHistoryQueryService(
                loadOverallRankingStatisticsPort
        );
    }

    private void givenCollectionsBetween(LatestCollection... collections) {
        given(loadOverallRankingStatisticsPort
                .loadAllConditionCollectionsBetween(FROM, TO))
                .willReturn(List.of(collections));
    }

    private LatestCollection collection(
            UUID collectionId,
            LocalDate snapshotDate,
            int sampleSize
    ) {
        return new LatestCollection(
                collectionId,
                snapshotDate,
                "NEXON_OPEN_API",
                COLLECTED_AT,
                sampleSize,
                1,
                100,
                false
        );
    }

    private WorldNameAggregateByCollection worldNameAggregate(
            UUID collectionId,
            String worldName,
            long count,
            int averageLevel
    ) {
        return new WorldNameAggregateByCollection(
                collectionId,
                worldName,
                count,
                BigDecimal.valueOf(averageLevel)
        );
    }
}
