package com.maplemetric.analysis.infrastructure.persistence;

import com.maplemetric.statistics.api.StatisticsSubjectType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 생성된 통계 설명이다.
 *
 * {@code factsPayload}는 이 문장을 만들 때 쓴 Fact 전체다. 문장만 남기면 나중에
 * 어떤 수치에서 나왔는지 알 수 없고, Fact가 바뀌었는지도 비교할 수 없다.
 */
@Getter
@Entity
@Table(
        name = "p_statistics_insight",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_p_statistics_insight_subject_range_as_of",
                        columnNames = {
                                "subject_type",
                                "subject_slug",
                                "range_preset",
                                "as_of"
                        }
                )
        }
)
public class StatisticsInsightEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "statistics_insight_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 10)
    private StatisticsSubjectType subjectType;

    @Column(name = "subject_slug", nullable = false, length = 60)
    private String subjectSlug;

    @Column(name = "range_preset", nullable = false, length = 10)
    private String rangePreset;

    @Column(name = "as_of", nullable = false)
    private LocalDate asOf;

    @Column(name = "headline", nullable = false, length = 200)
    private String headline;

    @Column(name = "summary", nullable = false, columnDefinition = "text")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "facts_payload", nullable = false, columnDefinition = "jsonb")
    private String factsPayload;

    @Column(name = "model", length = 60)
    private String model;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StatisticsInsightEntity() {
    }

    private StatisticsInsightEntity(
            StatisticsSubjectType subjectType,
            String subjectSlug,
            String rangePreset,
            LocalDate asOf,
            String headline,
            String summary,
            String factsPayload,
            String model,
            Instant generatedAt
    ) {
        this.subjectType = subjectType;
        this.subjectSlug = subjectSlug;
        this.rangePreset = rangePreset;
        this.asOf = asOf;
        this.headline = headline;
        this.summary = summary;
        this.factsPayload = factsPayload;
        this.model = model;
        this.generatedAt = generatedAt;
    }

    public static StatisticsInsightEntity create(
            StatisticsSubjectType subjectType,
            String subjectSlug,
            String rangePreset,
            LocalDate asOf,
            String headline,
            String summary,
            String factsPayload,
            String model,
            Instant generatedAt
    ) {
        return new StatisticsInsightEntity(
                subjectType,
                subjectSlug,
                rangePreset,
                asOf,
                headline,
                summary,
                factsPayload,
                model,
                generatedAt
        );
    }

    @PrePersist
    private void prePersist() {
        createdAt = Instant.now();
    }
}
