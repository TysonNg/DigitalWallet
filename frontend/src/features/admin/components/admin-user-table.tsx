"use client";

import { useState, useMemo } from "react";
import { AdminUser } from "../types/admin.types";
import { Badge } from "@/components/ui/badge";
import {
    Search,
    Shield,
    Lock,
    Unlock,
    Filter,
    Calendar,
    Phone,
    Wallet,
    RotateCcw,
    Snowflake,
    Scale,
} from "lucide-react";

interface AdminUserTableProps {
    users: AdminUser[];
    isPending?: boolean;
    onToggleStatus: (user: AdminUser) => void;
    onAssignRoles: (user: AdminUser) => void;
    onToggleWalletFreeze: (user: AdminUser) => void;
    onReconcileWallet?: (user: AdminUser) => void;
}

export function AdminUserTable({
    users,
    isPending,
    onToggleStatus,
    onAssignRoles,
    onToggleWalletFreeze,
    onReconcileWallet,
}: AdminUserTableProps) {
    const [searchTerm, setSearchTerm] = useState("");
    const [statusFilter, setStatusFilter] = useState<string>("ALL");
    const [roleFilter, setRoleFilter] = useState<string>("ALL");
    const [walletFilter, setWalletFilter] = useState<string>("ALL");

    const formatCurrency = (amount?: number) => {
        if (amount == null) return "0 ₫";
        return new Intl.NumberFormat("vi-VN", {
            style: "currency",
            currency: "VND",
            maximumFractionDigits: 0,
        }).format(amount);
    };

    const formatDate = (dateStr?: string) => {
        if (!dateStr) return "-";
        try {
            return new Intl.DateTimeFormat("vi-VN", {
                day: "2-digit",
                month: "2-digit",
                year: "numeric",
            }).format(new Date(dateStr));
        } catch {
            return dateStr;
        }
    };

    const filteredUsers = useMemo(() => {
        return users.filter((user) => {
            const matchesSearch =
                user.fullName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                user.email?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                user.phoneNumber?.includes(searchTerm);

            const matchesStatus = statusFilter === "ALL" || user.status === statusFilter;
            const matchesRole = roleFilter === "ALL" || user.roles?.includes(roleFilter);
            const matchesWallet =
                walletFilter === "ALL" ||
                (walletFilter === "FROZEN" && user.walletStatus === "FROZEN") ||
                (walletFilter === "ACTIVE" && user.walletStatus !== "FROZEN");

            return matchesSearch && matchesStatus && matchesRole && matchesWallet;
        });
    }, [users, searchTerm, statusFilter, roleFilter, walletFilter]);

    return (
        <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl shadow-xs overflow-hidden flex flex-col flex-1">
            {/* Filter and Search Bar */}
            <div className="p-4 sm:p-5 border-b border-slate-200 dark:border-slate-800 flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between bg-slate-50/50 dark:bg-slate-900/50">
                <div className="relative flex-1 max-w-md">
                    <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input
                        type="text"
                        placeholder="Tìm kiếm theo tên, email hoặc số điện thoại..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="w-full pl-10 pr-4 py-2 text-sm bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 dark:text-white placeholder:text-slate-400 transition-all"
                    />
                </div>

                <div className="flex items-center gap-2 flex-wrap">
                    <div className="flex items-center gap-1.5 text-xs text-slate-500 dark:text-slate-400">
                        <Filter className="w-3.5 h-3.5" />
                        <span className="hidden sm:inline">Lọc:</span>
                    </div>

                    <select
                        value={statusFilter}
                        onChange={(e) => setStatusFilter(e.target.value)}
                        className="px-3 py-2 text-xs font-medium bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 text-slate-700 dark:text-slate-200 cursor-pointer"
                    >
                        <option value="ALL">Mọi trạng thái tài khoản</option>
                        <option value="ACTIVE">Tài khoản hoạt động</option>
                        <option value="SUSPENDED">Tài khoản bị khóa</option>
                    </select>

                    <select
                        value={walletFilter}
                        onChange={(e) => setWalletFilter(e.target.value)}
                        className="px-3 py-2 text-xs font-medium bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 text-slate-700 dark:text-slate-200 cursor-pointer"
                    >
                        <option value="ALL">Mọi trạng thái ví</option>
                        <option value="ACTIVE">Ví hoạt động</option>
                        <option value="FROZEN">Ví đóng băng (FROZEN)</option>
                    </select>

                    <select
                        value={roleFilter}
                        onChange={(e) => setRoleFilter(e.target.value)}
                        className="px-3 py-2 text-xs font-medium bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 text-slate-700 dark:text-slate-200 cursor-pointer"
                    >
                        <option value="ALL">Mọi vai trò</option>
                        <option value="ROLE_ADMIN">Quản trị viên (ROLE_ADMIN)</option>
                        <option value="ROLE_USER">Người dùng (ROLE_USER)</option>
                    </select>

                    {(searchTerm || statusFilter !== "ALL" || roleFilter !== "ALL" || walletFilter !== "ALL") && (
                        <button
                            type="button"
                            onClick={() => {
                                setSearchTerm("");
                                setStatusFilter("ALL");
                                setRoleFilter("ALL");
                                setWalletFilter("ALL");
                            }}
                            className="p-2 text-slate-500 hover:text-slate-700 dark:hover:text-slate-300 rounded-xl hover:bg-slate-200/50 dark:hover:bg-slate-800 transition-colors cursor-pointer"
                            title="Xóa bộ lọc"
                        >
                            <RotateCcw className="w-4 h-4" />
                        </button>
                    )}
                </div>
            </div>

            {/* Table Container */}
            <div className="overflow-x-auto flex-1">
                <table className="w-full text-left border-collapse text-sm">
                    <thead>
                        <tr className="border-b border-slate-200 dark:border-slate-800 bg-slate-50/80 dark:bg-slate-800/40 text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                            <th className="py-3 px-4 sm:px-6">Người dùng</th>
                            <th className="py-3 px-4">Số điện thoại</th>
                            <th className="py-3 px-4">Trạng thái</th>
                            <th className="py-3 px-4">Vai trò (Roles)</th>
                            <th className="py-3 px-4 text-right">Số dư ví</th>
                            <th className="py-3 px-4">Ngày tạo</th>
                            <th className="py-3 px-4 sm:px-6 text-right">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60">
                        {isPending ? (
                            <tr>
                                <td colSpan={7} className="py-12 text-center text-sm text-slate-400">
                                    Đang tải dữ liệu người dùng...
                                </td>
                            </tr>
                        ) : filteredUsers.length === 0 ? (
                            <tr>
                                <td colSpan={7} className="py-12 text-center text-sm text-slate-400">
                                    Không tìm thấy người dùng nào phù hợp với bộ lọc.
                                </td>
                            </tr>
                        ) : (
                            filteredUsers.map((user) => {
                                const isAdmin = user.roles?.includes("ROLE_ADMIN");
                                const isSuspended = user.status === "SUSPENDED";

                                return (
                                    <tr
                                        key={user.id}
                                        className="hover:bg-slate-50/60 dark:hover:bg-slate-800/30 transition-colors"
                                    >
                                        <td className="py-3.5 px-4 sm:px-6">
                                            <div className="flex items-center gap-3">
                                                <div className="w-9 h-9 rounded-full bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 flex items-center justify-center font-bold text-xs text-slate-700 dark:text-slate-200">
                                                    {user.fullName
                                                        ? user.fullName
                                                              .split(" ")
                                                              .map((n) => n[0])
                                                              .slice(0, 2)
                                                              .join("")
                                                              .toUpperCase()
                                                        : "U"}
                                                </div>
                                                <div>
                                                    <div className="font-semibold text-slate-900 dark:text-white leading-tight">
                                                        {user.fullName || "Chưa đặt tên"}
                                                    </div>
                                                    <div className="text-xs text-slate-500 dark:text-slate-400">
                                                        {user.email}
                                                    </div>
                                                </div>
                                            </div>
                                        </td>
                                        <td className="py-3.5 px-4 text-xs font-mono text-slate-600 dark:text-slate-300">
                                            {user.phoneNumber || "-"}
                                        </td>
                                        <td className="py-3.5 px-4">
                                            {isSuspended ? (
                                                <Badge variant="danger">Bị khóa</Badge>
                                            ) : (
                                                <Badge variant="success">Hoạt động</Badge>
                                            )}
                                        </td>
                                        <td className="py-3.5 px-4">
                                            <div className="flex flex-wrap gap-1">
                                                {user.roles?.map((r) => (
                                                    <span
                                                        key={r}
                                                        className={`px-2 py-0.5 rounded-md text-[10px] font-semibold tracking-wide ${
                                                            r === "ROLE_ADMIN"
                                                                ? "bg-indigo-100 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800"
                                                                : "bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300 border border-slate-200 dark:border-slate-700"
                                                        }`}
                                                    >
                                                        {r.replace("ROLE_", "")}
                                                    </span>
                                                ))}
                                            </div>
                                        </td>
                                        <td className="py-3.5 px-4 text-right">
                                            <div className="font-semibold text-slate-900 dark:text-white leading-tight">
                                                {formatCurrency(user.walletBalance)}
                                            </div>
                                            <div className="flex items-center justify-end mt-1">
                                                {user.walletStatus === "FROZEN" ? (
                                                    <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-semibold bg-cyan-100 text-cyan-800 dark:bg-cyan-950/70 dark:text-cyan-300 border border-cyan-200 dark:border-cyan-800">
                                                        <Snowflake className="w-2.5 h-2.5 animate-pulse" />
                                                        Đóng băng
                                                    </span>
                                                ) : user.walletStatus === "CLOSED" ? (
                                                    <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400">
                                                        Đã đóng
                                                    </span>
                                                ) : (
                                                    <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium text-emerald-600 dark:text-emerald-400">
                                                        Ví hoạt động
                                                    </span>
                                                )}
                                            </div>
                                        </td>
                                        <td className="py-3.5 px-4 text-xs text-slate-500 dark:text-slate-400">
                                            {formatDate(user.createdAt)}
                                        </td>
                                        <td className="py-3.5 px-4 sm:px-6 text-right">
                                            <div className="flex items-center justify-end gap-1.5">
                                                <button
                                                    type="button"
                                                    onClick={() => onAssignRoles(user)}
                                                    className="p-1.5 text-slate-600 dark:text-slate-300 hover:text-indigo-600 dark:hover:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-950/40 rounded-lg transition-colors cursor-pointer"
                                                    title="Phân quyền vai trò"
                                                >
                                                    <Shield className="w-4 h-4" />
                                                </button>
                                                {user.walletId && onReconcileWallet && (
                                                    <button
                                                        type="button"
                                                        onClick={() => onReconcileWallet(user)}
                                                        className="p-1.5 text-slate-500 hover:text-emerald-600 dark:hover:text-emerald-400 hover:bg-emerald-50 dark:hover:bg-emerald-950/40 rounded-lg transition-colors cursor-pointer"
                                                        title="Đối soát Sổ cái (Audit Ledger)"
                                                    >
                                                        <Scale className="w-4 h-4" />
                                                    </button>
                                                )}
                                                {user.walletId && user.walletStatus !== "CLOSED" && (
                                                    <button
                                                        type="button"
                                                        onClick={() => onToggleWalletFreeze(user)}
                                                        className={`p-1.5 rounded-lg transition-colors cursor-pointer ${
                                                            user.walletStatus === "FROZEN"
                                                                ? "text-cyan-600 dark:text-cyan-400 bg-cyan-50 dark:bg-cyan-950/40 hover:bg-cyan-100 dark:hover:bg-cyan-900/60"
                                                                : "text-slate-500 hover:text-cyan-600 dark:hover:text-cyan-400 hover:bg-cyan-50 dark:hover:bg-cyan-950/40"
                                                        }`}
                                                        title={
                                                            user.walletStatus === "FROZEN"
                                                                ? "Mở băng ví điện tử"
                                                                : "Đóng băng ví điện tử"
                                                        }
                                                    >
                                                        <Snowflake className="w-4 h-4" />
                                                    </button>
                                                )}
                                                <button
                                                    type="button"
                                                    onClick={() => onToggleStatus(user)}
                                                    className={`p-1.5 rounded-lg transition-colors cursor-pointer ${
                                                        isSuspended
                                                            ? "text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-950/40"
                                                            : "text-red-500 hover:bg-red-50 dark:hover:bg-red-950/40"
                                                    }`}
                                                    title={
                                                        isSuspended
                                                            ? "Mở khóa tài khoản"
                                                            : "Khóa tài khoản"
                                                    }
                                                >
                                                    {isSuspended ? (
                                                        <Unlock className="w-4 h-4" />
                                                    ) : (
                                                        <Lock className="w-4 h-4" />
                                                    )}
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })
                        )}
                    </tbody>
                </table>
            </div>

            {/* Table Footer */}
            <div className="py-3 px-4 sm:px-6 border-t border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
                <span>
                    Hiển thị <strong>{filteredUsers.length}</strong> trên tổng số{" "}
                    <strong>{users.length}</strong> người dùng
                </span>
                <span className="italic">Dữ liệu bảo mật quản trị viên nội bộ</span>
            </div>
        </div>
    );
}
