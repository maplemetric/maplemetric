package com.maplemetric;

import static org.assertj.core.api.Assertions.assertThat;

import com.maplemetric.analysis.application.result.InsightResult;
import com.maplemetric.analysis.application.service.InsightGenerator;
import com.maplemetric.analysis.domain.model.InsightFacts;
import com.maplemetric.analysis.domain.model.InsightSentiment;
import com.maplemetric.ranking.CharacterRanking;
import com.maplemetric.ranking.CharacterRankingQuery;
import com.maplemetric.ranking.CharacterRankingQueryException;
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

    private static final String WORLD_ENTITY =
            "com.maplemetric.world.infrastructure.persistence."
                    + "WorldEntity";

    private static final String WORLD_REPOSITORY =
            "com.maplemetric.world.infrastructure.persistence."
                    + "WorldRepository";

    private static final String JOB_ENTITY =
            "com.maplemetric.ranking.infrastructure.persistence."
                    + "JobEntity";

    private static final String JOB_REPOSITORY =
            "com.maplemetric.ranking.infrastructure.persistence."
                    + "JobRepository";

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
                worldModule.getType(WORLD_ENTITY)
                        .map(type -> worldModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();

        assertThat(
                worldModule.getType(WORLD_REPOSITORY)
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
        ).containsExactlyInAnyOrder(
                CharacterRanking.class.getName(),
                CharacterRankingQuery.class.getName(),
                CharacterRankingQueryException.class.getName()
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
    }
}
