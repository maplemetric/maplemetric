package com.maplemetric;

import static org.assertj.core.api.Assertions.assertThat;

import com.maplemetric.analysis.application.result.InsightResult;
import com.maplemetric.analysis.application.service.InsightGenerator;
import com.maplemetric.analysis.domain.model.InsightEvidence;
import com.maplemetric.analysis.domain.model.InsightFacts;
import com.maplemetric.analysis.domain.model.InsightSentiment;
import com.maplemetric.analysis.domain.model.StatisticsInsightFact;
import com.maplemetric.analysis.domain.model.StatisticsInsightFactType;
import com.maplemetric.analysis.domain.model.StatisticsInsightFacts;
import com.maplemetric.analysis.domain.model.StatisticsInsightUnit;
import com.maplemetric.ranking.api.CanonicalJob;
import com.maplemetric.ranking.api.CharacterRanking;
import com.maplemetric.ranking.api.CharacterRankingQuery;
import com.maplemetric.ranking.api.CharacterRankingQueryException;
import com.maplemetric.ranking.api.CharacterRankingQueryFailure;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotOutcome;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotRequest;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotUseCase;
import com.maplemetric.ranking.api.ExpireOverallRankingSnapshotUseCase;
import com.maplemetric.ranking.api.JobCatalogQuery;
import com.maplemetric.ranking.api.OverallRankingRetentionPlan;
import com.maplemetric.ranking.api.OverallRankingRetentionRequest;
import com.maplemetric.ranking.api.OverallRankingCollectionAlreadyRunningException;
import com.maplemetric.ranking.api.OverallRankingCollectionException;
import com.maplemetric.ranking.api.OverallRankingCollectionFailure;
import com.maplemetric.ranking.api.OverallRankingCollectionStatus;
import com.maplemetric.ranking.api.OverallRankingComparisonQuery;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryException;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryFailure;
import com.maplemetric.ranking.api.OverallRankingStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsQuery;
import com.maplemetric.ranking.api.OverallRankingStatisticsQueryException;
import com.maplemetric.ranking.api.OverallRankingStatisticsQueryFailure;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsHistoryQuery;
import com.maplemetric.ranking.api.OverallRankingStatisticsHistoryQueryException;
import com.maplemetric.ranking.api.OverallRankingStatisticsHistoryQueryFailure;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsHistoryQuery;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsHistoryQueryException;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsHistoryQueryFailure;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQuery;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQueryException;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQueryFailure;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot;
import com.maplemetric.world.api.CanonicalWorld;
import com.maplemetric.statistics.api.StatisticsDataAvailability;
import com.maplemetric.statistics.api.StatisticsDetailFact;
import com.maplemetric.statistics.api.StatisticsFactQuery;
import com.maplemetric.statistics.api.StatisticsHistoryFact;
import com.maplemetric.statistics.api.StatisticsSubject;
import com.maplemetric.statistics.api.StatisticsSubjectType;
import com.maplemetric.statistics.api.StatisticsTrend;
import com.maplemetric.world.api.WorldAliasMatchingQuery;
import com.maplemetric.world.api.WorldCatalogQuery;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

class ModulithStructureTest {

    private static final String RANKING_DATE_RESOLVER =
            "com.maplemetric.ranking.application.service."
                    + "RankingDateResolver";

    private static final String TEMPLATE_INSIGHT_GENERATOR =
            "com.maplemetric.analysis.application.service."
                    + "TemplateInsightGenerator";

    private static final String OPENAI_INSIGHT_GENERATOR =
            "com.maplemetric.analysis.infrastructure.openai."
                    + "OpenAiInsightGenerator";

    private static final String STATISTICS_INSIGHT_FACTS_ASSEMBLER =
            "com.maplemetric.analysis.application.service."
                    + "StatisticsInsightFactsAssembler";

    private static final String STATISTICS_JOB_DETAIL_RESPONSE =
            "com.maplemetric.statistics.presentation.response."
                    + "GetJobStatisticsDetailResponse";

    private static final String STATISTICS_JOB_DETAIL_RESULT =
            "com.maplemetric.statistics.application.result."
                    + "GetJobStatisticsDetailResult";

