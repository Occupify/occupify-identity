package com.occupify.identity.service.client;

import com.occupify.identity.exception.AuthErrorCode;
import com.occupify.identity.exception.AuthException;
import com.occupify.identity.service.client.impl.SmtpNotificationClient;
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

class SmtpNotificationClientTest {

        private JavaMailSender mailSender;
        private SmtpNotificationClient client;

        @BeforeEach
        void setUp() {
                mailSender = mock(JavaMailSender.class);
                client = new SmtpNotificationClient(mailSender, "no-reply@occupify.com", 300L);
        }

        @Test
        void shouldSendPasswordResetOtpSuccessfully() {
                MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
                when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
                doNothing().when(mailSender).send(any(MimeMessage.class));

                StepVerifier.create(client.sendPasswordResetOtp("applicant@occupify.com", "849201"))
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

                StepVerifier.create(client.sendRegistrationOtp("applicant@occupify.com", "123456"))
                                .verifyComplete();

                verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        void shouldFailWhenRecipientEmailIsBlank() {
                StepVerifier.create(client.sendPasswordResetOtp("", "849201"))
                                .expectErrorMatches(throwable -> throwable instanceof AuthException authEx
                                                && authEx.getErrorCode() == AuthErrorCode.AUTH_000)
                                .verify();

                StepVerifier.create(client.sendRegistrationOtp("", "849201"))
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

                StepVerifier.create(client.sendPasswordResetOtp("applicant@occupify.com", "849201"))
                                .expectErrorMatches(throwable -> throwable instanceof AuthException authEx
                                                && authEx.getErrorCode() == AuthErrorCode.AUTH_011)
                                .verify();
        }
}
