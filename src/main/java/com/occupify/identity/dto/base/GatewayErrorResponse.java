package com.occupify.identity.dto.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "statusCode", "message", "errorCode" })
public record GatewayErrorResponse(
        int statusCode,
        String message,
        String errorCode
) {
    public record RequestContext(String path, String correlationId) {
    }

    public static GatewayErrorResponse of(int statusCode, String message, String errorCode) {
        return new GatewayErrorResponse(statusCode, message, errorCode);
    }

    public static GatewayErrorResponse of(int statusCode, String message) {
        return new GatewayErrorResponse(statusCode, message, null);
    }

    public static GatewayErrorResponse of(int statusCode, String message, RequestContext context) {
        return new GatewayErrorResponse(statusCode, message, null);
    }

    public static GatewayErrorResponse of(int statusCode, String error, String message, RequestContext context) {
        return new GatewayErrorResponse(statusCode, message, null);
    }

    public static GatewayErrorResponse of(int statusCode, String error, String message, String errorCode, RequestContext context) {
        return new GatewayErrorResponse(statusCode, message, errorCode);
    }

    public static GatewayErrorResponse of(int statusCode, String error, String message, String path, String correlationId) {
        return new GatewayErrorResponse(statusCode, message, null);
    }

    public static GatewayErrorResponse of(int statusCode, String error, String message, String errorCode, String path, String correlationId) {
        return new GatewayErrorResponse(statusCode, message, errorCode);
    }
}
