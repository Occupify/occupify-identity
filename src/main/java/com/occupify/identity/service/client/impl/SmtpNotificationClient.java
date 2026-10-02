package com.occupify.identity.service.client.impl;

import com.occupify.identity.exception.AuthErrorCode;
import com.occupify.identity.exception.AuthException;
import com.occupify.identity.service.client.NotificationClient;
import com.occupify.identity.util.EmailUtil;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;

@Component
public class SmtpNotificationClient implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(SmtpNotificationClient.class);
    private static final String REGISTRATION_TEMPLATE = "static/email/registration-otp.html";
    private static final String PASSWORD_RESET_TEMPLATE = "static/email/password-reset.html";

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final long expirationMinutes;

    public SmtpNotificationClient(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:no-reply@occupify.com}") String fromEmail,
            @Value("${app.otp.expiration-seconds:300}") long expirationSeconds) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail != null && !fromEmail.isBlank() ? fromEmail : "no-reply@occupify.com";
        this.expirationMinutes = Math.max(1, expirationSeconds / 60);
    }

    @Override
    public Mono<Void> sendRegistrationOtp(String recipientEmail, String otpCode) {
        return sendHtmlEmail(
                recipientEmail,
                "Occupify - Mã kích hoạt tài khoản",
                REGISTRATION_TEMPLATE,
                otpCode,
                "registration OTP"
        );
    }

    @Override
    public Mono<Void> sendPasswordResetOtp(String recipientEmail, String otpCode) {
        return sendHtmlEmail(
                recipientEmail,
                "Occupify - Mã đặt lại mật khẩu",
                PASSWORD_RESET_TEMPLATE,
                otpCode,
                "password reset OTP"
        );
    }

    private Mono<Void> sendHtmlEmail(
            String recipientEmail,
            String subject,
            String templatePath,
            String otpCode,
            String actionDescription) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_000));
        }

        return Mono.fromRunnable(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
                helper.setFrom(fromEmail);
                helper.setTo(recipientEmail);
                helper.setSubject(subject);

                String htmlContent = loadTemplateContent(templatePath, otpCode);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("Successfully dispatched {} email to [{}]", actionDescription, EmailUtil.mask(recipientEmail));
            } catch (MailException ex) {
                log.error("Failed to dispatch {} email to [{}] via SMTP: {}",
                        actionDescription, EmailUtil.mask(recipientEmail), ex.getMessage());
                throw new AuthException(AuthErrorCode.AUTH_011);
            } catch (Exception ex) {
                log.error("Unexpected error dispatching {} email to [{}]: {}",
                        actionDescription, EmailUtil.mask(recipientEmail), ex.getMessage());
                throw new AuthException(AuthErrorCode.AUTH_011);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
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
