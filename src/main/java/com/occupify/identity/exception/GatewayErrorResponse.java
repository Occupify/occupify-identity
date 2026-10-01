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
    public record RequestContext(String path, String correlationId) {
    }

    public static GatewayErrorResponse of(int status, String message, RequestContext context) {
        return of(status, "Gateway Error", message, context);
    }

    public static GatewayErrorResponse of(int status, String error, String message, RequestContext context) {
        return new GatewayErrorResponse(
                Instant.now().toString(),
                status,
                error,
                message,
                context != null ? context.path() : "",
                context != null ? context.correlationId() : "unknown"
        );
    }

    public static GatewayErrorResponse of(int status, String error, String message, String path, String correlationId) {
        return of(status, error, message, new RequestContext(path, correlationId));
    }
}
