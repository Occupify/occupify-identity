package com.occupify.identity.service.client;

import com.occupify.identity.exception.AuthErrorCode;
import com.occupify.identity.exception.AuthException;
import com.occupify.identity.service.client.impl.SmtpNotificationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpNotificationClientTest {

    private JavaMailSender mailSender;
    private SmtpNotificationClient client;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        client = new SmtpNotificationClient(mailSender, "no-reply@occupify.com");
    }

    @Test
    void shouldSendPasswordResetOtpSuccessfully() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        StepVerifier.create(client.sendPasswordResetOtp("applicant@occupify.com", "849201"))
                .verifyComplete();

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertEquals("no-reply@occupify.com", sentMessage.getFrom());
        assertNotNull(sentMessage.getTo());
        assertEquals("applicant@occupify.com", sentMessage.getTo()[0]);
        assertEquals("Occupify - Password Reset Verification Code", sentMessage.getSubject());
        assertNotNull(sentMessage.getText());
        assertTrue(sentMessage.getText().contains("849201"));
    }

    @Test
    void shouldFailWhenRecipientEmailIsBlank() {
        StepVerifier.create(client.sendPasswordResetOtp("", "849201"))
                .expectErrorMatches(throwable -> throwable instanceof AuthException authEx
                        && authEx.getErrorCode() == AuthErrorCode.AUTH_000)
                .verify();
    }

    @Test
    void shouldThrowAuthExceptionWhenSmtpFails() {
        doThrow(new MailSendException("SMTP server connection timeout"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        StepVerifier.create(client.sendPasswordResetOtp("applicant@occupify.com", "849201"))
                .expectErrorMatches(throwable -> throwable instanceof AuthException authEx
                        && authEx.getErrorCode() == AuthErrorCode.AUTH_011)
                .verify();
    }
}
