package com.occupify.identity.service.client.impl;

import com.occupify.identity.exception.AuthErrorCode;
import com.occupify.identity.exception.AuthException;
import com.occupify.identity.service.client.NotificationClient;
import com.occupify.identity.util.EmailUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class SmtpNotificationClient implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(SmtpNotificationClient.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public SmtpNotificationClient(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:no-reply@occupify.com}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail != null && !fromEmail.isBlank() ? fromEmail : "no-reply@occupify.com";
    }

    @Override
    public Mono<Void> sendPasswordResetOtp(String recipientEmail, String otpCode) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_000));
        }

        return Mono.fromRunnable(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(recipientEmail);
                message.setSubject("Occupify - Password Reset Verification Code");
                message.setText(buildEmailContent(otpCode));

                mailSender.send(message);
                log.info("Successfully dispatched password reset OTP email to [{}]", EmailUtil.mask(recipientEmail));
            } catch (MailException ex) {
                log.error("Failed to dispatch OTP email to [{}] via SMTP: {}",
                        EmailUtil.mask(recipientEmail), ex.getMessage());
                throw new AuthException(AuthErrorCode.AUTH_011);
            } catch (Exception ex) {
                log.error("Unexpected error dispatching OTP email to [{}]: {}",
                        EmailUtil.mask(recipientEmail), ex.getMessage());
                throw new AuthException(AuthErrorCode.AUTH_011);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    private String buildEmailContent(String otpCode) {
        return """
                Hello,

                You recently requested to reset your password for your Occupify account.
                Your 6-digit verification code (OTP) is:

                %s

                This code expires in 5 minutes. If you did not make this request, you can safely ignore this email.

                Best regards,
                Occupify Security Team
                """.formatted(otpCode);
    }
}