    private static final String STATISTICS_FACT_QUERY_SERVICE =
            "com.maplemetric.statistics.application.service."
                    + "StatisticsFactQueryService";

    private static final String WORLD_ENTITY =
            "com.maplemetric.world.infrastructure.persistence."
                    + "WorldEntity";

    private static final String WORLD_REPOSITORY =
            "com.maplemetric.world.infrastructure.persistence."
                    + "WorldRepository";

    private static final String WORLD_ALIAS_ENTITY =
            "com.maplemetric.world.infrastructure.persistence."
                    + "WorldAliasEntity";

    private static final String WORLD_ALIAS_REPOSITORY =
            "com.maplemetric.world.infrastructure.persistence."
                    + "WorldAliasRepository";

    private static final String WORLD_ALIAS_TYPE =
            "com.maplemetric.world.infrastructure.persistence."
                    + "WorldAliasType";

    private static final String JOB_ENTITY =
            "com.maplemetric.ranking.infrastructure.persistence."
                    + "JobEntity";

    private static final String JOB_REPOSITORY =
            "com.maplemetric.ranking.infrastructure.persistence."
                    + "JobRepository";

    private static final String JOB_ALIAS_ENTITY =
            "com.maplemetric.ranking.infrastructure.persistence."
                    + "JobAliasEntity";

    private static final String JOB_ALIAS_REPOSITORY =
            "com.maplemetric.ranking.infrastructure.persistence."
                    + "JobAliasRepository";

    private static final String JOB_ALIAS_TYPE =
            "com.maplemetric.ranking.infrastructure.persistence."
                    + "JobAliasType";

    private static final String CHARACTER_SNAPSHOT_ENTITY =
            "com.maplemetric.character.infrastructure.persistence."
                    + "CharacterSnapshotEntity";

    private static final String CHARACTER_SNAPSHOT_REPOSITORY =
            "com.maplemetric.character.infrastructure.persistence."
                    + "CharacterSnapshotRepository";

    private static final String CHARACTER_EQUIPMENT_SNAPSHOT_ENTITY =
            "com.maplemetric.character.infrastructure.persistence."
                    + "CharacterEquipmentSnapshotEntity";

    private static final String CHARACTER_EQUIPMENT_SNAPSHOT_REPOSITORY =
            "com.maplemetric.character.infrastructure.persistence."
                    + "CharacterEquipmentSnapshotRepository";

    private final ApplicationModules modules =
            ApplicationModules.of(
                    MaplemetricServiceApplication.class
            );

    @Test
    void 모듈의존관계를검증한다() {
        modules.verify();
    }

    @Test
    void 공통Nexon인터페이스를공개한다() {
        assertThat(
                modules.getModuleByName("common")
                        .orElseThrow()
                        .getNamedInterfaces()
                        .getByName("nexon")
        ).isPresent();
    }

    @Test
    void 기존Global모듈은존재하지않는다() {
        assertThat(
                modules.getModuleByName("global")
        ).isEmpty();
    }

    @Test
    void 공지와이벤트를독립모듈로구성한다() {
        assertThat(
                modules.getModuleByName("notice")
        ).isPresent();

        assertThat(
                modules.getModuleByName("event")
        ).isPresent();
    }

