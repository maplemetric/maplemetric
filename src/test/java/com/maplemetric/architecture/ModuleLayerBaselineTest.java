package com.maplemetric.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.maplemetric.MaplemetricServiceApplication;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

/**
 * 모든 모듈에 같은 계층 방향 기준을 적용한다.
 *
 * 검사 대상 모듈을 손으로 적지 않고 ApplicationModules에서 가져온다. 모듈이
 * 새로 생기면 목록을 고치지 않아도 자동으로 검사에 들어온다. 이 Test가 존재하는
 * 이유가 "규칙을 한곳에 모으는 것"이 아니라 "검사가 빠지지 않는 것"이기 때문이다.
 *
 * 모듈 고유 규칙은 여기 두지 않는다. 각 모듈의 ArchitectureTest에 남긴다.
 */
class ModuleLayerBaselineTest {

    private static final String INTERNAL_API_KEY_FILTER =
            "com.maplemetric.internal.infrastructure.filter."
                    + "InternalApiKeyFilter";

    private static final String INTERNAL_ERROR_CODE =
            "com.maplemetric.internal.presentation.code."
                    + "InternalErrorCode";

    /**
     * Infrastructure가 Presentation을 참조하는 알려진 예외다.
     *
     * ErrorCode를 presentation/code에 두는 것은 다섯 모듈의 공통 관례이고,
     * Filter를 옮길지 ErrorCode를 옮길지는 그 관례를 바꾸는 결정이다.
     * 방향 전체를 열지 않고 이 참조 하나만 비켜 간다.
     */
    private static final Set<AllowedDependency> INFRASTRUCTURE_EXCEPTIONS =
            Set.of(
                    new AllowedDependency(
                            INTERNAL_API_KEY_FILTER,
                            INTERNAL_ERROR_CODE,
                            "인증 실패 응답을 공통 ApiResponse 계약으로 쓰기 위해"
                                    + " ErrorCode를 읽는다."
                    )
            );

    private static JavaClasses productionClasses;

    @BeforeAll
    static void setUp() {
        productionClasses = new ClassFileImporter()
                .withImportOption(
                        ImportOption.Predefined.DO_NOT_INCLUDE_TESTS
                )
                .importPackages("com.maplemetric");
    }

    private static Stream<String> modules() {
        return ApplicationModules
                .of(MaplemetricServiceApplication.class)
                .stream()
                .map(ApplicationModule::getName);
    }

    @Test
    void 검사대상모듈을ApplicationModules에서가져온다() {
        List<String> moduleNames = modules().toList();

        assertThat(moduleNames)
                .isNotEmpty()
                .contains(
                        "analysis",
                        "catalog",
                        "character",
                        "event",
                        "internal",
                        "notice",
                        "ranking",
                        "statistics",
                        "world"
                );
    }

    @ParameterizedTest(name = "{0}: Application은 Infrastructure와 Presentation을 참조하지 않는다")
    @MethodSource("modules")
    void Application은Infrastructure와Presentation을참조하지않는다(String module) {
        LayerDependencyRules.noDependency(
                module,
                "application",
                List.of("infrastructure", "presentation"),
                Set.of()
        ).check(productionClasses);
    }

    /**
     * Presentation은 Domain을 참조할 수 있다.
     *
     * Domain Exception을 HTTP Error 계약으로 옮기는 참조를 허용하기로 했다.
     * 그래서 금지 대상은 Infrastructure뿐이다.
     */
    @ParameterizedTest(name = "{0}: Presentation은 Infrastructure를 참조하지 않는다")
    @MethodSource("modules")
    void Presentation은Infrastructure를참조하지않는다(String module) {
        LayerDependencyRules.noDependency(
                module,
                "presentation",
                List.of("infrastructure"),
                Set.of()
        ).check(productionClasses);
    }

    @ParameterizedTest(name = "{0}: Infrastructure는 Presentation을 참조하지 않는다")
    @MethodSource("modules")
    void Infrastructure는Presentation을참조하지않는다(String module) {
        LayerDependencyRules.noDependency(
                module,
                "infrastructure",
                List.of("presentation"),
                INFRASTRUCTURE_EXCEPTIONS
        ).check(productionClasses);
    }

    @ParameterizedTest(name = "{0}: Domain은 상위 계층을 참조하지 않는다")
    @MethodSource("modules")
    void Domain은상위계층을참조하지않는다(String module) {
        LayerDependencyRules.noDependency(
                module,
                "domain",
                List.of("application", "infrastructure", "presentation"),
                Set.of()
        ).check(productionClasses);
    }
}
