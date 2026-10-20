"use server";

import { backendFetch, BackendError } from "@/server/backend-client";
import { getSession } from "@/server/session";
import { AdminStats, AdminUser, TrialBalance, WalletReconciliation } from "../types/admin.types";
import { revalidatePath } from "next/cache";

export interface ActionResult<T> {
    success: boolean;
    message: string;
    errorCode?: string;
    data?: T;
}

export async function getAdminStatsAction(): Promise<ActionResult<AdminStats>> {
    try {
        const session = await getSession();
        if (!session.isLoggedIn || !session.user) {
            return { success: false, message: "Phiên đăng nhập đã hết hạn" };
        }

        const res = await backendFetch<AdminStats>("/api/v1/admin/stats");
        return {
            success: true,
            message: "Lấy thống kê thành công",
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message, errorCode: error.errorCode };
        }
        return { success: false, message: "Không thể kết nối đến máy chủ" };
    }
}

export async function getAdminUsersAction(): Promise<ActionResult<AdminUser[]>> {
    try {
        const session = await getSession();
        if (!session.isLoggedIn || !session.user) {
            return { success: false, message: "Phiên đăng nhập đã hết hạn" };
        }

        const res = await backendFetch<AdminUser[]>("/api/v1/admin/users");
        return {
            success: true,
            message: "Lấy danh sách người dùng thành công",
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message, errorCode: error.errorCode };
        }
        return { success: false, message: "Không thể kết nối đến máy chủ" };
    }
}

export async function toggleUserStatusAction(userId: string): Promise<ActionResult<AdminUser>> {
    try {
        const session = await getSession();
        if (!session.isLoggedIn || !session.user) {
            return { success: false, message: "Phiên đăng nhập đã hết hạn" };
        }

        const res = await backendFetch<AdminUser>(`/api/v1/admin/users/${userId}/toggle-status`, {
            method: "POST",
        });
        revalidatePath("/admin");
        return {
            success: true,
            message: "Cập nhật trạng thái người dùng thành công",
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message, errorCode: error.errorCode };
        }
        return { success: false, message: "Không thể cập nhật trạng thái người dùng" };
    }
}

export async function assignUserRolesAction(
    userId: string,
    roles: string[]
): Promise<ActionResult<AdminUser>> {
    try {
        const session = await getSession();
        if (!session.isLoggedIn || !session.user) {
            return { success: false, message: "Phiên đăng nhập đã hết hạn" };
        }

        const res = await backendFetch<AdminUser>(`/api/v1/admin/users/${userId}/roles`, {
            method: "POST",
            body: JSON.stringify({ roles }),
        });
        revalidatePath("/admin");
        return {
            success: true,
            message: "Phân quyền người dùng thành công",
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message, errorCode: error.errorCode };
        }
        return { success: false, message: "Không thể phân quyền người dùng" };
    }
}

export async function toggleWalletFreezeAction(walletId: string): Promise<ActionResult<AdminUser>> {
    try {
        const session = await getSession();
        if (!session.isLoggedIn || !session.user) {
            return { success: false, message: "Phiên đăng nhập đã hết hạn" };
        }

        const res = await backendFetch<AdminUser>(`/api/v1/admin/wallets/${walletId}/toggle-freeze`, {
            method: "POST",
        });
        revalidatePath("/admin");
        return {
            success: true,
            message: "Cập nhật trạng thái đóng băng ví thành công",
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message, errorCode: error.errorCode };
        }
        return { success: false, message: "Không thể cập nhật trạng thái ví" };
    }
}

export async function getTrialBalanceAction(): Promise<ActionResult<TrialBalance>> {
    try {
        const session = await getSession();
        if (!session.isLoggedIn || !session.user) {
            return { success: false, message: "Phiên đăng nhập đã hết hạn" };
        }

        const res = await backendFetch<TrialBalance>("/api/v1/admin/ledger/trial-balance");
        return {
            success: true,
            message: "Lấy bảng cân đối thử thành công",
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message, errorCode: error.errorCode };
        }
        return { success: false, message: "Không thể lấy bảng cân đối thử" };
    }
}

export async function reconcileWalletAction(walletId: string): Promise<ActionResult<WalletReconciliation>> {
    try {
        const session = await getSession();
        if (!session.isLoggedIn || !session.user) {
            return { success: false, message: "Phiên đăng nhập đã hết hạn" };
        }

        const res = await backendFetch<WalletReconciliation>(`/api/v1/admin/ledger/reconcile/${walletId}`);
        return {
            success: true,
            message: "Đối soát ví thành công",
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message, errorCode: error.errorCode };
        }
        return { success: false, message: "Không thể đối soát ví" };
    }
}


