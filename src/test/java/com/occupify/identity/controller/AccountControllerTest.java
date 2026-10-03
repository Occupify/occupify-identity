package com.occupify.identity.controller;

import com.occupify.identity.dto.request.ChangePasswordRequest;
import com.occupify.identity.exception.AuthControllerAdvice;
import com.occupify.identity.jwt.JwtUtils;
import com.occupify.identity.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private static final String TEST_SECRET = "occupify-super-secret-jwt-signing-key-for-unit-testing-must-be-at-least-64-bytes-long!";

    @Mock
    private AuthService authService;

    private JwtUtils jwtUtils;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(TEST_SECRET, 3600000, 604800000);
        AccountController controller = new AccountController(authService, jwtUtils);
        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new AuthControllerAdvice())
                .build();
    }

    @Test
    void shouldChangePasswordWith200OkUsingInjectedHeader() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123!", "NewPassword123!");
        when(authService.changePassword(eq("injected@occupify.com"), any(ChangePasswordRequest.class)))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/account/change-password")
                .header("X-User-Email", "injected@occupify.com")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("Password changed successfully");
    }

    @Test
    void shouldChangePasswordWith200OkUsingBearerToken() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123!", "NewPassword123!");
        String token = jwtUtils.generateAccessToken("tokenuser@occupify.com", UUID.randomUUID().toString(), "USER");
        when(authService.changePassword(eq("tokenuser@occupify.com"), any(ChangePasswordRequest.class)))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/account/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("Password changed successfully");
    }

    @Test
    void shouldReturn401WhenChangePasswordHasNoIdentityHeadersOrToken() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123!", "NewPassword123!");

        webTestClient.post()
                .uri("/account/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("AUTH_003");
    }
}
