package com.maplemetric.ranking;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RankingArchitectureTest {

    private static JavaClasses rankingClasses;

    @BeforeAll
    static void setUp() {
        rankingClasses = new ClassFileImporter()
                .withImportOption(
                        ImportOption.Predefined.DO_NOT_INCLUDE_TESTS
                )
                .importPackages("com.maplemetric.ranking");
    }

    @Test
    void Application은Infrastructure와Presentation을참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..ranking.application.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..ranking.infrastructure..",
                        "..ranking.presentation..",
                        "..common.nexon.."
                )
                .check(rankingClasses);
    }
}
