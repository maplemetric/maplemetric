package com.maplemetric.notice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * notice 모듈의 고유 규칙만 둔다.
 *
 * 계층 방향은 ModuleLayerBaselineTest가 모든 모듈에 같은 기준으로 검사한다.
 */
class NoticeArchitectureTest {

    private static JavaClasses noticeClasses;

    @BeforeAll
    static void setUp() {
        noticeClasses = new ClassFileImporter()
                .withImportOption(
                        ImportOption.Predefined.DO_NOT_INCLUDE_TESTS
                )
                .importPackages("com.maplemetric.notice");
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
                        "..notice.application.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        "..common.nexon.."
                )
                .check(noticeClasses);
    }

    /**
     * Domain은 Spring을 알지 않는다.
     *
     * baseline은 계층 방향만 본다. 외부 기술 차단은 notice가 도메인 모델을
     * 순수하게 유지하기로 한 고유 결정이라 여기 남긴다.
     */
    @Test
    void Domain은Spring을참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..notice.domain.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..common.nexon..",
                        "org.springframework.."
                )
                .check(noticeClasses);
    }
}
