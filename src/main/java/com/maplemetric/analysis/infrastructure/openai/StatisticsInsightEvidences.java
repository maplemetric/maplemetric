package com.maplemetric.analysis.infrastructure.openai;

import com.maplemetric.analysis.domain.model.StatisticsInsightFacts;
import java.util.List;

/**
 * 근거 줄 형식을 한 곳에서 만든다.
 *
 * 이 형식은 요청과 검증 양쪽에서 쓰인다. 요청은 이 줄들을 Prompt에 담아 모델이
 * 그대로 돌려주게 하고, 검증은 응답이 같은 줄인지 비교한다.
 *
 * 두 곳이 형식을 따로 정의하면 어긋나는 순간 모든 응답이 거부되고 연동이 조용히
 * 템플릿으로만 동작한다. 컴파일도 통과하고 Stub 테스트도 통과해서 드러나지 않는다.
 * 실제로 그 상태가 한 번 나왔기 때문에 생성 지점을 하나로 모은다.
 */
final class StatisticsInsightEvidences {

    private StatisticsInsightEvidences() {
    }

    static List<String> lines(StatisticsInsightFacts facts) {
        return facts.facts()
                .stream()
                .flatMap(fact -> fact.evidence().stream())
                .map(evidence -> evidence.label() + ": " + evidence.value())
                .toList();
    }
}
