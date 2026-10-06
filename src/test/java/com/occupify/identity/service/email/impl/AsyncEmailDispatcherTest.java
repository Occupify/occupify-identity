package com.occupify.identity.service.email.impl;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsyncEmailDispatcherTest {

    private JavaMailSender mailSender;
    private AsyncEmailDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        dispatcher = new AsyncEmailDispatcher(mailSender, "no-reply@occupify.com", 300L);
    }

    @Test
    void shouldDispatchMimeMessageSuccessfully() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        AsyncEmailDispatcher.EmailDispatchPayload payload = new AsyncEmailDispatcher.EmailDispatchPayload(
                "applicant@occupify.com",
                "Occupify - Test",
                "static/email/registration-otp.html",
                "123456",
                "registration OTP"
        );

        dispatcher.dispatchMimeMessage(payload);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertNotNull(captor.getValue());
    }

    @Test
    void shouldHandleSmtpExceptionGracefully() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP timeout")).when(mailSender).send(any(MimeMessage.class));

        AsyncEmailDispatcher.EmailDispatchPayload payload = new AsyncEmailDispatcher.EmailDispatchPayload(
                "applicant@occupify.com",
                "Occupify - Test",
                "static/email/registration-otp.html",
                "123456",
                "registration OTP"
        );

        assertDoesNotThrow(() -> dispatcher.dispatchMimeMessage(payload));
    }
}
