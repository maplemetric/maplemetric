package com.maplemetric.world.api;

public record CanonicalWorld(
        String worldSlug,
        String worldName,
        Status status,
        int displayOrder
) {

    public enum Status {
        ACTIVE,
        CLOSED
    }
}
