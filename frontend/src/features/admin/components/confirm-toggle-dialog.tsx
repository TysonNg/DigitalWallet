"use client";

import { AdminUser } from "../types/admin.types";
import { AlertTriangle, Lock, Unlock } from "lucide-react";

interface ConfirmToggleDialogProps {
    user: AdminUser | null;
    isOpen: boolean;
    isPending: boolean;
    onClose: () => void;
    onConfirm: (userId: string) => void;
}

export function ConfirmToggleDialog({
    user,
    isOpen,
    isPending,
    onClose,
    onConfirm,
}: ConfirmToggleDialogProps) {
    if (!isOpen || !user) return null;

    const isSuspending = user.status === "ACTIVE";

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in duration-150">
            <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl max-w-md w-full p-6 shadow-2xl">
                <div className="flex items-center gap-3">
                    <div
                        className={`p-3 rounded-xl ${
                            isSuspending
                                ? "bg-red-50 text-red-600 dark:bg-red-950/40 dark:text-red-400"
                                : "bg-emerald-50 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-400"
                        }`}
                    >
                        {isSuspending ? <Lock className="w-6 h-6" /> : <Unlock className="w-6 h-6" />}
                    </div>
                    <div>
                        <h3 className="text-lg font-bold text-slate-900 dark:text-white">
                            {isSuspending ? "Xác nhận khóa tài khoản" : "Mở khóa tài khoản"}
                        </h3>
                        <p className="text-xs text-slate-500 dark:text-slate-400">{user.email}</p>
                    </div>
                </div>

                <div className="mt-4 p-3 bg-amber-50 dark:bg-amber-950/30 border border-amber-200/60 dark:border-amber-900/50 rounded-xl text-xs text-amber-800 dark:text-amber-300 flex items-start gap-2">
                    <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                    <div>
                        {isSuspending ? (
                            <span>
                                Khi bị khóa, toàn bộ phiên đăng nhập (Active Session) và Refresh Token của{" "}
                                <strong>{user.fullName}</strong> trong Redis sẽ bị thu hồi ngay lập tức. Người
                                dùng không thể nạp, rút hoặc chuyển tiền.
                            </span>
                        ) : (
                            <span>
                                Mở khóa tài khoản sẽ cho phép <strong>{user.fullName}</strong> đăng nhập và
                                tiếp tục thực hiện giao dịch bình thường.
                            </span>
                        )}
                    </div>
                </div>

                <div className="mt-6 flex items-center justify-end gap-3">
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={isPending}
                        className="px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 rounded-lg transition-colors cursor-pointer"
                    >
                        Hủy bỏ
                    </button>
                    <button
                        type="button"
                        onClick={() => onConfirm(user.id)}
                        disabled={isPending}
                        className={`px-4 py-2 text-sm font-medium text-white rounded-lg transition-colors cursor-pointer flex items-center gap-2 ${
                            isSuspending
                                ? "bg-red-600 hover:bg-red-700"
                                : "bg-emerald-600 hover:bg-emerald-700"
                        }`}
                    >
                        {isPending
                            ? "Đang xử lý..."
                            : isSuspending
                              ? "Xác nhận khóa"
                              : "Xác nhận mở khóa"}
                    </button>
                </div>
            </div>
        </div>
    );
}
