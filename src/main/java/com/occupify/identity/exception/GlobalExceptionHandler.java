package com.occupify.identity.exception;

import com.occupify.identity.dto.base.ErrorResponse;
import com.occupify.identity.exception.auth.AuthErrorCode;
import com.occupify.identity.exception.system.SystemErrorCode;
import com.occupify.identity.filter.CorrelationIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

@Slf4j
@RestControllerAdvice(basePackages = "com.occupify.identity.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        String correlationId = CorrelationIdFilter.resolveCorrelationId(exchange);

        log.warn("[Corr-{}] {} at [{}]: Code={} Message={}",
                correlationId, ex.getClass().getSimpleName(), path, ex.getCode(), ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                ex.getHttpStatus().value(),
                ex.getMessage(),
                ex.getCode()
        );
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(WebExchangeBindException ex, ServerWebExchange exchange) {
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

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                code.getCode()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        String correlationId = CorrelationIdFilter.resolveCorrelationId(exchange);
        String reason = ex.getReason() != null ? ex.getReason() : ex.getMessage();

        log.warn("[Corr-{}] ResponseStatusException at [{}]: status={} reason={}",
                correlationId, path, ex.getStatusCode(), reason);

        HttpStatus status = (ex.getStatusCode() instanceof HttpStatus hs) ? hs : HttpStatus.INTERNAL_SERVER_ERROR;

        ErrorResponse response = new ErrorResponse(
                status.value(),
                reason,
                null
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        String correlationId = CorrelationIdFilter.resolveCorrelationId(exchange);

        log.error("[Corr-{}] Unhandled exception at [{}]: {}",
                correlationId, path, ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                SystemErrorCode.INTERNAL_ERROR.getMessage(),
                SystemErrorCode.INTERNAL_ERROR.getCode()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private AuthErrorCode resolveValidationErrorCode(String fieldName) {
        if ("email".equalsIgnoreCase(fieldName)) {
            return AuthErrorCode.AUTH_000;
        }
        if ("password".equalsIgnoreCase(fieldName)) {
            return AuthErrorCode.AUTH_002;
        }
        return AuthErrorCode.AUTH_000;
    }
}
