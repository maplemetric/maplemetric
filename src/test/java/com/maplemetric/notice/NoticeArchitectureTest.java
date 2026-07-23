package com.maplemetric.notice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NoticeArchitectureTest {

    private static JavaClasses noticeClasses;

    @BeforeAll
    static void setUp() {
        noticeClasses = new ClassFileImporter()
                .importPackages("com.maplemetric.notice");
    }

    @Test
    void Application은Infrastructure와Presentation을참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..notice.application.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..notice.infrastructure..",
                        "..notice.presentation..",
                        "..common.nexon.."
                )
                .check(noticeClasses);
    }

    @Test
    void Domain은상위계층과외부기술을참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..notice.domain.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..notice.application..",
                        "..notice.infrastructure..",
                        "..notice.presentation..",
                        "..common.nexon.."
                )
                .check(noticeClasses);
    }

    @Test
    void Infrastructure는Presentation을참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..notice.infrastructure.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        "..notice.presentation.."
                )
                .check(noticeClasses);
    }

    @Test
    void Presentation은Infrastructure를참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..notice.presentation.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        "..notice.infrastructure.."
                )
                .check(noticeClasses);
    }
}
