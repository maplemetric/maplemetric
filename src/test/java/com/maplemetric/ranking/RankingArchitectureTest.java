package com.maplemetric.ranking;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * ranking 모듈의 고유 규칙만 둔다.
 *
 * 계층 방향은 ModuleLayerBaselineTest가 모든 모듈에 같은 기준으로 검사한다.
 */
class RankingArchitectureTest {

    private static JavaClasses rankingClasses;

    @BeforeAll
    static void setUp() {
        rankingClasses = new ClassFileImporter()
                .withImportOption(
                        ImportOption.Predefined.DO_NOT_INCLUDE_TESTS
                )
                .importPackages("com.maplemetric.ranking");
    }

    /**
     * 공개 api는 다른 모듈이 소비하는 계약이다.
     *
     * 여기에 Nexon 호출 규약이 새면 소비 모듈이 수집처 변경에 함께 깨진다.
     */
    @Test
    void 공개API는Nexon구현을참조하지않는다() {
        noClasses()
                .that()
                .resideInAPackage(
                        "..ranking.api.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        "..common.nexon.."
                )
                .check(rankingClasses);
    }
}
