package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.exception.InvalidCredentialsException;
import com.tyson.digitalwallet.ddd.application.usecase.auth.CheckSessionUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.auth.LoginUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.auth.LogoutUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.auth.RefreshTokenUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.auth.RegisterUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.auth.response.AuthResponse;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.auth.LoginDto;
import com.tyson.digitalwallet.ddd.controller.dto.request.auth.RefreshTokenRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.request.auth.RegisterRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.request.auth.VerifyOtpRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.auth.AuthResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;
    private final CheckSessionUseCase checkSessionUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    public AuthController(
            LoginUseCase loginUseCase,
            RegisterUseCase registerUseCase,
            CheckSessionUseCase checkSessionUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase
    ) {
        this.loginUseCase = loginUseCase;
        this.registerUseCase = registerUseCase;
        this.checkSessionUseCase = checkSessionUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@Valid @RequestBody LoginDto loginDto) {
        AuthResponse response = loginUseCase.login(loginDto.toCommand());
        return ApiResponse.ok("Login successfully!", AuthResponseDto.from(response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody RegisterRequestDto requestDto) {
        registerUseCase.register(requestDto.toCommand());
        return ApiResponse.ok("An OTP verification code has been sent to your email. Please check your inbox!", null);
    }

    @PostMapping("/register/verify")
    public ResponseEntity<ApiResponse<AuthResponseDto>> verifyOtp(@RequestBody VerifyOtpRequestDto requestDto) {
        AuthResponse response = registerUseCase.verifyOtp(requestDto.toCommand());
        return ApiResponse.ok("OTP verified successfully. Account has been activated!", AuthResponseDto.from(response));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponseDto>> refreshToken(@Valid @RequestBody RefreshTokenRequestDto requestDto) {
        AuthResponse response = refreshTokenUseCase.refreshToken(requestDto.toCommand());
        return ApiResponse.ok("Token refreshed successfully!", AuthResponseDto.from(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(name = "Authorization", required = false) String authHeader
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidCredentialsException("Authorization header is missing or invalid!");
        }

        String token = authHeader.substring(7);
        logoutUseCase.logout(token);
        return ApiResponse.ok("Logged out successfully!", null);
    }

    @GetMapping("/check-session")
    public ResponseEntity<ApiResponse<Void>> checkSession(
            @RequestHeader(name = "Authorization", required = false) String authHeader
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidCredentialsException("Authorization header is missing or invalid!");
        }

        String token = authHeader.substring(7);
        checkSessionUseCase.checkSession(token);
        return ApiResponse.ok("Session is valid.", null);
    }
}
