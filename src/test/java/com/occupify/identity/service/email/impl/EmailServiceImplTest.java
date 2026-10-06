package com.occupify.identity.service.email.impl;

import com.occupify.identity.exception.auth.AuthErrorCode;
import com.occupify.identity.exception.auth.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailServiceImplTest {

    private AsyncEmailDispatcher asyncEmailDispatcher;
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        asyncEmailDispatcher = mock(AsyncEmailDispatcher.class);
        emailService = new EmailServiceImpl(asyncEmailDispatcher);
    }

    @Test
    void shouldSendPasswordResetOtpSuccessfully() {
        doNothing().when(asyncEmailDispatcher).dispatchMimeMessage(any());

        StepVerifier.create(emailService.sendPasswordResetOtp("applicant@occupify.com", "849201"))
                .verifyComplete();

        ArgumentCaptor<AsyncEmailDispatcher.EmailDispatchPayload> captor =
                ArgumentCaptor.forClass(AsyncEmailDispatcher.EmailDispatchPayload.class);
        verify(asyncEmailDispatcher).dispatchMimeMessage(captor.capture());

        AsyncEmailDispatcher.EmailDispatchPayload payload = captor.getValue();
        assertEquals("applicant@occupify.com", payload.recipientEmail());
        assertEquals("849201", payload.otpCode());
    }

    @Test
    void shouldSendRegistrationOtpSuccessfully() {
        doNothing().when(asyncEmailDispatcher).dispatchMimeMessage(any());

        StepVerifier.create(emailService.sendRegistrationOtp("applicant@occupify.com", "123456"))
                .verifyComplete();

        ArgumentCaptor<AsyncEmailDispatcher.EmailDispatchPayload> captor =
                ArgumentCaptor.forClass(AsyncEmailDispatcher.EmailDispatchPayload.class);
        verify(asyncEmailDispatcher).dispatchMimeMessage(captor.capture());

        AsyncEmailDispatcher.EmailDispatchPayload payload = captor.getValue();
        assertEquals("applicant@occupify.com", payload.recipientEmail());
        assertEquals("123456", payload.otpCode());
    }

    @Test
    void shouldFailWhenRecipientEmailIsBlank() {
        StepVerifier.create(emailService.sendPasswordResetOtp("", "849201"))
                .expectErrorMatches(throwable -> throwable instanceof AuthException authEx
                        && authEx.getErrorCode() == AuthErrorCode.AUTH_000)
                .verify();

        StepVerifier.create(emailService.sendRegistrationOtp("", "849201"))
                .expectErrorMatches(throwable -> throwable instanceof AuthException authEx
                        && authEx.getErrorCode() == AuthErrorCode.AUTH_000)
                .verify();
    }
}
