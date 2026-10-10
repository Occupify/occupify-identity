package com.occupify.identity.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_ATTRIBUTE = "correlationId";
    public static final String UNKNOWN_CORRELATION_ID = "unknown";

    public static String resolveCorrelationId(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN_CORRELATION_ID;
        }
        Object attr = request.getAttribute(CORRELATION_ID_ATTRIBUTE);
        if (attr != null && !attr.toString().isBlank()) {
            return attr.toString();
        }
        String headerId = request.getHeader(CORRELATION_ID_HEADER);
        if (headerId != null && !headerId.isBlank()) {
            return headerId;
        }
        return UNKNOWN_CORRELATION_ID;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        filterChain.doFilter(request, response);
    }
}
