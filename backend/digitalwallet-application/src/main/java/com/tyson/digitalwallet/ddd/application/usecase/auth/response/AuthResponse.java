package com.tyson.digitalwallet.ddd.application.usecase.auth.response;

import com.tyson.digitalwallet.ddd.application.usecase.user.response.UserResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        UserResponse user
) {
    public static AuthResponse of (
            String accessToken,
            String refreshToken,
            Long expiresIn,
            User user
    ) {
        if(user == null) return null;

        return new AuthResponse(
            accessToken,
            refreshToken,
            "Bearer",
            expiresIn,
            UserResponse.from(user)
        );
    }
}
