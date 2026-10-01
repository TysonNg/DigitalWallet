package com.tyson.digitalwallet.ddd.controller.dto.response.auth;

import com.tyson.digitalwallet.ddd.application.usecase.auth.response.AuthResponse;
import com.tyson.digitalwallet.ddd.controller.dto.response.user.UserResponseDto;

public record AuthResponseDto(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        UserResponseDto user
) {
    public static AuthResponseDto from(AuthResponse response) {
        if(response == null) return null;

        return new AuthResponseDto(
                response.accessToken(),
                response.refreshToken(),
                response.tokenType(),
                response.expiresIn(),
                UserResponseDto.from(response.user())
        );
    }
}
