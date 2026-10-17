"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/query-keys";
import {
    getAdminStatsAction,
    getAdminUsersAction,
    toggleUserStatusAction,
    assignUserRolesAction,
    toggleWalletFreezeAction,
} from "../actions/admin.actions";
import { toast } from "sonner";
import { AdminStats, AdminUser } from "../types/admin.types";

export function useAdminStatsQuery(initialData?: AdminStats | null) {
    return useQuery({
        queryKey: queryKeys.admin.stats,
        queryFn: async () => {
            const res = await getAdminStatsAction();
            if (!res.success || !res.data) {
                throw new Error(res.message || "Không thể tải thống kê hệ thống");
            }
            return res.data;
        },
        initialData: initialData ?? undefined,
        refetchInterval: 30000,
    });
}

export function useAdminUsersQuery(initialData?: AdminUser[] | null) {
    return useQuery({
        queryKey: queryKeys.admin.users,
        queryFn: async () => {
            const res = await getAdminUsersAction();
            if (!res.success || !res.data) {
                throw new Error(res.message || "Không thể tải danh sách người dùng");
            }
            return res.data;
        },
        initialData: initialData ?? undefined,
    });
}

export function useToggleUserStatusMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (userId: string) => {
            const res = await toggleUserStatusAction(userId);
            if (!res.success || !res.data) {
                throw new Error(res.message || "Thao tác thất bại");
            }
            return res.data;
        },
        onSuccess: (updatedUser) => {
            toast.success(
                `Tài khoản ${updatedUser.email} đã được chuyển sang trạng thái ${updatedUser.status}`
            );
            queryClient.invalidateQueries({ queryKey: queryKeys.admin.users });
            queryClient.invalidateQueries({ queryKey: queryKeys.admin.stats });
        },
        onError: (err: Error) => {
            toast.error(err.message || "Lỗi cập nhật trạng thái");
        },
    });
}

export function useAssignUserRolesMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ userId, roles }: { userId: string; roles: string[] }) => {
            const res = await assignUserRolesAction(userId, roles);
            if (!res.success || !res.data) {
                throw new Error(res.message || "Thao tác thất bại");
            }
            return res.data;
        },
        onSuccess: (updatedUser) => {
            toast.success(`Đã cập nhật vai trò cho người dùng ${updatedUser.email}`);
            queryClient.invalidateQueries({ queryKey: queryKeys.admin.users });
        },
        onError: (err: Error) => {
            toast.error(err.message || "Lỗi phân quyền người dùng");
        },
    });
}

export function useToggleWalletFreezeMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (walletId: string) => {
            const res = await toggleWalletFreezeAction(walletId);
            if (!res.success || !res.data) {
                throw new Error(res.message || "Thao tác thất bại");
            }
            return res.data;
        },
        onSuccess: (updatedUser) => {
            const isFrozen = updatedUser.walletStatus === "FROZEN";
            toast.success(
                isFrozen
                    ? `Ví của người dùng ${updatedUser.email} đã được ĐÓNG BĂNG`
                    : `Ví của người dùng ${updatedUser.email} đã được MỞ BĂNG thành công`
            );
            queryClient.invalidateQueries({ queryKey: queryKeys.admin.users });
            queryClient.invalidateQueries({ queryKey: queryKeys.admin.stats });
        },
        onError: (err: Error) => {
            toast.error(err.message || "Lỗi cập nhật trạng thái ví");
        },
    });
}

