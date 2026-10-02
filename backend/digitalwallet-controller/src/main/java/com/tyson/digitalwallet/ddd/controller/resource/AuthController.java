package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.auth.LoginUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.auth.response.AuthResponse;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.auth.LoginDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.auth.AuthResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private LoginUseCase loginUseCase;

    public AuthController(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @GetMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@RequestBody LoginDto loginDto) {
        AuthResponse response = loginUseCase.login(loginDto.toCommand());

        return ApiResponse.ok("Login successfully!", AuthResponseDto.from(response));
    }
}
