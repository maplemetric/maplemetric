package com.maplemetric.ranking.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "p_job")
public class JobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "job_name", nullable = false, length = 50)
    private String jobName;

    @Column(name = "external_job_code", length = 50)
    private String externalJobCode;

    @Column(name = "job_group", nullable = false, length = 30)
    private String jobGroup;

    @Column(name = "job_branch", length = 30)
    private String jobBranch;

    @Column(name = "is_available", nullable = false)
    private boolean available;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected JobEntity() {
    }

    private JobEntity(
            String jobName,
            String externalJobCode,
            String jobGroup,
            String jobBranch,
            boolean available,
            int displayOrder
    ) {
        this.jobName = requireText(
                jobName,
                "직업명은 비어 있을 수 없습니다."
        );
        this.externalJobCode = requireNullableText(
                externalJobCode,
                "외부 직업 코드는 빈 문자열일 수 없습니다."
        );
        this.jobGroup = requireText(
                jobGroup,
                "직업군은 비어 있을 수 없습니다."
        );
        this.jobBranch = requireNullableText(
                jobBranch,
                "직업 분류는 빈 문자열일 수 없습니다."
        );

        if (displayOrder < 0) {
            throw new IllegalArgumentException(
                    "직업 표시 순서는 0 이상이어야 합니다."
            );
        }

        this.available = available;
        this.displayOrder = displayOrder;
    }

    public static JobEntity create(
            String jobName,
            String externalJobCode,
            String jobGroup,
            String jobBranch,
            boolean available,
            int displayOrder
    ) {
        return new JobEntity(
                jobName,
                externalJobCode,
                jobGroup,
                jobBranch,
                available,
                displayOrder
        );
    }

    public void softDelete(Instant deletedAt) {
        if (deletedAt == null) {
            throw new IllegalArgumentException(
                    "직업 삭제 시각은 비어 있을 수 없습니다."
            );
        }

        this.deletedAt = deletedAt;
    }

    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }

    private static String requireText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static String requireNullableText(
            String value,
            String message
    ) {
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }
}
