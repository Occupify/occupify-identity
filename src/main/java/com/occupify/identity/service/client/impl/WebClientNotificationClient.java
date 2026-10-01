package com.occupify.identity.service.client.impl;

import com.occupify.identity.service.client.NotificationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class WebClientNotificationClient implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientNotificationClient.class);

    private final WebClient webClient;
    private final String notificationServiceUrl;

    public WebClientNotificationClient(
            WebClient.Builder webClientBuilder,
            @Value("${SERVICES_NOTIFICATION_URL:http://localhost:8184}") String notificationServiceUrl) {
        this.notificationServiceUrl = notificationServiceUrl;
        this.webClient = webClientBuilder.baseUrl(notificationServiceUrl).build();
    }

    @Override
    public Mono<Void> sendPasswordResetOtp(String recipientEmail, String otpCode) {
        Map<String, String> payload = Map.of(
                "recipient", recipientEmail,
                "subject", "Occupify - Password Reset Verification Code",
                "content", "Your password reset verification code is: " + otpCode + ". This code expires in 5 minutes."
        );

        return webClient.post()
                .uri("/notifications/email")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorResume(ex -> {
                    log.warn("Downstream notification-service offline at [{}]. Fallback: Password reset OTP for [{}] is [{}]",
                            notificationServiceUrl, maskEmail(recipientEmail), otpCode);
                    return Mono.empty();
                });
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
