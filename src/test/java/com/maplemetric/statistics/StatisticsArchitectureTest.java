package com.maplemetric.statistics;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * statistics 모듈의 고유 규칙만 둔다.
 *
 * 계층 방향은 ModuleLayerBaselineTest가 모든 모듈에 같은 기준으로 검사한다.
 * 여기 남는 것은 statistics가 ranking·world를 어떻게 소비하는가다.
 */
class StatisticsArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(
                        ImportOption.Predefined.DO_NOT_INCLUDE_TESTS
                )
                .importPackages(
                        "com.maplemetric.statistics",
                        "com.maplemetric.ranking",
                        "com.maplemetric.world"
                );
    }

    /**
     * ranking은 api로만 소비한다.
     *
     * 이전에는 application과 infrastructure만 막아 domain과 presentation이
     * 열려 있었다. api 이외 전부를 막는다.
     */
    @Test
    void statistics는ranking의api만참조한다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..statistics.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..ranking.application..",
                        "..ranking.domain..",
                        "..ranking.infrastructure..",
                        "..ranking.presentation.."
                )
                .check(classes);
    }

    /**
     * world도 api로만 소비한다.
     */
    @Test
    void statistics는world의api만참조한다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..statistics.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..world.application..",
                        "..world.domain..",
                        "..world.infrastructure..",
                        "..world.presentation.."
                )
                .check(classes);
    }
}
