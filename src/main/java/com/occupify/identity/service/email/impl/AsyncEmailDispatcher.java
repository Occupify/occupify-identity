package com.occupify.identity.service.email.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class AsyncEmailDispatcher {

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final long expirationMinutes;

    public AsyncEmailDispatcher(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:no-reply@occupify.com}") String fromEmail,
            @Value("${app.otp.expiration-seconds:300}") long expirationSeconds) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail != null && !fromEmail.isBlank() ? fromEmail : "no-reply@occupify.com";
        this.expirationMinutes = Math.max(1, expirationSeconds / 60);
    }

    public record EmailDispatchPayload(
            String recipientEmail,
            String subject,
            String templatePath,
            String otpCode,
            String actionDescription
    ) {}

    @Async("mailExecutor")
    public void dispatchMimeMessage(EmailDispatchPayload payload) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(payload.recipientEmail());
            helper.setSubject(payload.subject());

            String htmlContent = loadTemplateContent(payload.templatePath(), payload.otpCode());
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Successfully dispatched {} email to [{}] asynchronously", payload.actionDescription(), payload.recipientEmail());
        } catch (MailException ex) {
            log.error("Failed to dispatch {} email to [{}] via SMTP: {}",
                    payload.actionDescription(), payload.recipientEmail(), ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error dispatching {} email to [{}]: {}",
                    payload.actionDescription(), payload.recipientEmail(), ex.getMessage());
        }
    }

    private String loadTemplateContent(String templatePath, String otpCode) {
        try {
            return new ClassPathResource(templatePath)
                    .getContentAsString(StandardCharsets.UTF_8)
                    .replace("{{otp}}", otpCode)
                    .replace("{{expiryMinutes}}", String.valueOf(expirationMinutes));
        } catch (Exception e) {
            log.warn("Failed to load email template [{}], using fallback text: {}", templatePath, e.getMessage());
            return "Your Occupify verification code is: " + otpCode + " (expires in " + expirationMinutes + " minutes)";
        }
    }
}
