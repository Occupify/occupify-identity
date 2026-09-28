package com.occupify.identity.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;

@Component
public class GatewayRouteLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewayRouteLoggingFilter.class);

    public static final String DEFAULT_CORRELATION_ID = "N/A";
    public static final String UNKNOWN_CLIENT_IP = "unknown";
    public static final String UNMATCHED_ROUTE = "unmatched-route";
    public static final String UNRESOLVED_DESTINATION = "unresolved";
    public static final String UNKNOWN_STATUS = "UNKNOWN";
    public static final String QUERY_SEPARATOR = "?";

    private static final String LOG_ROUTING_REQUEST =
            "[Corr-{}] Routing request: {} {} -> Route [{}] Target [{}] | Client IP: {}";
    private static final String LOG_COMPLETED_REQUEST =
            "[Corr-{}] Completed request: {} {} -> Route [{}] with status [{}] (elapsed: {}ms)";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getURI().getRawPath();
        String query = request.getURI().getRawQuery();
        String fullPath = (query != null && !query.isBlank()) ? path + QUERY_SEPARATOR + query : path;

        String correlationId = exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        if (correlationId == null) {
            correlationId = DEFAULT_CORRELATION_ID;
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        String clientIp = (remoteAddress != null && remoteAddress.getAddress() != null)
                ? remoteAddress.getAddress().getHostAddress()
                : UNKNOWN_CLIENT_IP;

        // Retrieve matched route and destination target URI
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        URI targetUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);

        String routeId = (route != null) ? route.getId() : UNMATCHED_ROUTE;
        String destination = (targetUri != null) ? targetUri.toString()
                : (route != null ? route.getUri().toString() : UNRESOLVED_DESTINATION);

        // SLF4J Request & Target Route Logging
        log.info(LOG_ROUTING_REQUEST,
                correlationId, method, fullPath, routeId, destination, clientIp);

        final String finalCorrelationId = correlationId;
        return chain.filter(exchange).doFinally(signalType -> {
            long duration = System.currentTimeMillis() - startTime;
            HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
            log.info(LOG_COMPLETED_REQUEST,
                    finalCorrelationId, method, fullPath, routeId,
                    statusCode != null ? statusCode : UNKNOWN_STATUS,
                    duration);
        });
    }

    @Override
    public int getOrder() {
        // Runs immediately after RouteToRequestUrlFilter so GATEWAY_REQUEST_URL_ATTR is
        // available
        return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
    }
}
