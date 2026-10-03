package com.occupify.identity.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.identity.filter.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class GatewayErrorResponseWriter {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorResponseWriter.class);

    public static final String UNKNOWN_CORRELATION_ID = CorrelationIdFilter.UNKNOWN_CORRELATION_ID;
    private static final String DEFAULT_ERROR_PHRASE = "Gateway Error";

    private final ObjectMapper objectMapper;

    public GatewayErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> writeError(ServerWebExchange exchange, HttpStatusCode status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.empty();
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getURI().getPath();
        String correlationId = resolveCorrelationId(exchange);
        String reasonPhrase = (status instanceof HttpStatus hs) ? hs.getReasonPhrase() : DEFAULT_ERROR_PHRASE;
        String resolvedMessage = message != null ? message : reasonPhrase;

        GatewayErrorResponse.RequestContext context = new GatewayErrorResponse.RequestContext(path, correlationId);
        GatewayErrorResponse errorResponse = GatewayErrorResponse.of(
                status.value(),
                reasonPhrase,
                resolvedMessage,
                context
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorResponse);
        } catch (JsonProcessingException e) {
            log.error("[Corr-{}] Failed to serialize GatewayErrorResponse to JSON: {}", correlationId, e.getMessage(), e);
            bytes = buildFallbackJson(errorResponse);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    public String resolveCorrelationId(ServerWebExchange exchange) {
        return CorrelationIdFilter.resolveCorrelationId(exchange);
    }

    private byte[] buildFallbackJson(GatewayErrorResponse response) {
        String json = "{\"timestamp\":\"" + escapeJson(response.timestamp()) + "\","
                + "\"status\":" + response.status() + ","
                + "\"error\":\"" + escapeJson(response.error()) + "\","
                + "\"message\":\"" + escapeJson(response.message()) + "\","
                + (response.errorCode() != null ? "\"errorCode\":\"" + escapeJson(response.errorCode()) + "\"," : "")
                + "\"path\":\"" + escapeJson(response.path()) + "\","
                + "\"correlationId\":\"" + escapeJson(response.correlationId()) + "\"}";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
