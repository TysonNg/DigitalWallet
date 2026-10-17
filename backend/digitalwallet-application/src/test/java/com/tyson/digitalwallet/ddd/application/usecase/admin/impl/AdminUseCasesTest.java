package com.tyson.digitalwallet.ddd.application.usecase.admin.impl;

import com.tyson.digitalwallet.ddd.application.port.security.TokenStorage;
import com.tyson.digitalwallet.ddd.application.usecase.admin.command.AssignUserRolesCommand;
import com.tyson.digitalwallet.ddd.application.usecase.admin.command.ToggleUserStatusCommand;
import com.tyson.digitalwallet.ddd.application.usecase.admin.command.ToggleWalletFreezeCommand;
import com.tyson.digitalwallet.ddd.application.usecase.admin.response.AdminStatsResponse;
import com.tyson.digitalwallet.ddd.application.usecase.admin.response.AdminUserSummaryResponse;
import com.tyson.digitalwallet.ddd.application.usecase.admin.impl.ToggleWalletFreezeUseCaseImpl;
import com.tyson.digitalwallet.ddd.domain.model.entity.Authority;
import com.tyson.digitalwallet.ddd.domain.model.entity.Role;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.model.enums.TransactionStatus;
import com.tyson.digitalwallet.ddd.domain.model.enums.UserStatus;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;
import com.tyson.digitalwallet.ddd.domain.repository.RoleRepository;
import com.tyson.digitalwallet.ddd.domain.repository.TransactionRepository;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AdminUseCasesTest {

    private UserRepository userRepository;
    private WalletRepository walletRepository;
    private TransactionRepository transactionRepository;
    private RoleRepository roleRepository;
    private TokenStorage tokenStorage;

    @Before
    public void setUp() {
        userRepository = mock(UserRepository.class);
        walletRepository = mock(WalletRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        roleRepository = mock(RoleRepository.class);
        tokenStorage = mock(TokenStorage.class);
    }

    @Test
    public void testGetAdminStats() {
        when(userRepository.count()).thenReturn(50L);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(45L);
        when(userRepository.countByStatus(UserStatus.SUSPENDED)).thenReturn(5L);
        when(walletRepository.count()).thenReturn(48L);
        when(walletRepository.getTotalBalance()).thenReturn(new BigDecimal("250000000"));
        when(transactionRepository.count()).thenReturn(1200L);
        when(transactionRepository.getTotalVolumeByStatus(TransactionStatus.SUCCESS)).thenReturn(new BigDecimal("980000000"));

        GetAdminStatsUseCaseImpl useCase = new GetAdminStatsUseCaseImpl(
                userRepository, walletRepository, transactionRepository
        );

        AdminStatsResponse response = useCase.getSystemStats();

        assertEquals(50L, response.totalUsers());
        assertEquals(45L, response.activeUsers());
        assertEquals(5L, response.suspendedUsers());
        assertEquals(48L, response.totalWallets());
        assertEquals(new BigDecimal("250000000"), response.totalWalletBalance());
        assertEquals(1200L, response.totalTransactions());
        assertEquals(new BigDecimal("980000000"), response.totalTransactionVolume());
    }

    @Test
    public void testGetAdminUsers() {
        UUID userId = UUID.randomUUID();
        Authority auth = new Authority(1L, "SYSTEM_STATS", "Stats access", LocalDateTime.now());
        Role role = new Role(1L, "ROLE_ADMIN", "Admin role", LocalDateTime.now(), Set.of(auth));
        User user = new User(
                userId, "Admin", "admin@domain.com", "pass", Set.of(role),
                "0987654321", LocalDate.of(1990, 1, 1), UserStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(userRepository.findAll()).thenReturn(List.of(user));
        Wallet wallet = new Wallet(
                UUID.randomUUID(), userId, new BigDecimal("1500000"), "VND",
                WalletStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now()
        );
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        GetAdminUsersUseCaseImpl useCase = new GetAdminUsersUseCaseImpl(userRepository, walletRepository);
        List<AdminUserSummaryResponse> results = useCase.getAllUsers();

        assertEquals(1, results.size());
        AdminUserSummaryResponse summary = results.get(0);
        assertEquals(userId, summary.id());
        assertEquals("admin@domain.com", summary.email());
        assertTrue(summary.roles().contains("ROLE_ADMIN"));
        assertTrue(summary.authorities().contains("SYSTEM_STATS"));
        assertEquals(new BigDecimal("1500000"), summary.walletBalance());
    }

    @Test
    public void testToggleUserStatus_ActiveToSuspended() {
        UUID userId = UUID.randomUUID();
        User user = new User(
                userId, "User", "user@domain.com", "pass", new HashSet<>(),
                "0123456789", LocalDate.of(1995, 2, 2), UserStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ToggleUserStatusUseCaseImpl useCase = new ToggleUserStatusUseCaseImpl(
                userRepository, walletRepository, tokenStorage
        );

        AdminUserSummaryResponse response = useCase.toggleStatus(new ToggleUserStatusCommand(userId));

        assertEquals(UserStatus.SUSPENDED, response.status());
        verify(tokenStorage).deleteRefreshToken(userId);
        verify(tokenStorage).deleteActiveSession(userId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void testAssignUserRoles() {
        UUID userId = UUID.randomUUID();
        User user = new User(
                userId, "User", "user@domain.com", "pass", new HashSet<>(),
                "0123456789", LocalDate.of(1995, 2, 2), UserStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now()
        );

        Role adminRole = new Role(2L, "ROLE_ADMIN", "Admin role", LocalDateTime.now(), new HashSet<>());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssignUserRolesUseCaseImpl useCase = new AssignUserRolesUseCaseImpl(
                userRepository, roleRepository, walletRepository, tokenStorage
        );

        AdminUserSummaryResponse response = useCase.assignRoles(
                new AssignUserRolesCommand(userId, Set.of("ROLE_ADMIN"))
        );

        assertNotNull(response);
        assertTrue(response.roles().contains("ROLE_ADMIN"));
        verify(tokenStorage).deleteActiveSession(userId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void testToggleWalletFreeze_ActiveToFrozen() {
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Wallet wallet = new Wallet(
                walletId, userId, new BigDecimal("2000000"), "VND",
                WalletStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now()
        );
        User user = new User(
                userId, "Customer", "customer@wallet.com", "pass", new HashSet<>(),
                "0911223344", LocalDate.of(1998, 3, 3), UserStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.saveWallet(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ToggleWalletFreezeUseCaseImpl useCase = new ToggleWalletFreezeUseCaseImpl(walletRepository, userRepository);
        AdminUserSummaryResponse response = useCase.toggleFreeze(new ToggleWalletFreezeCommand(walletId));

        assertNotNull(response);
        assertEquals(walletId, response.walletId());
        assertEquals(WalletStatus.FROZEN, response.walletStatus());
        verify(walletRepository).saveWallet(any(Wallet.class));
    }

    @Test
    public void testToggleWalletFreeze_FrozenToActive() {
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Wallet wallet = new Wallet(
                walletId, userId, new BigDecimal("2000000"), "VND",
                WalletStatus.FROZEN, LocalDateTime.now(), LocalDateTime.now()
        );
        User user = new User(
                userId, "Customer", "customer@wallet.com", "pass", new HashSet<>(),
                "0911223344", LocalDate.of(1998, 3, 3), UserStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.saveWallet(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ToggleWalletFreezeUseCaseImpl useCase = new ToggleWalletFreezeUseCaseImpl(walletRepository, userRepository);
        AdminUserSummaryResponse response = useCase.toggleFreeze(new ToggleWalletFreezeCommand(walletId));

        assertNotNull(response);
        assertEquals(walletId, response.walletId());
        assertEquals(WalletStatus.ACTIVE, response.walletStatus());
        verify(walletRepository).saveWallet(any(Wallet.class));
    }
}
