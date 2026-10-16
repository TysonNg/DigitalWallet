package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.user.ChangeEmailUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.user.ChangePasswordUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.user.GetUserUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.user.response.UserResponse;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.user.ChangeEmailUserRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.request.user.ChangePasswordRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.user.UserResponseDto;
import com.tyson.digitalwallet.ddd.controller.ratelimit.RateLimit;
import com.tyson.digitalwallet.ddd.controller.ratelimit.RateLimitType;
import com.tyson.digitalwallet.ddd.controller.security.WalletAccess;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/users", "/users"})
public class UserController {

    private final GetUserUseCase getUserUseCase;
    private final ChangeEmailUseCase changeEmailUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    public UserController(
            GetUserUseCase getUserUseCase,
            ChangeEmailUseCase changeEmailUseCase,
            ChangePasswordUseCase changePasswordUseCase
    ) {
        this.getUserUseCase = getUserUseCase;
        this.changeEmailUseCase = changeEmailUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable("id") UUID id) {
        UserResponse response = getUserUseCase.getUserById(id);
        return ApiResponse.ok("Get user successfully!", UserResponseDto.from(response));
    }

    @PostMapping("/email")
    public ResponseEntity<ApiResponse<UserResponseDto>> changeEmail(@RequestBody ChangeEmailUserRequestDto requestDto) {
        UserResponse response = changeEmailUseCase.changeEmail(requestDto.toCommand());
        return ApiResponse.ok("Email updated successfully", UserResponseDto.from(response));
    }

    @RateLimit(name = "authRateLimiter", type = RateLimitType.USER_OR_IP)
    @PostMapping({"/password", "/change-password"})
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto requestDto,
            Principal principal
    ) {
        UUID currentUserId = WalletAccess.currentUser(principal);
        changePasswordUseCase.changePassword(requestDto.toCommand(currentUserId));
        return ApiResponse.ok("Password changed successfully", null);
    }
}
