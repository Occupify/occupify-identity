package com.occupify.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class OccupifyIdentityGatewayApplicationTests {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void contextLoads() {
        // Verifies Spring Cloud Gateway reactive context, filters, and properties load successfully
    }

    @Test
    void shouldDefineConfiguredRoutes() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertNotNull(routes);
        List<String> routeIds = routes.stream().map(Route::getId).toList();
        assertTrue(routeIds.contains("core-service"), "Routes should contain core-service");
        assertTrue(routeIds.contains("payment-service"), "Routes should contain payment-service");
        assertTrue(routeIds.contains("notification-service"), "Routes should contain notification-service");
        assertTrue(routeIds.contains("administration-service"), "Routes should contain administration-service");
        assertTrue(routeIds.contains("auth-service"), "Routes should contain auth-service");
    }
}

