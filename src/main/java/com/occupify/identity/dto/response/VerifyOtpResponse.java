package com.occupify.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VerifyOtpResponse(
        String resetToken
) {
}
