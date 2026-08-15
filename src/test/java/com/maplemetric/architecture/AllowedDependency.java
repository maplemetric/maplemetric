package com.maplemetric.architecture;

/**
 * baseline 규칙에서 예외로 허용하는 참조 하나다.
 *
 * 계층 방향 전체를 여는 대신 알려진 참조 한 쌍만 비켜 가게 한다. 같은 계층의
 * 다른 참조가 새로 생기면 그대로 실패한다.
 *
 * 중첩 클래스에서 나온 참조도 바깥 클래스 기준으로 잡는다.
 */
record AllowedDependency(
        String originClass,
        String targetClass,
        String reason
) {

    AllowedDependency {
        if (originClass == null || originClass.isBlank()) {
            throw new IllegalArgumentException("참조하는 쪽 클래스는 필수입니다.");
        }

        if (targetClass == null || targetClass.isBlank()) {
            throw new IllegalArgumentException("참조되는 쪽 클래스는 필수입니다.");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "예외를 두는 이유는 필수입니다."
            );
        }
    }

    boolean matches(String origin, String target) {
        return outerClassOf(origin).equals(originClass)
                && outerClassOf(target).equals(targetClass);
    }

    private static String outerClassOf(String className) {
        int nested = className.indexOf('$');

        return nested < 0 ? className : className.substring(0, nested);
    }
}
