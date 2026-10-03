package com.occupify.identity.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.identity.filter.CorrelationIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.junit.jupiter.api.Assertions.*;

class GatewayErrorResponseWriterTest {

    private ObjectMapper objectMapper;
    private GatewayErrorResponseWriter responseWriter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        responseWriter = new GatewayErrorResponseWriter(objectMapper);
    }

    @Test
    void shouldWriteErrorResponseWithCorrectStatusAndJsonBody() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test-endpoint").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE, "corr-abc-123");

        responseWriter.writeError(exchange, HttpStatus.UNAUTHORIZED, "Invalid Token").block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertEquals("application/json", exchange.getResponse().getHeaders().getFirst("Content-Type"));

        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);
        assertTrue(body.contains("\"status\":401"));
        assertTrue(body.contains("\"message\":\"Invalid Token\""));
        assertTrue(body.contains("\"path\":\"/test-endpoint\""));
        assertTrue(body.contains("\"correlationId\":\"corr-abc-123\""));
    }

    @Test
    void shouldFallbackWhenJsonSerializationFails() {
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public byte[] writeValueAsBytes(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("Serialization failed") {
                };
            }
        };

        GatewayErrorResponseWriter fallbackWriter = new GatewayErrorResponseWriter(failingMapper);

        MockServerHttpRequest request = MockServerHttpRequest.get("/fail-path").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        fallbackWriter.writeError(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Server broke").block();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);
        assertTrue(body.contains("\"status\":500"));
        assertTrue(body.contains("\"message\":\"Server broke\""));
    }

    @Test
    void shouldResolveCorrelationIdCorrectly() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
        MockServerWebExchange exchangeWithoutCorr = MockServerWebExchange.from(request);
        assertEquals(GatewayErrorResponseWriter.UNKNOWN_CORRELATION_ID,
                responseWriter.resolveCorrelationId(exchangeWithoutCorr));

        MockServerWebExchange exchangeWithCorr = MockServerWebExchange.from(request);
        exchangeWithCorr.getAttributes().put(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE, "my-corr-id");
        assertEquals("my-corr-id", responseWriter.resolveCorrelationId(exchangeWithCorr));
    }
}
