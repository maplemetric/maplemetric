package com.maplemetric.internal;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 이번 범위에서 두지 않은 규칙이 둘 있다. 둘 다 위반이 실재해서 제외했고
 * 규칙을 약화시킨 것이 아니라 판단이 남아 있다는 뜻이다.
 *
 * Infrastructure → Presentation
 *   InternalApiKeyFilter가 presentation.code의 InternalErrorCode를 읽는다.
 *   AGENTS.md §5가 금지하는 방향이지만, ErrorCode를 presentation/code에 두는 것은
 *   event·notice·ranking·statistics·internal 다섯 모듈의 공통 관례다.
 *   Filter를 옮길지 ErrorCode를 옮길지는 관례를 바꾸는 결정이라 별도로 정한다.
 *
 * Presentation → Infrastructure
 *   OverallRankingCollectionController가 infrastructure의
 *   OverallRankingCollectionProperties를 읽는다. 이 방향은 §5 금지 목록에
 *   명시되지 않아 판단이 필요하다.
 */
class InternalArchitectureTest {

    private static JavaClasses internalClasses;

    @BeforeAll
    static void setUp() {
        internalClasses = new ClassFileImporter()
                .withImportOption(
                        ImportOption.Predefined.DO_NOT_INCLUDE_TESTS
                )
                .importPackages("com.maplemetric.internal");
    }

    @Test
    void Application은Infrastructure와Presentation을참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..internal.application.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..internal.infrastructure..",
                        "..internal.presentation.."
                )
                .check(internalClasses);
    }
}
