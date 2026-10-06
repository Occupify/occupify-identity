package com.occupify.identity.exception.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.identity.exception.BaseException;
import com.occupify.identity.filter.CorrelationIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalErrorWebExceptionHandlerTest {

    private ObjectMapper objectMapper;
    private GlobalErrorWebExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        exceptionHandler = new GlobalErrorWebExceptionHandler(objectMapper);
    }

    @Test
    void shouldWriteErrorResponseWithCorrectStatusAndJsonBody() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test-endpoint").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE, "corr-abc-123");

        exceptionHandler.writeError(exchange, HttpStatus.UNAUTHORIZED, "Invalid Token").block();

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
    void shouldReturnEmptyWhenResponseIsAlreadyCommitted() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test-endpoint").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setComplete().block();

        exceptionHandler.writeError(exchange, HttpStatus.BAD_REQUEST, "Error").block();
        assertTrue(exchange.getResponse().isCommitted());
    }

    @Test
    void shouldUseDefaultReasonPhraseWhenMessageIsNull() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test-endpoint").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        exceptionHandler.writeError(exchange, HttpStatus.FORBIDDEN, null).block();

        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);
        assertTrue(body.contains("\"status\":403"));
        assertTrue(body.contains("\"error\":\"Forbidden\""));
        assertTrue(body.contains("\"message\":\"Forbidden\""));
    }

    @Test
    void shouldFallbackWhenObjectMapperSerializationFails() throws Exception {
        ObjectMapper failingMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        org.mockito.Mockito.when(failingMapper.writeValueAsBytes(org.mockito.Mockito.any()))
                .thenThrow(new JsonProcessingException("Serialization failed") {});

        GlobalErrorWebExceptionHandler fallbackHandler = new GlobalErrorWebExceptionHandler(failingMapper);

        MockServerHttpRequest request = MockServerHttpRequest.get("/test-endpoint").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        fallbackHandler.writeError(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Fallback Message").block();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);
        assertTrue(body.contains("\"status\":500"));
        assertTrue(body.contains("\"message\":\"Fallback Message\""));
    }

    @Test
    void shouldHandleResponseStatusException() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        exceptionHandler.handle(exchange, ex).block();

        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);
        assertTrue(body.contains("\"status\":404"));
        assertTrue(body.contains("Resource not found"));
    }

    @Test
    void shouldHandleConnectException() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/downstream").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ConnectException ex = new ConnectException("Connection refused");
        exceptionHandler.handle(exchange, ex).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);
        assertTrue(body.contains("\"status\":503"));
        assertTrue(body.contains("Downstream microservice is unreachable or offline"));
    }

    @Test
    void shouldHandleBaseException() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        BaseException ex = new BaseException("TEST_CODE", "Custom base exception", HttpStatus.BAD_REQUEST) {};
        exceptionHandler.handle(exchange, ex).block();

        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);
        assertTrue(body.contains("\"status\":400"));
        assertTrue(body.contains("Custom base exception"));
    }

    @Test
    void shouldHandleGenericException() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/error").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        RuntimeException ex = new RuntimeException("Something went wrong");
        exceptionHandler.handle(exchange, ex).block();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);
        assertTrue(body.contains("\"status\":500"));
        assertTrue(body.contains("Unexpected gateway error"));
    }

    @Test
    void shouldReturnMonoErrorWhenResponseIsCommittedInHandle() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/committed").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setComplete().block();

        RuntimeException ex = new RuntimeException("Boom");
        assertThrows(RuntimeException.class, () -> exceptionHandler.handle(exchange, ex).block());
    }

    @Test
    void shouldResolveCorrelationIdCorrectly() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
        MockServerWebExchange exchangeWithoutCorr = MockServerWebExchange.from(request);
        assertEquals(GlobalErrorWebExceptionHandler.UNKNOWN_CORRELATION_ID,
                exceptionHandler.resolveCorrelationId(exchangeWithoutCorr));

        MockServerWebExchange exchangeWithCorr = MockServerWebExchange.from(request);
        exchangeWithCorr.getAttributes().put(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE, "my-corr-id");
        assertEquals("my-corr-id", exceptionHandler.resolveCorrelationId(exchangeWithCorr));
    }
}
