package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.admin.AssignUserRolesUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.admin.GetAdminStatsUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.admin.GetAdminUsersUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.admin.ToggleUserStatusUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.admin.ToggleWalletFreezeUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.admin.command.AssignUserRolesCommand;
import com.tyson.digitalwallet.ddd.application.usecase.admin.command.ToggleUserStatusCommand;
import com.tyson.digitalwallet.ddd.application.usecase.admin.command.ToggleWalletFreezeCommand;
import com.tyson.digitalwallet.ddd.application.usecase.admin.response.AdminStatsResponse;
import com.tyson.digitalwallet.ddd.application.usecase.admin.response.AdminUserSummaryResponse;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.AssignRolesRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final GetAdminStatsUseCase getAdminStatsUseCase;
    private final GetAdminUsersUseCase getAdminUsersUseCase;
    private final ToggleUserStatusUseCase toggleUserStatusUseCase;
    private final AssignUserRolesUseCase assignUserRolesUseCase;
    private final ToggleWalletFreezeUseCase toggleWalletFreezeUseCase;

    public AdminController(
            GetAdminStatsUseCase getAdminStatsUseCase,
            GetAdminUsersUseCase getAdminUsersUseCase,
            ToggleUserStatusUseCase toggleUserStatusUseCase,
            AssignUserRolesUseCase assignUserRolesUseCase,
            ToggleWalletFreezeUseCase toggleWalletFreezeUseCase
    ) {
        this.getAdminStatsUseCase = getAdminStatsUseCase;
        this.getAdminUsersUseCase = getAdminUsersUseCase;
        this.toggleUserStatusUseCase = toggleUserStatusUseCase;
        this.assignUserRolesUseCase = assignUserRolesUseCase;
        this.toggleWalletFreezeUseCase = toggleWalletFreezeUseCase;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('SYSTEM_STATS')")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getSystemStats() {
        AdminStatsResponse stats = getAdminStatsUseCase.getSystemStats();
        return ApiResponse.ok("System statistics retrieved successfully", stats);
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<ApiResponse<List<AdminUserSummaryResponse>>> getAllUsers() {
        List<AdminUserSummaryResponse> users = getAdminUsersUseCase.getAllUsers();
        return ApiResponse.ok("User list retrieved successfully", users);
    }

    @PostMapping("/users/{userId}/toggle-status")
    @PreAuthorize("hasAuthority('USER_SUSPEND')")
    public ResponseEntity<ApiResponse<AdminUserSummaryResponse>> toggleUserStatus(@PathVariable UUID userId) {
        AdminUserSummaryResponse response = toggleUserStatusUseCase.toggleStatus(new ToggleUserStatusCommand(userId));
        return ApiResponse.ok("User status updated successfully", response);
    }

    @PostMapping("/users/{userId}/roles")
    @PreAuthorize("hasAuthority('USER_MANAGE_ROLE')")
    public ResponseEntity<ApiResponse<AdminUserSummaryResponse>> assignRoles(
            @PathVariable UUID userId,
            @Valid @RequestBody AssignRolesRequest request
    ) {
        AdminUserSummaryResponse response = assignUserRolesUseCase.assignRoles(
                new AssignUserRolesCommand(userId, request.roles())
        );
        return ApiResponse.ok("User roles updated successfully", response);
    }

    @PostMapping("/wallets/{walletId}/toggle-freeze")
    @PreAuthorize("hasAuthority('WALLET_FREEZE')")
    public ResponseEntity<ApiResponse<AdminUserSummaryResponse>> toggleWalletFreeze(@PathVariable UUID walletId) {
        AdminUserSummaryResponse response = toggleWalletFreezeUseCase.toggleFreeze(new ToggleWalletFreezeCommand(walletId));
        return ApiResponse.ok("Wallet freeze status updated successfully", response);
    }
}
