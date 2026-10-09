package com.occupify.identity.exception;

import com.occupify.identity.dto.base.ErrorResponse;
import com.occupify.identity.exception.auth.AuthErrorCode;
import com.occupify.identity.exception.system.SystemErrorCode;
import com.occupify.identity.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.occupify.identity.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, HttpServletRequest request) {
        String path = request != null ? request.getRequestURI() : "unknown";
        String correlationId = CorrelationIdFilter.resolveCorrelationId(request);

        log.warn("[Corr-{}] {} at [{}]: Code={} Message={}",
                correlationId, ex.getClass().getSimpleName(), path, ex.getCode(), ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                ex.getHttpStatus().value(),
                ex.getMessage(),
                ex.getCode()
        );
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String path = request != null ? request.getRequestURI() : "unknown";
        String correlationId = CorrelationIdFilter.resolveCorrelationId(request);

        FieldError fieldError = ex.getBindingResult().getFieldError();
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String path = request != null ? request.getRequestURI() : "unknown";
        String correlationId = CorrelationIdFilter.resolveCorrelationId(request);

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
