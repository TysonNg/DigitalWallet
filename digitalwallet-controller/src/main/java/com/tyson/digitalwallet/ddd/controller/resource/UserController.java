package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.user.ChangeEmailUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.user.GetUserUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.user.RegisterUserUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.user.response.UserResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.ChangeEmailUserRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.request.RegisterUserRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.UserResponseDto;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<UserResponseDto> register(@RequestBody RegisterUserRequestDto requestDto) {
        UserResponse response = registerUserUseCase.registerAccount(requestDto.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDto.from(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
        UserResponse response = getUserUseCase.getUserById(id);
        return ResponseEntity.ok(UserResponseDto.from(response));
    }

    @PostMapping("/email")
    public ResponseEntity<UserResponseDto> changeEmail(@RequestBody ChangeEmailUserRequestDto requestDto) {
        UserResponse response = changeEmailUseCase.changeEmail(requestDto.toCommand());
        return ResponseEntity.ok(UserResponseDto.from(response));
    }
}
