package com.tyson.digitalwallet.ddd.controller.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.application.usecase.admin.AssignUserRolesUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.admin.GetAdminStatsUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.admin.GetAdminUsersUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.admin.ToggleUserStatusUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.admin.response.AdminStatsResponse;
import com.tyson.digitalwallet.ddd.application.usecase.admin.response.AdminUserSummaryResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.AssignRolesRequest;
import com.tyson.digitalwallet.ddd.controller.exception.GlobalExceptionHandler;
import com.tyson.digitalwallet.ddd.domain.model.enums.UserStatus;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.tyson.digitalwallet.ddd.application.usecase.admin.ToggleWalletFreezeUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.admin.command.ToggleWalletFreezeCommand;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;

public class AdminControllerTest {

    private GetAdminStatsUseCase getAdminStatsUseCase;
    private GetAdminUsersUseCase getAdminUsersUseCase;
    private ToggleUserStatusUseCase toggleUserStatusUseCase;
    private AssignUserRolesUseCase assignUserRolesUseCase;
    private ToggleWalletFreezeUseCase toggleWalletFreezeUseCase;
    private com.tyson.digitalwallet.ddd.application.usecase.admin.GetTrialBalanceUseCase getTrialBalanceUseCase;
    private com.tyson.digitalwallet.ddd.application.usecase.admin.ReconcileWalletUseCase reconcileWalletUseCase;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup() {
        getAdminStatsUseCase = mock(GetAdminStatsUseCase.class);
        getAdminUsersUseCase = mock(GetAdminUsersUseCase.class);
        toggleUserStatusUseCase = mock(ToggleUserStatusUseCase.class);
        assignUserRolesUseCase = mock(AssignUserRolesUseCase.class);
        toggleWalletFreezeUseCase = mock(ToggleWalletFreezeUseCase.class);
        getTrialBalanceUseCase = mock(com.tyson.digitalwallet.ddd.application.usecase.admin.GetTrialBalanceUseCase.class);
        reconcileWalletUseCase = mock(com.tyson.digitalwallet.ddd.application.usecase.admin.ReconcileWalletUseCase.class);

        AdminController adminController = new AdminController(
                getAdminStatsUseCase,
                getAdminUsersUseCase,
                toggleUserStatusUseCase,
                assignUserRolesUseCase,
                toggleWalletFreezeUseCase,
                getTrialBalanceUseCase,
                reconcileWalletUseCase
        );

        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    public void getSystemStats_shouldReturnOkWithData() throws Exception {
        AdminStatsResponse response = new AdminStatsResponse(
                100L, 90L, 10L, 95L, new BigDecimal("50000000"), 350L, new BigDecimal("120000000")
        );
        when(getAdminStatsUseCase.getSystemStats()).thenReturn(response);

        MvcResult result = mockMvc.perform(get("/api/v1/admin/stats")).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("\"success\":true"));
        assertTrue(content.contains("\"totalUsers\":100"));
        assertTrue(content.contains("\"activeUsers\":90"));
        assertTrue(content.contains("\"suspendedUsers\":10"));
        assertTrue(content.contains("\"totalWallets\":95"));

        verify(getAdminStatsUseCase).getSystemStats();
    }

    @Test
    public void getAllUsers_shouldReturnUserList() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        AdminUserSummaryResponse userSummary = new AdminUserSummaryResponse(
                userId,
                "Admin John",
                "admin@wallet.com",
                "0987654321",
                LocalDate.of(1995, 5, 20),
                UserStatus.ACTIVE,
                Set.of("ROLE_ADMIN"),
                Set.of("SYSTEM_STATS", "USER_READ"),
                walletId,
                WalletStatus.ACTIVE,
                new BigDecimal("1000000"),
                "VND",
                LocalDateTime.now()
        );
        when(getAdminUsersUseCase.getAllUsers()).thenReturn(List.of(userSummary));

        MvcResult result = mockMvc.perform(get("/api/v1/admin/users")).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("\"success\":true"));
        assertTrue(content.contains("admin@wallet.com"));
        assertTrue(content.contains("Admin John"));
        assertTrue(content.contains("ROLE_ADMIN"));

        verify(getAdminUsersUseCase).getAllUsers();
    }

    @Test
    public void toggleUserStatus_shouldReturnUpdatedUser() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        AdminUserSummaryResponse updatedUser = new AdminUserSummaryResponse(
                targetUserId,
                "Regular User",
                "user@wallet.com",
                "0123456789",
                LocalDate.of(2000, 1, 1),
                UserStatus.SUSPENDED,
                Set.of("ROLE_USER"),
                Set.of("USER_READ", "WALLET_READ"),
                walletId,
                WalletStatus.ACTIVE,
                BigDecimal.ZERO,
                "VND",
                LocalDateTime.now()
        );
        when(toggleUserStatusUseCase.toggleStatus(any())).thenReturn(updatedUser);

        MvcResult result = mockMvc.perform(post("/api/v1/admin/users/" + targetUserId + "/toggle-status")).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("\"success\":true"));
        assertTrue(content.contains("SUSPENDED"));

        verify(toggleUserStatusUseCase).toggleStatus(any());
    }

    @Test
    public void assignRoles_shouldReturnUpdatedUserWithNewRoles() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        AssignRolesRequest request = new AssignRolesRequest(Set.of("ROLE_USER", "ROLE_ADMIN"));
        AdminUserSummaryResponse updatedUser = new AdminUserSummaryResponse(
                targetUserId,
                "Promoted User",
                "promoted@wallet.com",
                "0123456789",
                LocalDate.of(2000, 1, 1),
                UserStatus.ACTIVE,
                Set.of("ROLE_USER", "ROLE_ADMIN"),
                Set.of("USER_READ", "SYSTEM_STATS"),
                walletId,
                WalletStatus.ACTIVE,
                BigDecimal.ZERO,
                "VND",
                LocalDateTime.now()
        );
        when(assignUserRolesUseCase.assignRoles(any())).thenReturn(updatedUser);

        MvcResult result = mockMvc.perform(post("/api/v1/admin/users/" + targetUserId + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("\"success\":true"));
        assertTrue(content.contains("promoted@wallet.com"));

        verify(assignUserRolesUseCase).assignRoles(any());
    }

    @Test
    public void toggleWalletFreeze_shouldReturnUpdatedUserWithFrozenStatus() throws Exception {
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AdminUserSummaryResponse updatedUser = new AdminUserSummaryResponse(
                userId,
                "Frozen User",
                "frozen@wallet.com",
                "0123456789",
                LocalDate.of(2000, 1, 1),
                UserStatus.ACTIVE,
                Set.of("ROLE_USER"),
                Set.of("USER_READ"),
                walletId,
                WalletStatus.FROZEN,
                new BigDecimal("500000"),
                "VND",
                LocalDateTime.now()
        );
        when(toggleWalletFreezeUseCase.toggleFreeze(any())).thenReturn(updatedUser);

        MvcResult result = mockMvc.perform(post("/api/v1/admin/wallets/" + walletId + "/toggle-freeze")).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("\"success\":true"));
        assertTrue(content.contains("FROZEN"));
        assertTrue(content.contains("frozen@wallet.com"));

        verify(toggleWalletFreezeUseCase).toggleFreeze(any());
    }
}
