package com.maplemetric.world.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "p_world")
public class WorldEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "world_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "world_name", nullable = false, length = 30)
    private String worldName;

    @Column(name = "world_slug", nullable = false, length = 80)
    private String worldSlug;

    @Column(name = "external_world_code", length = 50)
    private String externalWorldCode;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected WorldEntity() {
    }

    private WorldEntity(
            String worldName,
            String worldSlug,
            String externalWorldCode,
            int displayOrder,
            Status status
    ) {
        this.worldName = requireText(
                worldName,
                "월드명은 비어 있을 수 없습니다."
        );
        this.worldSlug = requireText(
                worldSlug,
                "월드 Slug는 비어 있을 수 없습니다."
        );
        this.externalWorldCode = requireNullableText(
                externalWorldCode,
                "외부 월드 코드는 빈 문자열일 수 없습니다."
        );

        if (displayOrder < 0) {
            throw new IllegalArgumentException(
                    "월드 표시 순서는 0 이상이어야 합니다."
            );
        }

        if (status == null) {
            throw new IllegalArgumentException(
                    "월드 상태는 비어 있을 수 없습니다."
            );
        }

        this.displayOrder = displayOrder;
        this.status = status;
    }

    public static WorldEntity create(
            String worldName,
            String worldSlug,
            String externalWorldCode,
            int displayOrder,
            Status status
    ) {
        return new WorldEntity(
                worldName,
                worldSlug,
                externalWorldCode,
                displayOrder,
                status
        );
    }

    public void softDelete(Instant deletedAt) {
        if (deletedAt == null) {
            throw new IllegalArgumentException(
                    "월드 삭제 시각은 비어 있을 수 없습니다."
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

    public enum Status {
        ACTIVE,
        CLOSED
    }
}
