package com.occupify.identity.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;

@Slf4j
@Component
@Order(GlobalErrorWebExceptionHandler.ORDER)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    // Higher precedence than default Spring WebFlux ErrorWebExceptionHandler (ORDER = -1)
    public static final int ORDER = -2;

    private static final String DEFAULT_DOWNSTREAM_OFFLINE_MSG = "Downstream microservice is unreachable or offline";
    private static final String DEFAULT_UNEXPECTED_ERROR_MSG = "Unexpected gateway error";

    private final GatewayErrorResponseWriter responseWriter;

    @org.springframework.beans.factory.annotation.Autowired
    public GlobalErrorWebExceptionHandler(GatewayErrorResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this(new GatewayErrorResponseWriter(objectMapper));
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        ErrorDetails details = resolveErrorDetails(ex);
        logError(exchange, details, ex);

        return responseWriter.writeError(exchange, details.status(), details.message());
    }

    private ErrorDetails resolveErrorDetails(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            String reason = rse.getReason() != null ? rse.getReason() : rse.getMessage();
            return new ErrorDetails(rse.getStatusCode(), reason);
        }
        if (ex instanceof ConnectException) {
            return new ErrorDetails(HttpStatus.SERVICE_UNAVAILABLE, DEFAULT_DOWNSTREAM_OFFLINE_MSG);
        }
        return new ErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR, DEFAULT_UNEXPECTED_ERROR_MSG);
    }

    private void logError(ServerWebExchange exchange, ErrorDetails details, Throwable ex) {
        String path = exchange.getRequest().getURI().getPath();
        String correlationId = responseWriter.resolveCorrelationId(exchange);

        if (details.status().is5xxServerError()) {
            log.error("[Corr-{}] Server error handling request for [{}]: Status {} - {}",
                    correlationId, path, details.status().value(), details.message(), ex);
        } else {
            log.warn("[Corr-{}] Client error handling request for [{}]: Status {} - {}",
                    correlationId, path, details.status().value(), details.message());
        }
    }

    private record ErrorDetails(HttpStatusCode status, String message) {
    }
}
