package com.maplemetric.statistics;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

    @Test
    void Application은Infrastructure와Presentation을참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..statistics.application.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..statistics.infrastructure..",
                        "..statistics.presentation.."
                )
                .check(classes);
    }

    @Test
    void statistics는ranking의api를제외한내부패키지를참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..statistics.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..ranking.application..",
                        "..ranking.infrastructure.."
                )
                .check(classes);
    }

    @Test
    void statistics는world의api를제외한내부패키지를참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..statistics.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..world.application..",
                        "..world.infrastructure.."
                )
                .check(classes);
    }
}
