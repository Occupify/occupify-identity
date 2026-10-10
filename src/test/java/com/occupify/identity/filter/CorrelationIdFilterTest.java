package com.occupify.identity.filter;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
    }

    @Test
    void shouldGenerateCorrelationIdWhenMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Object correlationIdAttr = request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        assertNotNull(correlationIdAttr, "Correlation ID attribute should be set");
        assertFalse(correlationIdAttr.toString().isBlank(), "Correlation ID should not be blank");
        assertEquals(correlationIdAttr.toString(), response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER),
                "Response should include correlation ID header");
    }

    @Test
    void shouldPreserveExistingCorrelationId() throws ServletException, IOException {
        String existingId = "client-correlation-12345";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/login");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(existingId, request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE));
        assertEquals(existingId, response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @Test
    void shouldResolveCorrelationIdFromAttributeOrHeader() {
        // Null request
        assertEquals(CorrelationIdFilter.UNKNOWN_CORRELATION_ID, CorrelationIdFilter.resolveCorrelationId(null));

        // Request with attribute
        MockHttpServletRequest req1 = new MockHttpServletRequest();
        req1.setAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE, "attr-corr-id");
        assertEquals("attr-corr-id", CorrelationIdFilter.resolveCorrelationId(req1));

        // Request with header only
        MockHttpServletRequest req2 = new MockHttpServletRequest();
        req2.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "header-corr-id");
        assertEquals("header-corr-id", CorrelationIdFilter.resolveCorrelationId(req2));

        // Request with neither
        MockHttpServletRequest req3 = new MockHttpServletRequest();
        assertEquals(CorrelationIdFilter.UNKNOWN_CORRELATION_ID, CorrelationIdFilter.resolveCorrelationId(req3));
    }
}
