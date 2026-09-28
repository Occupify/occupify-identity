package com.occupify.identity.exception;

import java.time.Instant;

public record GatewayErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId
) {
    public static GatewayErrorResponse of(int status, String error, String message, String path, String correlationId) {
        return new GatewayErrorResponse(
                Instant.now().toString(),
                status,
                error,
                message,
                path,
                correlationId
        );
    }
}
