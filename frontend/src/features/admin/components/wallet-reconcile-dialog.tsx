"use client";

import { AdminUser, WalletReconciliation } from "../types/admin.types";
import { CheckCircle2, AlertTriangle, X, Scale, RefreshCw } from "lucide-react";

interface WalletReconcileDialogProps {
    user: AdminUser | null;
    isOpen: boolean;
    isPending: boolean;
    reconciliation?: WalletReconciliation | null;
    onClose: () => void;
    onReconcile: (walletId: string) => void;
}

export function WalletReconcileDialog({
    user,
    isOpen,
    isPending,
    reconciliation,
    onClose,
    onReconcile,
}: WalletReconcileDialogProps) {
    if (!isOpen || !user) return null;

    const formatCurrency = (amount?: number) => {
        if (amount == null) return "0 ₫";
        return new Intl.NumberFormat("vi-VN", {
            style: "currency",
            currency: "VND",
            maximumFractionDigits: 0,
        }).format(amount);
    };

    const isReconciled = reconciliation?.reconciled ?? false;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs">
            <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-lg max-w-md w-full overflow-hidden animate-in fade-in-0 zoom-in-95 duration-150">
                {/* Header */}
                <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100 dark:border-slate-800">
                    <div className="flex items-center gap-2.5">
                        <div className="p-1.5 rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
                            <Scale className="w-4 h-4" />
                        </div>
                        <h3 className="text-sm font-semibold text-slate-900 dark:text-white">
                            Đối soát Sổ cái Ví điện tử
                        </h3>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        className="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg transition-colors cursor-pointer"
                    >
                        <X className="w-4 h-4" />
                    </button>
                </div>

                {/* Body */}
                <div className="p-5 space-y-4">
                    {/* User Meta */}
                    <div className="bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800 rounded-lg p-3 text-xs space-y-1.5">
                        <div className="flex justify-between">
                            <span className="text-slate-500 dark:text-slate-400">Chủ tài khoản:</span>
                            <span className="font-medium text-slate-900 dark:text-white">{user.fullName}</span>
                        </div>
                        <div className="flex justify-between">
                            <span className="text-slate-500 dark:text-slate-400">Email:</span>
                            <span className="font-medium text-slate-900 dark:text-white">{user.email}</span>
                        </div>
                        <div className="flex justify-between">
                            <span className="text-slate-500 dark:text-slate-400">Mã ví (Wallet ID):</span>
                            <span className="font-mono text-slate-700 dark:text-slate-300 truncate max-w-[200px]">
                                {user.walletId}
                            </span>
                        </div>
                    </div>

                    {/* Balance Comparison */}
                    <div className="grid grid-cols-2 gap-3">
                        <div className="bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800 rounded-lg p-3">
                            <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400">
                                Số dư hiển thị (App)
                            </span>
                            <p className="text-base font-bold text-slate-900 dark:text-white mt-1">
                                {formatCurrency(user.walletBalance)}
                            </p>
                        </div>
                        <div className="bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800 rounded-lg p-3">
                            <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400">
                                Số dư Sổ cái lũy kế
                            </span>
                            <p className="text-base font-bold text-slate-900 dark:text-white mt-1">
                                {isPending ? "..." : formatCurrency(reconciliation?.walletBalance ?? user.walletBalance)}
                            </p>
                        </div>
                    </div>

                    {/* Result Banner */}
                    {isPending ? (
                        <div className="p-3 rounded-lg bg-slate-100 dark:bg-slate-800 text-xs text-slate-600 dark:text-slate-300 text-center">
                            Đang truy vấn các dòng bút toán Nợ/Có...
                        </div>
                    ) : isReconciled ? (
                        <div className="p-3.5 rounded-lg bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800/70 text-xs text-emerald-800 dark:text-emerald-300 space-y-1">
                            <div className="flex items-center gap-1.5 font-semibold">
                                <CheckCircle2 className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0" />
                                <span>Khớp 100% với Sổ cái kế toán</span>
                            </div>
                            <p className="text-[11px] text-emerald-700/80 dark:text-emerald-400/80 pl-5.5">
                                Mọi biến động số dư của ví này đều bắt nguồn từ các bút toán đối ứng hợp lệ. Không có tình trạng tự sinh tiền hay can thiệp CSDL bất hợp pháp.
                            </p>
                        </div>
                    ) : (
                        <div className="p-3.5 rounded-lg bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800/70 text-xs text-red-800 dark:text-red-300 space-y-1">
                            <div className="flex items-center gap-1.5 font-semibold">
                                <AlertTriangle className="w-4 h-4 text-red-600 dark:text-red-400 shrink-0" />
                                <span>Phát hiện sai lệch dữ liệu ví!</span>
                            </div>
                            <p className="text-[11px] text-red-700/80 dark:text-red-400/80 pl-5.5">
                                Số dư thực tế trong ví không khớp với tổng lũy kế các dòng bút toán trong sổ cái. Cần kiểm tra log giao dịch khẩn cấp.
                            </p>
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="flex items-center justify-end gap-2 px-5 py-3.5 bg-slate-50/70 dark:bg-slate-800/30 border-t border-slate-100 dark:border-slate-800">
                    {user.walletId && (
                        <button
                            type="button"
                            onClick={() => onReconcile(user.walletId!)}
                            disabled={isPending}
                            className="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-slate-700 dark:text-slate-300 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-700 transition-colors disabled:opacity-50 cursor-pointer"
                        >
                            <RefreshCw className={`w-3.5 h-3.5 ${isPending ? "animate-spin" : ""}`} />
                            Đối soát lại
                        </button>
                    )}
                    <button
                        type="button"
                        onClick={onClose}
                        className="px-3.5 py-1.5 text-xs font-medium text-white bg-slate-900 hover:bg-slate-800 dark:bg-slate-100 dark:text-slate-900 dark:hover:bg-white rounded-lg transition-colors cursor-pointer"
                    >
                        Đóng
                    </button>
                </div>
            </div>
        </div>
    );
}
