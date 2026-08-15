package com.maplemetric.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;
import java.util.Set;

/**
 * AGENTS.md 5장의 계층 방향을 규칙으로 만든다.
 *
 * 규칙을 모듈마다 손으로 옮겨 적으면 빠뜨려도 아무도 모른다. 여기서 한 번만
 * 정의하고 모듈 이름만 바꿔 끼운다.
 *
 * 계층이 아직 없는 모듈에도 그대로 적용한다. 대상이 없으면 검사할 것이 없을
 * 뿐이고, 나중에 그 계층이 생기면 규칙이 이미 걸려 있다.
 */
final class LayerDependencyRules {

    private static final String ROOT = "com.maplemetric.";

    private LayerDependencyRules() {
    }

    /**
     * 한 모듈의 한 계층이 같은 모듈의 다른 계층을 참조하지 못하게 한다.
     *
     * @param allowed 이 규칙에서만 예외로 허용할 (참조하는 쪽, 참조되는 쪽) 쌍.
     *                계층 전체를 여는 대신 알려진 참조 하나만 비켜 간다.
     */
    static ArchRule noDependency(
            String module,
            String fromLayer,
            List<String> toLayers,
            Set<AllowedDependency> allowed
    ) {
        // noClasses()는 조건을 뒤집는다. 커스텀 조건에서 위반을 직접 보고하려면
        // classes()를 쓰고 조건 자체를 "참조하지 않는다"로 서술해야 한다.
        return classes()
                .that()
                .resideInAPackage(layerPackage(module, fromLayer))
                .should(notDependOnLayers(module, toLayers, allowed))
                .as(
                        "%s 모듈의 %s는 %s를 참조하지 않는다"
                                .formatted(module, fromLayer, toLayers)
                )
                // 계층이 아직 없는 모듈에서는 대상 클래스가 하나도 없다.
                // 그것을 실패로 보면 미래 대비 규칙을 둘 수 없다.
                .allowEmptyShould(true);
    }

    private static ArchCondition<JavaClass> notDependOnLayers(
            String module,
            List<String> toLayers,
            Set<AllowedDependency> allowed
    ) {
        String description =
                "%s의 %s를 참조하지 않는다".formatted(module, toLayers);

        return new ArchCondition<>(description) {

            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
                    String origin = dependency.getOriginClass().getName();
                    String target = dependency.getTargetClass().getName();

                    if (!residesInAnyLayer(target, module, toLayers)) {
                        continue;
                    }

                    if (isAllowed(origin, target, allowed)) {
                        continue;
                    }

                    events.add(
                            SimpleConditionEvent.violated(
                                    dependency,
                                    dependency.getDescription()
                            )
                    );
                }
            }
        };
    }

    private static boolean residesInAnyLayer(
            String className,
            String module,
            List<String> layers
    ) {
        return layers.stream()
                .anyMatch(layer -> className.startsWith(
                        ROOT + module + "." + layer + "."
                ));
    }

    private static boolean isAllowed(
            String origin,
            String target,
            Set<AllowedDependency> allowed
    ) {
        return allowed.stream()
                .anyMatch(exception -> exception.matches(origin, target));
    }

    private static String layerPackage(String module, String layer) {
        return ".." + module + "." + layer + "..";
    }
}
