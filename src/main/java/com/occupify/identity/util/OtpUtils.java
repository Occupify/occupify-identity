package com.occupify.identity.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateNumericOtp(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("OTP length must be greater than 0");
        }

        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(RANDOM.nextInt(10));
        }
        return otp.toString();
    }

    public String generateNumericOtp() {
        return generateNumericOtp(6);
    }
}
