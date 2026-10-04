package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.exception.InvalidCredentialsException;
import com.tyson.digitalwallet.ddd.application.usecase.auth.CheckSessionUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.auth.LoginUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.auth.LogoutUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.auth.RefreshTokenUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.auth.RegisterUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.auth.command.RefreshTokenCommand;
import com.tyson.digitalwallet.ddd.application.usecase.auth.response.AuthResponse;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.auth.LoginDto;
import com.tyson.digitalwallet.ddd.controller.dto.request.auth.RegisterRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.request.auth.VerifyOtpRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.auth.AuthResponseDto;
import com.tyson.digitalwallet.ddd.controller.util.AuthCookieManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/auth", "/auth"})
public class AuthController {
    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;
    private final CheckSessionUseCase checkSessionUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final AuthCookieManager authCookieManager;

    public AuthController(
            LoginUseCase loginUseCase,
            RegisterUseCase registerUseCase,
            CheckSessionUseCase checkSessionUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase,
            AuthCookieManager authCookieManager
    ) {
        this.loginUseCase = loginUseCase;
        this.registerUseCase = registerUseCase;
        this.checkSessionUseCase = checkSessionUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.authCookieManager = authCookieManager;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@Valid @RequestBody LoginDto loginDto) {
        AuthResponse response = loginUseCase.login(loginDto.toCommand());
        ResponseCookie cookie = authCookieManager.createRefreshTokenCookie(response.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.okBody("Login successfully!", AuthResponseDto.from(response)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody RegisterRequestDto requestDto) {
        registerUseCase.register(requestDto.toCommand());
        return ApiResponse.ok("An OTP verification code has been sent to your email. Please check your inbox!", null);
    }

    @PostMapping("/register/verify")
    public ResponseEntity<ApiResponse<AuthResponseDto>> verifyOtp(@RequestBody VerifyOtpRequestDto requestDto) {
        AuthResponse response = registerUseCase.verifyOtp(requestDto.toCommand());
        ResponseCookie cookie = authCookieManager.createRefreshTokenCookie(response.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.okBody("OTP verified successfully. Account has been activated!", AuthResponseDto.from(response)));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponseDto>> refreshToken(HttpServletRequest request) {
        String refreshToken = authCookieManager.extractRefreshToken(request)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token cookie is missing! Please log in again."));

        AuthResponse response = refreshTokenUseCase.refreshToken(new RefreshTokenCommand(refreshToken));
        ResponseCookie cookie = authCookieManager.createRefreshTokenCookie(response.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.okBody("Token refreshed successfully!", AuthResponseDto.from(response)));
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
        ResponseCookie cleanCookie = authCookieManager.deleteRefreshTokenCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                .body(ApiResponse.okBody("Logged out successfully!", null));
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
