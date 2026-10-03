package com.occupify.identity.exception;

import com.occupify.identity.filter.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice(basePackages = "com.occupify.identity.controller")
public class AuthControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(AuthControllerAdvice.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<GatewayErrorResponse> handleAuthException(AuthException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        String correlationId = CorrelationIdFilter.resolveCorrelationId(exchange);

        log.warn("[Corr-{}] AuthException at [{}]: Code={} Message={}",
                correlationId, path, ex.getErrorCode().getCode(), ex.getMessage());

        GatewayErrorResponse response = GatewayErrorResponse.of(
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                ex.getMessage(),
                ex.getErrorCode().getCode(),
                path,
                correlationId
        );
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<GatewayErrorResponse> handleValidationException(WebExchangeBindException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        String correlationId = CorrelationIdFilter.resolveCorrelationId(exchange);

        FieldError fieldError = ex.getFieldError();
        String fieldName = fieldError != null ? fieldError.getField() : "request";
        String message = fieldError != null && fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "Validation failed for " + fieldName;

        AuthErrorCode code = resolveValidationErrorCode(fieldName);

        log.warn("[Corr-{}] Validation failure at [{}] on field [{}]: {}",
                correlationId, path, fieldName, message);

        GatewayErrorResponse response = GatewayErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                code.getCode(),
                path,
                correlationId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<GatewayErrorResponse> handleResponseStatusException(ResponseStatusException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        String correlationId = CorrelationIdFilter.resolveCorrelationId(exchange);
        String reason = ex.getReason() != null ? ex.getReason() : ex.getMessage();

        log.warn("[Corr-{}] ResponseStatusException at [{}]: Status={} Reason={}",
                correlationId, path, ex.getStatusCode().value(), reason);

        GatewayErrorResponse response = GatewayErrorResponse.of(
                ex.getStatusCode().value(),
                HttpStatus.resolve(ex.getStatusCode().value()) != null
                        ? HttpStatus.resolve(ex.getStatusCode().value()).getReasonPhrase()
                        : "Error",
                reason,
                path,
                correlationId
        );
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GatewayErrorResponse> handleGenericException(Exception ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        String correlationId = CorrelationIdFilter.resolveCorrelationId(exchange);

        log.error("[Corr-{}] Unhandled error at [{}]: {}", correlationId, path, ex.getMessage(), ex);

        GatewayErrorResponse response = GatewayErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected server error occurred",
                path,
                correlationId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private AuthErrorCode resolveValidationErrorCode(String fieldName) {
        return switch (fieldName) {
            case "email" -> AuthErrorCode.AUTH_000;
            case "password", "newPassword" -> AuthErrorCode.AUTH_002;
            case "otpCode" -> AuthErrorCode.AUTH_009;
            default -> AuthErrorCode.AUTH_000;
        };
    }
}
