package com.maplemetric.global;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data
) {

    public static <T> ApiResponse<T> ok(
            SuccessCode successCode,
            T data
    ) {
        return new ApiResponse<>(
                true,
                successCode.getCode(),
                successCode.getMessage(),
                data
        );
    }
}