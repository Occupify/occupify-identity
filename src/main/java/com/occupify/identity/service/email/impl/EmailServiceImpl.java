package com.occupify.identity.service.email.impl;

import com.occupify.identity.exception.auth.AuthErrorCode;
import com.occupify.identity.exception.auth.AuthException;
import com.occupify.identity.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String REGISTRATION_TEMPLATE = "static/email/registration-otp.html";
    private static final String PASSWORD_RESET_TEMPLATE = "static/email/password-reset.html";

    private final AsyncEmailDispatcher asyncEmailDispatcher;

    @Override
    public Mono<Void> sendRegistrationOtp(String recipientEmail, String otpCode) {
        return sendHtmlEmail(new AsyncEmailDispatcher.EmailDispatchPayload(
                recipientEmail,
                "Occupify - Mã kích hoạt tài khoản",
                REGISTRATION_TEMPLATE,
                otpCode,
                "registration OTP"
        ));
    }

    @Override
    public Mono<Void> sendPasswordResetOtp(String recipientEmail, String otpCode) {
        return sendHtmlEmail(new AsyncEmailDispatcher.EmailDispatchPayload(
                recipientEmail,
                "Occupify - Mã đặt lại mật khẩu",
                PASSWORD_RESET_TEMPLATE,
                otpCode,
                "password reset OTP"
        ));
    }

    private Mono<Void> sendHtmlEmail(AsyncEmailDispatcher.EmailDispatchPayload payload) {
        if (payload.recipientEmail() == null || payload.recipientEmail().isBlank()) {
            return Mono.error(new AuthException(AuthErrorCode.AUTH_000));
        }

        return Mono.fromRunnable(() -> asyncEmailDispatcher.dispatchMimeMessage(payload))
                .then();
    }
}
