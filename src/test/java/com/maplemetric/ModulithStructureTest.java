package com.maplemetric;

import static org.assertj.core.api.Assertions.assertThat;

import com.maplemetric.analysis.InsightFacts;
import com.maplemetric.analysis.InsightGenerator;
import com.maplemetric.analysis.InsightResult;
import com.maplemetric.analysis.InsightSentiment;
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
    void Analysis모듈은계약만공개하고구현은내부에둔다() {
        ApplicationModule analysisModule =
                modules.getModuleByName("analysis")
                        .orElseThrow();

        assertThat(
                analysisModule.getNamedInterfaces()
                        .getUnnamedInterface()
                        .asJavaClasses()
                        .map(type -> type.getName())
        ).containsExactlyInAnyOrder(
                InsightFacts.class.getName(),
                InsightFacts.Evidence.class.getName(),
                InsightGenerator.class.getName(),
                InsightResult.class.getName(),
                InsightSentiment.class.getName()
        );

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
    }
}
