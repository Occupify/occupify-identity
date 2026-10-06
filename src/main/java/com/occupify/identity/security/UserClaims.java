package com.occupify.identity.security;

public record UserClaims(
        String userId,
        String email,
        String role
) {
}
