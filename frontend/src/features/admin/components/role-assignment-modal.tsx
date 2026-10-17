"use client";

import { useState, useEffect } from "react";
import { AdminUser } from "../types/admin.types";
import { Shield, ShieldCheck, Check, Info } from "lucide-react";

interface RoleAssignmentModalProps {
    user: AdminUser | null;
    isOpen: boolean;
    isPending: boolean;
    onClose: () => void;
    onSave: (userId: string, roles: string[]) => void;
}

const AVAILABLE_ROLES = [
    {
        name: "ROLE_USER",
        label: "Người dùng tiêu chuẩn (ROLE_USER)",
        description: "Quyền truy cập ví cá nhân, nạp tiền, rút tiền, chuyển khoản.",
        authorities: ["USER_READ", "WALLET_READ"],
    },
    {
        name: "ROLE_ADMIN",
        label: "Quản trị viên hệ thống (ROLE_ADMIN)",
        description: "Toàn quyền quản trị: Thống kê chỉ số, quản lý người dùng, khóa/mở khóa tài khoản.",
        authorities: [
            "USER_READ",
            "USER_SUSPEND",
            "USER_MANAGE_ROLE",
            "WALLET_READ",
            "WALLET_FREEZE",
            "SYSTEM_STATS",
        ],
    },
];

export function RoleAssignmentModal({
    user,
    isOpen,
    isPending,
    onClose,
    onSave,
}: RoleAssignmentModalProps) {
    const [selectedRoles, setSelectedRoles] = useState<string[]>([]);

    useEffect(() => {
        if (user) {
            setSelectedRoles(user.roles || ["ROLE_USER"]);
        }
    }, [user]);

    if (!isOpen || !user) return null;

    const toggleRole = (roleName: string) => {
        if (selectedRoles.includes(roleName)) {
            // Không cho phép bỏ chọn hết vai trò (phải giữ ít nhất 1 role)
            if (selectedRoles.length > 1) {
                setSelectedRoles(selectedRoles.filter((r) => r !== roleName));
            }
        } else {
            setSelectedRoles([...selectedRoles, roleName]);
        }
    };

    // Gom danh sách authority kế thừa từ các role được chọn
    const inheritedAuthorities = Array.from(
        new Set(
            AVAILABLE_ROLES.filter((r) => selectedRoles.includes(r.name)).flatMap(
                (r) => r.authorities
            )
        )
    );

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in duration-150">
            <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl max-w-lg w-full p-6 shadow-2xl">
                <div className="flex items-center gap-3">
                    <div className="p-3 rounded-xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-400">
                        <Shield className="w-6 h-6" />
                    </div>
                    <div>
                        <h3 className="text-lg font-bold text-slate-900 dark:text-white">
                            Phân quyền & Vai trò người dùng
                        </h3>
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                            {user.fullName} ({user.email})
                        </p>
                    </div>
                </div>

                <div className="mt-5 space-y-3">
                    <label className="text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider">
                        Danh sách Vai trò (Roles)
                    </label>
                    {AVAILABLE_ROLES.map((role) => {
                        const isChecked = selectedRoles.includes(role.name);
                        return (
                            <div
                                key={role.name}
                                onClick={() => toggleRole(role.name)}
                                className={`p-4 rounded-xl border cursor-pointer transition-all flex items-start gap-3 ${
                                    isChecked
                                        ? "border-indigo-500 bg-indigo-50/40 dark:bg-indigo-950/20 shadow-xs"
                                        : "border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700 bg-white dark:bg-slate-900"
                                }`}
                            >
                                <div
                                    className={`mt-0.5 w-5 h-5 rounded-md flex items-center justify-center border transition-colors ${
                                        isChecked
                                            ? "bg-indigo-600 border-indigo-600 text-white"
                                            : "border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800"
                                    }`}
                                >
                                    {isChecked && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                                </div>
                                <div className="flex-1">
                                    <div className="flex items-center gap-2">
                                        <span className="text-sm font-semibold text-slate-900 dark:text-white">
                                            {role.label}
                                        </span>
                                    </div>
                                    <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                                        {role.description}
                                    </p>
                                </div>
                            </div>
                        );
                    })}
                </div>

                {/* Preview Inherited Authorities */}
                <div className="mt-5 p-4 bg-slate-50 dark:bg-slate-800/60 rounded-xl border border-slate-200/60 dark:border-slate-700/50">
                    <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-700 dark:text-slate-300">
                        <ShieldCheck className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
                        <span>Quyền hạn hạt nhân được cấp ({inheritedAuthorities.length})</span>
                    </div>
                    <div className="mt-2.5 flex flex-wrap gap-1.5">
                        {inheritedAuthorities.map((auth) => (
                            <span
                                key={auth}
                                className="px-2 py-0.5 text-[11px] font-mono bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-md text-slate-700 dark:text-slate-300"
                            >
                                {auth}
                            </span>
                        ))}
                    </div>
                </div>

                <div className="mt-3 flex items-center gap-1.5 text-xs text-slate-400">
                    <Info className="w-3.5 h-3.5 shrink-0" />
                    <span>
                        Sau khi thay đổi, phiên làm việc hiện tại của người dùng sẽ tự động làm mới để cập
                        nhật JWT claims.
                    </span>
                </div>

                <div className="mt-6 flex items-center justify-end gap-3">
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={isPending}
                        className="px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 rounded-lg transition-colors cursor-pointer"
                    >
                        Hủy
                    </button>
                    <button
                        type="button"
                        onClick={() => onSave(user.id, selectedRoles)}
                        disabled={isPending}
                        className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition-colors cursor-pointer flex items-center gap-2"
                    >
                        {isPending ? "Đang lưu..." : "Lưu thay đổi"}
                    </button>
                </div>
            </div>
        </div>
    );
}
