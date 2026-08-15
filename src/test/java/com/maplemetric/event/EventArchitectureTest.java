package com.maplemetric.event;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * event 모듈의 고유 규칙만 둔다.
 *
 * 계층 방향은 ModuleLayerBaselineTest가 모든 모듈에 같은 기준으로 검사한다.
 */
class EventArchitectureTest {

    private static JavaClasses eventClasses;

    @BeforeAll
    static void setUp() {
        eventClasses = new ClassFileImporter()
                .withImportOption(
                        ImportOption.Predefined.DO_NOT_INCLUDE_TESTS
                )
                .importPackages("com.maplemetric.event");
    }

    /**
     * 외부 수집은 infrastructure의 Adapter가 맡는다.
     *
     * Application이 Nexon 호출 규약을 직접 알면 수집처를 바꿀 때 함께 바뀐다.
     */
    @Test
    void Application은Nexon호출규약을참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..event.application.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        "..common.nexon.."
                )
                .check(eventClasses);
    }
}
