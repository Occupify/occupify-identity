package com.occupify.identity.dto.base;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GatewayErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String errorCode,
        String path,
        String correlationId
) {
    public record RequestContext(String path, String correlationId) {
    }

    public static GatewayErrorResponse of(int status, String message, RequestContext context) {
        return of(status, "Gateway Error", message, null, context);
    }

    public static GatewayErrorResponse of(int status, String error, String message, RequestContext context) {
        return of(status, error, message, null, context);
    }

    public static GatewayErrorResponse of(int status, String error, String message, String errorCode, RequestContext context) {
        return new GatewayErrorResponse(
                Instant.now().toString(),
                status,
                error,
                message,
                errorCode,
                context != null ? context.path() : "",
                context != null ? context.correlationId() : "unknown"
        );
    }

    public static GatewayErrorResponse of(int status, String error, String message, String path, String correlationId) {
        return of(status, error, message, null, new RequestContext(path, correlationId));
    }

    public static GatewayErrorResponse of(int status, String error, String message, String errorCode, String path, String correlationId) {
        return of(status, error, message, errorCode, new RequestContext(path, correlationId));
    }
}
