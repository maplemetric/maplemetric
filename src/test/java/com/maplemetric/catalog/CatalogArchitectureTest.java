package com.maplemetric.catalog;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CatalogArchitectureTest {

    private static JavaClasses catalogClasses;

    @BeforeAll
    static void setUp() {
        catalogClasses = new ClassFileImporter()
                .withImportOption(
                        ImportOption.Predefined.DO_NOT_INCLUDE_TESTS
                )
                .importPackages("com.maplemetric.catalog");
    }

    @Test
    void Application은Presentation을참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..catalog.application.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        "..catalog.presentation.."
                )
                .check(catalogClasses);
    }
}
