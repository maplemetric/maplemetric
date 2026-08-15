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

    /**
     * catalog에는 아직 infrastructure 패키지가 없다.
     *
     * 그래도 금지 대상에 함께 적는다. 나중에 저장소나 외부 호출이 생길 때
     * 규칙이 이미 걸려 있어야 하고, 다른 모듈의 같은 규칙과 형태도 맞는다.
     */
    @Test
    void Application은Infrastructure와Presentation을참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..catalog.application.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..catalog.infrastructure..",
                        "..catalog.presentation.."
                )
                .check(catalogClasses);
    }
}
