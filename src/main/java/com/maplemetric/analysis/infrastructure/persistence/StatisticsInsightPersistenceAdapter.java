package com.maplemetric.analysis.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.analysis.application.port.out.SaveStatisticsInsightPort;
import com.maplemetric.analysis.domain.model.StatisticsInsightFacts;
import com.maplemetric.statistics.api.StatisticsSubjectType;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
class StatisticsInsightPersistenceAdapter implements SaveStatisticsInsightPort {

    private final StatisticsInsightRepository repository;
    private final ObjectMapper objectMapper;

    StatisticsInsightPersistenceAdapter(
            StatisticsInsightRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean exists(
            StatisticsSubjectType subjectType,
            String subjectSlug,
            String rangePreset,
            LocalDate asOf
    ) {
        return repository
                .existsBySubjectTypeAndSubjectSlugAndRangePresetAndAsOf(
                        subjectType,
                        subjectSlug,
                        rangePreset,
                        asOf
                );
    }

    /**
     * Fact 직렬화는 Adapter가 맡는다.
     *
     * JSON은 저장 방식이므로 Application이 알 필요가 없다. 직렬화에 실패하면
     * 저장하지 않는다. 근거 없는 문장만 남기면 나중에 추적할 수 없다.
     */
    @Override
    public void save(
            String rangePreset,
            LocalDate asOf,
            String headline,
            String summary,
            StatisticsInsightFacts facts,
            String model
    ) {
        String payload;

        try {
            payload = objectMapper.writeValueAsString(facts);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "통계 인사이트 근거를 저장할 수 없습니다.",
                    exception
            );
        }

        repository.save(StatisticsInsightEntity.create(
                facts.subject().type(),
                facts.subject().slug(),
                rangePreset,
                asOf,
                headline,
                summary,
                payload,
                model,
                Instant.now()
        ));
    }
}
