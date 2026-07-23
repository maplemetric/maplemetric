package com.maplemetric.event;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EventArchitectureTest {

    private static JavaClasses eventClasses;

    @BeforeAll
    static void setUp() {
        eventClasses = new ClassFileImporter()
                .importPackages("com.maplemetric.event");
    }

    @Test
    void Application은Infrastructure와Presentation을참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..event.application.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..event.infrastructure..",
                        "..event.presentation..",
                        "..common.nexon.."
                )
                .check(eventClasses);
    }

    @Test
    void Infrastructure는Presentation을참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..event.infrastructure.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        "..event.presentation.."
                )
                .check(eventClasses);
    }

    @Test
    void Presentation은Infrastructure를참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..event.presentation.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        "..event.infrastructure.."
                )
                .check(eventClasses);
    }
}
