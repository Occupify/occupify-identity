package com.occupify.identity.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
    }

    @Test
    void shouldGenerateCorrelationIdWhenMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/projects").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        AtomicReference<String> downstreamHeader = new AtomicReference<>();
        WebFilterChain chain = ex -> {
            chainCalled.set(true);
            downstreamHeader.set(ex.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertTrue(chainCalled.get());
        String correlationIdAttr = exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        assertNotNull(correlationIdAttr, "Correlation ID attribute should be set");
        assertFalse(correlationIdAttr.isBlank(), "Correlation ID should not be blank");
        assertEquals(correlationIdAttr, downstreamHeader.get(), "Downstream request should receive correlation ID header");
        assertEquals(correlationIdAttr, exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER), "Response should include correlation ID header");
    }

    @Test
    void shouldPreserveExistingCorrelationId() {
        String existingId = "client-correlation-12345";
        MockServerHttpRequest request = MockServerHttpRequest.get("/projects")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        AtomicReference<String> downstreamHeader = new AtomicReference<>();
        WebFilterChain chain = ex -> {
            chainCalled.set(true);
            downstreamHeader.set(ex.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertTrue(chainCalled.get());
        assertEquals(existingId, exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE));
        assertEquals(existingId, downstreamHeader.get());
        assertEquals(existingId, exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }
}
