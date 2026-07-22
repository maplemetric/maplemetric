package com.maplemetric.common.nexon;

record NexonApiErrorResponse(
        NexonApiError error
) {

    record NexonApiError(
            String name,
            String message
    ) {
    }
}
