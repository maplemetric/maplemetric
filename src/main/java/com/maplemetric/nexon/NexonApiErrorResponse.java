package com.maplemetric.nexon;

record NexonApiErrorResponse(
        NexonApiError error
) {

    record NexonApiError(
            String name,
            String message
    ) {
    }
}
