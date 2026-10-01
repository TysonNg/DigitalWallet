package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.user.ChangeEmailUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.user.GetUserUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.user.response.UserResponse;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.user.ChangeEmailUserRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.request.user.RegisterUserRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.user.UserResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;
    private final GetUserUseCase getUserUseCase;
    private final ChangeEmailUseCase changeEmailUseCase;

    public UserController(
            RegisterUserUseCase registerUserUseCase,
            GetUserUseCase getUserUseCase,
            ChangeEmailUseCase changeEmailUseCase
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.getUserUseCase = getUserUseCase;
        this.changeEmailUseCase = changeEmailUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponseDto>> register(@RequestBody RegisterUserRequestDto requestDto) {
        UserResponse response = registerUserUseCase.registerAccount(requestDto.toCommand());
        return ApiResponse.created("User registered successfully", UserResponseDto.from(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable UUID id) {
        UserResponse response = getUserUseCase.getUserById(id);
        return ApiResponse.ok("Get user successfully!",UserResponseDto.from(response));
    }

    @PostMapping("/email")
    public ResponseEntity<ApiResponse<UserResponseDto>> changeEmail(@RequestBody ChangeEmailUserRequestDto requestDto) {
        UserResponse response = changeEmailUseCase.changeEmail(requestDto.toCommand());
        return ApiResponse.ok("Email updated successfully", UserResponseDto.from(response));
    }
}
