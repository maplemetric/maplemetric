package com.maplemetric;

import static org.assertj.core.api.Assertions.assertThat;

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
    void Ranking모듈은계약만공개하고기준일정책은내부에둔다() {
        ApplicationModule rankingModule =
                modules.getModuleByName("ranking")
                        .orElseThrow();

        assertThat(rankingModule.isExposed(CharacterRanking.class))
                .isTrue();

        assertThat(rankingModule.isExposed(CharacterRankingQuery.class))
                .isTrue();

        assertThat(rankingModule.isExposed(
                CharacterRankingQueryException.class
        )).isTrue();

        assertThat(
                rankingModule.getType(
                                RANKING_DATE_RESOLVER
                        )
                        .map(type -> rankingModule.isExposed(type))
                        .orElseThrow()
        ).isFalse();
    }
}
