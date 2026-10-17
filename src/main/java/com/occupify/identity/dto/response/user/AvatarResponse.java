package com.occupify.identity.dto.response.user;

import org.springframework.lang.Nullable;

public record AvatarResponse(
        @Nullable String avatarUrl
) {
}
