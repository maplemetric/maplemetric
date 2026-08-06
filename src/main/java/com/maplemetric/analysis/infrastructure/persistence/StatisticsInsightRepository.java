package com.maplemetric.analysis.infrastructure.persistence;

import com.maplemetric.statistics.api.StatisticsSubjectType;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface StatisticsInsightRepository
        extends JpaRepository<StatisticsInsightEntity, UUID> {

    boolean existsBySubjectTypeAndSubjectSlugAndRangePresetAndAsOf(
            StatisticsSubjectType subjectType,
            String subjectSlug,
            String rangePreset,
            LocalDate asOf
    );
}
