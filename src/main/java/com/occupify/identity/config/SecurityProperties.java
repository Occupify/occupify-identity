package com.occupify.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private List<String> whitelistedPaths = new ArrayList<>(List.of(
            "/auth/register",
            "/auth/send-otp",
            "/auth/resend-otp",
            "/auth/verify-otp",
            "/auth/login",
            "/auth/refresh-token",
            "/auth/signout",
            "/auth/forget-password",
            "/auth/reset-password",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/scalar/**",
            "/scalar",
            "/docs/**",
            "/docs"
    ));

    private List<String> adminRestrictedPaths = new ArrayList<>(List.of(
            "/admin/**",
            "/audit-logs/**",
            "/reports/**"
    ));

    public List<String> getWhitelistedPaths() {
        return whitelistedPaths;
    }

    public void setWhitelistedPaths(List<String> whitelistedPaths) {
        this.whitelistedPaths = whitelistedPaths;
    }

    public List<String> getAdminRestrictedPaths() {
        return adminRestrictedPaths;
    }

    public void setAdminRestrictedPaths(List<String> adminRestrictedPaths) {
        this.adminRestrictedPaths = adminRestrictedPaths;
    }
}
