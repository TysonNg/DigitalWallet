"use client";

import { useState } from "react";
import { User } from "@/features/auth/types/auth.types";
import { AdminStats, AdminUser } from "../types/admin.types";
import {
    useAdminStatsQuery,
    useAdminUsersQuery,
    useToggleUserStatusMutation,
    useAssignUserRolesMutation,
    useToggleWalletFreezeMutation,
} from "../hooks/use-admin";
import { AdminHeader } from "../components/admin-header";
import { AdminKpiStrip } from "../components/admin-kpi-strip";
import { AdminUserTable } from "../components/admin-user-table";
import { ConfirmToggleDialog } from "../components/confirm-toggle-dialog";
import { ConfirmFreezeDialog } from "../components/confirm-freeze-dialog";
import { RoleAssignmentModal } from "../components/role-assignment-modal";

interface AdminConsoleViewProps {
    currentUser?: User;
    initialStats?: AdminStats | null;
    initialUsers?: AdminUser[] | null;
}

export function AdminConsoleView({
    currentUser,
    initialStats,
    initialUsers,
}: AdminConsoleViewProps) {
    const {
        data: stats,
        isPending: statsPending,
        isRefetching: statsRefetching,
        refetch: refetchStats,
    } = useAdminStatsQuery(initialStats);

    const {
        data: users = [],
        isPending: usersPending,
        isRefetching: usersRefetching,
        refetch: refetchUsers,
    } = useAdminUsersQuery(initialUsers);

    const toggleStatusMutation = useToggleUserStatusMutation();
    const assignRolesMutation = useAssignUserRolesMutation();
    const toggleWalletFreezeMutation = useToggleWalletFreezeMutation();

    const [userToToggle, setUserToToggle] = useState<AdminUser | null>(null);
    const [userToAssign, setUserToAssign] = useState<AdminUser | null>(null);
    const [userToFreeze, setUserToFreeze] = useState<AdminUser | null>(null);

    const handleRefreshAll = () => {
        void refetchStats();
        void refetchUsers();
    };

    const handleConfirmToggle = (userId: string) => {
        toggleStatusMutation.mutate(userId, {
            onSettled: () => setUserToToggle(null),
        });
    };

    const handleConfirmAssignRoles = (userId: string, roles: string[]) => {
        assignRolesMutation.mutate(
            { userId, roles },
            {
                onSettled: () => setUserToAssign(null),
            }
        );
    };

    const handleConfirmToggleFreeze = (walletId: string) => {
        toggleWalletFreezeMutation.mutate(walletId, {
            onSettled: () => setUserToFreeze(null),
        });
    };

    const isRefreshing = statsRefetching || usersRefetching;

    return (
        <div className="min-h-screen bg-slate-50/60 dark:bg-slate-950 flex flex-col">
            <AdminHeader
                currentUser={currentUser}
                isRefreshing={isRefreshing}
                onRefresh={handleRefreshAll}
            />

            <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 flex flex-col gap-6">
                {/* Compact KPI Metrics Strip on top */}
                <AdminKpiStrip stats={stats} isPending={statsPending} />

                {/* Data Grid Centered View */}
                <AdminUserTable
                    users={users}
                    isPending={usersPending}
                    onToggleStatus={(user) => setUserToToggle(user)}
                    onAssignRoles={(user) => setUserToAssign(user)}
                    onToggleWalletFreeze={(user) => setUserToFreeze(user)}
                />
            </main>

            {/* Confirmation Dialog for Lock / Unlock Account */}
            <ConfirmToggleDialog
                user={userToToggle}
                isOpen={!!userToToggle}
                isPending={toggleStatusMutation.isPending}
                onClose={() => setUserToToggle(null)}
                onConfirm={handleConfirmToggle}
            />

            {/* Confirmation Dialog for Wallet Freeze / Unfreeze */}
            <ConfirmFreezeDialog
                user={userToFreeze}
                isOpen={!!userToFreeze}
                isPending={toggleWalletFreezeMutation.isPending}
                onClose={() => setUserToFreeze(null)}
                onConfirm={handleConfirmToggleFreeze}
            />

            {/* Role & Authority Assignment Modal */}
            <RoleAssignmentModal
                user={userToAssign}
                isOpen={!!userToAssign}
                isPending={assignRolesMutation.isPending}
                onClose={() => setUserToAssign(null)}
                onSave={handleConfirmAssignRoles}
            />
        </div>
    );
}
