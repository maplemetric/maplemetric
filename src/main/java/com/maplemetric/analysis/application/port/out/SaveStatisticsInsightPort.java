package com.maplemetric.analysis.application.port.out;

import com.maplemetric.analysis.domain.model.StatisticsInsightFacts;
import com.maplemetric.statistics.api.StatisticsSubjectType;
import java.time.LocalDate;

public interface SaveStatisticsInsightPort {

    /**
     * 같은 대상·기간·기준일의 설명이 이미 있는지 본다.
     *
     * 재실행이 비용을 늘리지 않도록 생성 전에 확인한다.
     */
    boolean exists(
            StatisticsSubjectType subjectType,
            String subjectSlug,
            String rangePreset,
            LocalDate asOf
    );

    /**
     * 생성된 설명을 저장한다.
     *
     * {@code facts} 전체를 함께 남겨 어떤 수치에서 나온 문장인지 추적할 수 있게 한다.
     */
    void save(
            String rangePreset,
            LocalDate asOf,
            String headline,
            String summary,
            StatisticsInsightFacts facts,
            String model
    );
}