    @Test
    void World모듈은영속성구현을외부에공개하지않는다() {
        ApplicationModule worldModule =
                modules.getModuleByName("world")
                        .orElseThrow();

        assertThat(
                worldModule.getNamedInterfaces()
                        .getByName("api")
                        .orElseThrow()
                        .asJavaClasses()
                        .map(type -> type.getName())
        ).containsExactlyInAnyOrder(
                WorldAliasMatchingQuery.class.getName(),
                WorldCatalogQuery.class.getName(),
                CanonicalWorld.class.getName(),
                CanonicalWorld.Status.class.getName()
        );

        assertThat(
                worldModule.getType(WORLD_ENTITY)
                        .map(type -> worldModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                worldModule.getType(WORLD_REPOSITORY)
                        .map(type -> worldModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                worldModule.getType(WORLD_ALIAS_ENTITY)
                        .map(type -> worldModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                worldModule.getType(WORLD_ALIAS_REPOSITORY)
                        .map(type -> worldModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                worldModule.getType(WORLD_ALIAS_TYPE)
                        .map(type -> worldModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();
    }

    @Test
    void Character모듈은Snapshot영속성구현을외부에공개하지않는다() {
        ApplicationModule characterModule =
                modules.getModuleByName("character")
                        .orElseThrow();

        assertThat(
                characterModule.getType(
                                CHARACTER_SNAPSHOT_ENTITY
                        )
                        .map(type -> characterModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                characterModule.getType(
                                CHARACTER_SNAPSHOT_REPOSITORY
                        )
                        .map(type -> characterModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                characterModule.getType(
                                CHARACTER_EQUIPMENT_SNAPSHOT_ENTITY
                        )
                        .map(type -> characterModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                characterModule.getType(
                                CHARACTER_EQUIPMENT_SNAPSHOT_REPOSITORY
                        )
                        .map(type -> characterModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();
    }

    @Test
    void Analysis모듈은외부계약이없고구현을내부에둔다() {
        ApplicationModule analysisModule =
                modules.getModuleByName("analysis")
                        .orElseThrow();

        assertThat(
                analysisModule.getNamedInterfaces()
                        .getUnnamedInterface()
                        .asJavaClasses()
        ).isEmpty();

        assertThat(
                analysisModule.getType(InsightFacts.class.getName())
                        .map(type -> analysisModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                analysisModule.getType(
                                InsightFacts.Evidence.class.getName()
                        )
                        .map(type -> analysisModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                analysisModule.getType(InsightGenerator.class.getName())
                        .map(type -> analysisModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                analysisModule.getType(InsightResult.class.getName())
                        .map(type -> analysisModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                analysisModule.getType(InsightSentiment.class.getName())
                        .map(type -> analysisModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                analysisModule.getType(
                                TEMPLATE_INSIGHT_GENERATOR
                        )
                        .map(type -> analysisModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                analysisModule.getType(
                                OPENAI_INSIGHT_GENERATOR
                        )
                        .map(type -> analysisModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        // 통계 Insight 모델도 계약이 아니다. 소비자는 아직 Analysis 안에만 있다.
        for (String statisticsInsightType : new String[]{
                StatisticsInsightFacts.class.getName(),
                StatisticsInsightFact.class.getName(),
                StatisticsInsightFactType.class.getName(),
                StatisticsInsightUnit.class.getName(),
                InsightEvidence.class.getName(),
                STATISTICS_INSIGHT_FACTS_ASSEMBLER
        }) {
            assertThat(
                    analysisModule.getType(statisticsInsightType)
                            .map(type -> analysisModule.isExposed(type))
                            .orElseThrow()
            ).isFalse();
        }
    }

    @Test
    void Ranking모듈은계약만공개하고기준일정책은내부에둔다() {
        ApplicationModule rankingModule =
                modules.getModuleByName("ranking")
                        .orElseThrow();

        assertThat(
                rankingModule.getNamedInterfaces()
                        .getUnnamedInterface()
                        .asJavaClasses()
                        .map(type -> type.getName())
        ).isEmpty();

        assertThat(
                rankingModule.getNamedInterfaces()
                        .getByName("api")
                        .orElseThrow()
                        .asJavaClasses()
                        .map(type -> type.getName())
        ).containsExactlyInAnyOrder(
                CharacterRanking.class.getName(),
                CharacterRankingQuery.class.getName(),
                CharacterRankingQueryException.class.getName(),
                CharacterRankingQueryFailure.class.getName(),
                OverallRankingStatisticsSnapshot.class.getName(),
                OverallRankingStatisticsSnapshot.JobCount.class.getName(),
                OverallRankingStatisticsQuery.class.getName(),
                OverallRankingStatisticsQueryException.class.getName(),
                OverallRankingStatisticsQueryFailure.class.getName(),
                OverallRankingWorldStatisticsSnapshot.class.getName(),
                OverallRankingWorldStatisticsSnapshot.WorldCount.class.getName(),
                OverallRankingWorldStatisticsQuery.class.getName(),
                OverallRankingWorldStatisticsQueryException.class.getName(),
                OverallRankingWorldStatisticsQueryFailure.class.getName(),
                OverallRankingWorldStatisticsHistoryQuery.class.getName(),
                OverallRankingWorldStatisticsHistoryQueryException.class.getName(),
                OverallRankingWorldStatisticsHistoryQueryFailure.class.getName(),
                OverallRankingStatisticsComparisonSnapshot.class.getName(),
                OverallRankingWorldStatisticsComparisonSnapshot.class.getName(),
                OverallRankingComparisonQuery.class.getName(),
                OverallRankingComparisonQueryException.class.getName(),
                OverallRankingComparisonQueryFailure.class.getName(),
                CollectOverallRankingSnapshotUseCase.class.getName(),
                CollectOverallRankingSnapshotRequest.class.getName(),
                CollectOverallRankingSnapshotOutcome.class.getName(),
                OverallRankingCollectionStatus.class.getName(),
                OverallRankingCollectionAlreadyRunningException.class.getName(),
                OverallRankingCollectionException.class.getName(),
                OverallRankingCollectionFailure.class.getName(),
                OverallRankingStatisticsHistoryQuery.class.getName(),
                OverallRankingStatisticsHistoryQueryException.class.getName(),
                OverallRankingStatisticsHistoryQueryFailure.class.getName(),
                ExpireOverallRankingSnapshotUseCase.class.getName(),
                OverallRankingRetentionRequest.class.getName(),
                OverallRankingRetentionPlan.class.getName(),
                JobCatalogQuery.class.getName(),
                CanonicalJob.class.getName()
        );

        assertThat(
                rankingModule.getType(
                                RANKING_DATE_RESOLVER
                        )
                        .map(type -> rankingModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                rankingModule.getType(JOB_ENTITY)
                        .map(type -> rankingModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                rankingModule.getType(JOB_REPOSITORY)
                        .map(type -> rankingModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                rankingModule.getType(JOB_ALIAS_ENTITY)
                        .map(type -> rankingModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                rankingModule.getType(JOB_ALIAS_REPOSITORY)
                        .map(type -> rankingModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                rankingModule.getType(JOB_ALIAS_TYPE)
                        .map(type -> rankingModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();
    }

    @Test
    void Internal모듈이존재한다() {
        assertThat(
                modules.getModuleByName("internal")
        ).isPresent();
    }

    @Test
    void Statistics모듈은Fact계약만공개하고응답과조회구현은내부에둔다() {
        ApplicationModule statisticsModule =
                modules.getModuleByName("statistics")
                        .orElseThrow();

        assertThat(
                statisticsModule.getNamedInterfaces()
                        .getByName("api")
                        .orElseThrow()
                        .asJavaClasses()
                        .map(type -> type.getName())
        ).containsExactlyInAnyOrder(
                StatisticsFactQuery.class.getName(),
                StatisticsSubject.class.getName(),
                StatisticsSubjectType.class.getName(),
                StatisticsDataAvailability.class.getName(),
                StatisticsTrend.class.getName(),
                StatisticsDetailFact.class.getName(),
                StatisticsDetailFact.LatestFact.class.getName(),
                StatisticsDetailFact.ComparisonFact.class.getName(),
                StatisticsDetailFact.SourceMetaFact.class.getName(),
                StatisticsHistoryFact.class.getName(),
                StatisticsHistoryFact.RangeFact.class.getName(),
                StatisticsHistoryFact.RangeComparisonFact.class.getName(),
                StatisticsHistoryFact.PointFact.class.getName()
        );

        // 응답 DTO와 조회 구현이 공개되면 소비자가 HTTP 형식 변경에 함께 깨진다.
        for (String internalType : new String[]{
                STATISTICS_JOB_DETAIL_RESPONSE,
                STATISTICS_JOB_DETAIL_RESULT,
                STATISTICS_FACT_QUERY_SERVICE
        }) {
            assertThat(
                    statisticsModule.getType(internalType)
                            .map(type -> statisticsModule.isExposed(type))
                            .orElseThrow()
            ).isFalse();
        }
    }
}
