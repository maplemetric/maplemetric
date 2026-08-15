package com.maplemetric.analysis;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AnalysisArchitectureTest {

    private static JavaClasses analysisClasses;

    @BeforeAll
    static void setUp() {
        analysisClasses = new ClassFileImporter()
                .withImportOption(
                        ImportOption.Predefined.DO_NOT_INCLUDE_TESTS
                )
                .importPackages("com.maplemetric.analysis");
    }

    @Test
    void Application은Infrastructure를참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..analysis.application.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        "..analysis.infrastructure.."
                )
                .check(analysisClasses);
    }

    @Test
    void Domain은상위계층을참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..analysis.domain.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..analysis.application..",
                        "..analysis.infrastructure.."
                )
                .check(analysisClasses);
    }
}
