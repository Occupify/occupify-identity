package com.occupify.identity.jwt;

public record UserClaims(
        String userId,
        String email,
        String role
) {
}
