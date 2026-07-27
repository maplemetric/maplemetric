package com.maplemetric.world.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "p_world_alias")
public class WorldAliasEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "world_alias_id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "world_id", nullable = false, updatable = false)
    private WorldEntity world;

    @Column(
            name = "alias_name",
            nullable = false,
            updatable = false,
            length = 30
    )
    private String aliasName;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "alias_type",
            nullable = false,
            updatable = false,
            length = 20
    )
    private WorldAliasType aliasType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected WorldAliasEntity() {
    }

    private WorldAliasEntity(
            WorldEntity world,
            String aliasName,
            WorldAliasType aliasType
    ) {
        this.world = Objects.requireNonNull(
                world,
                "월드는 비어 있을 수 없습니다."
        );
        this.aliasName = requireText(
                aliasName,
                "Alias 이름은 비어 있을 수 없습니다."
        );
        this.aliasType = Objects.requireNonNull(
                aliasType,
                "Alias 유형은 비어 있을 수 없습니다."
        );
    }

    public static WorldAliasEntity create(
            WorldEntity world,
            String aliasName,
            WorldAliasType aliasType
    ) {
        return new WorldAliasEntity(world, aliasName, aliasType);
    }

    public void softDelete(Instant deletedAt) {
        this.deletedAt = Objects.requireNonNull(
                deletedAt,
                "Alias 삭제 시각은 비어 있을 수 없습니다."
        );
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
}
