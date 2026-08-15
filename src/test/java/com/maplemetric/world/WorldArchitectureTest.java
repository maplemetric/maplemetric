package com.maplemetric.world;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WorldArchitectureTest {

    private static JavaClasses worldClasses;

    @BeforeAll
    static void setUp() {
        worldClasses = new ClassFileImporter()
                .withImportOption(
                        ImportOption.Predefined.DO_NOT_INCLUDE_TESTS
                )
                .importPackages("com.maplemetric.world");
    }

    @Test
    void Application은Infrastructure를참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..world.application.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        "..world.infrastructure.."
                )
                .check(worldClasses);
    }
}
