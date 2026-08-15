package com.maplemetric.character;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CharacterArchitectureTest {

    private static JavaClasses characterClasses;

    @BeforeAll
    static void setUp() {
        characterClasses = new ClassFileImporter()
                .withImportOption(
                        ImportOption.Predefined.DO_NOT_INCLUDE_TESTS
                )
                .importPackages("com.maplemetric.character");
    }

    @Test
    void Application은Infrastructure와Presentation을참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..character.application.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..character.infrastructure..",
                        "..character.presentation.."
                )
                .check(characterClasses);
    }

    @Test
    void Domain은상위계층과외부기술을참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..character.domain.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..character.application..",
                        "..character.infrastructure..",
                        "..character.presentation.."
                )
                .check(characterClasses);
    }

    @Test
    void Infrastructure는Presentation을참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..character.infrastructure.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        "..character.presentation.."
                )
                .check(characterClasses);
    }

    @Test
    void Presentation은Infrastructure를참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..character.presentation.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        "..character.infrastructure.."
                )
                .check(characterClasses);
    }
}
