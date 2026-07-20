package com.maplemetric.character.infrastructure.client.dto;

public record NexonApiErrorResponse(
        NexonApiError error
) {

    public record NexonApiError(
            String name,
            String message
    ) {
    }
}