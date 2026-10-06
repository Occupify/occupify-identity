package com.occupify.identity.service.email.impl;

import com.occupify.identity.exception.auth.AuthErrorCode;
import com.occupify.identity.exception.auth.AuthException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import reactor.test.StepVerifier;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceImplTest {

    private JavaMailSender mailSender;
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        emailService = new EmailServiceImpl(mailSender, "no-reply@occupify.com", 300L);
    }

    @Test
    void shouldSendPasswordResetOtpSuccessfully() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        StepVerifier.create(emailService.sendPasswordResetOtp("applicant@occupify.com", "849201"))
                .verifyComplete();

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
    }

    @Test
    void shouldSendRegistrationOtpSuccessfully() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        StepVerifier.create(emailService.sendRegistrationOtp("applicant@occupify.com", "123456"))
                .verifyComplete();

        verify(mailSender).send(any(MimeMessage.class));
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

    @Test
    void shouldThrowAuthExceptionWhenSmtpFails() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP server connection timeout"))
                .when(mailSender).send(any(MimeMessage.class));

        StepVerifier.create(emailService.sendPasswordResetOtp("applicant@occupify.com", "849201"))
                .expectErrorMatches(throwable -> throwable instanceof AuthException authEx
                        && authEx.getErrorCode() == AuthErrorCode.AUTH_011)
                .verify();
    }
}
