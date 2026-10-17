"use client";

import { AdminUser } from "../types/admin.types";
import { AlertCircle, Snowflake, Sun } from "lucide-react";

interface ConfirmFreezeDialogProps {
    user: AdminUser | null;
    isOpen: boolean;
    isPending: boolean;
    onClose: () => void;
    onConfirm: (walletId: string) => void;
}

export function ConfirmFreezeDialog({
    user,
    isOpen,
    isPending,
    onClose,
    onConfirm,
}: ConfirmFreezeDialogProps) {
    if (!isOpen || !user || !user.walletId) return null;

    const isFreezing = user.walletStatus !== "FROZEN";

    const formatCurrency = (amount?: number) => {
        if (amount == null) return "0 ₫";
        return new Intl.NumberFormat("vi-VN", {
            style: "currency",
            currency: "VND",
            maximumFractionDigits: 0,
        }).format(amount);
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in duration-150">
            <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl max-w-md w-full p-6 shadow-2xl">
                <div className="flex items-center gap-3">
                    <div
                        className={`p-3 rounded-xl ${
                            isFreezing
                                ? "bg-cyan-50 text-cyan-600 dark:bg-cyan-950/40 dark:text-cyan-400"
                                : "bg-emerald-50 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-400"
                        }`}
                    >
                        {isFreezing ? <Snowflake className="w-6 h-6 animate-pulse" /> : <Sun className="w-6 h-6" />}
                    </div>
                    <div>
                        <h3 className="text-lg font-bold text-slate-900 dark:text-white">
                            {isFreezing ? "Xác nhận đóng băng ví" : "Xác nhận mở băng ví"}
                        </h3>
                        <p className="text-xs text-slate-500 dark:text-slate-400">{user.email}</p>
                    </div>
                </div>

                {/* Wallet Details summary */}
                <div className="mt-4 p-3.5 bg-slate-50 dark:bg-slate-800/60 rounded-xl border border-slate-200/80 dark:border-slate-800 space-y-1.5 text-xs">
                    <div className="flex justify-between items-center">
                        <span className="text-slate-500 dark:text-slate-400">Chủ ví:</span>
                        <span className="font-semibold text-slate-900 dark:text-white">{user.fullName || "Chưa đặt tên"}</span>
                    </div>
                    <div className="flex justify-between items-center">
                        <span className="text-slate-500 dark:text-slate-400">Mã ví:</span>
                        <span className="font-mono text-slate-700 dark:text-slate-300">{user.walletId.slice(0, 18)}...</span>
                    </div>
                    <div className="flex justify-between items-center">
                        <span className="text-slate-500 dark:text-slate-400">Số dư ví:</span>
                        <span className="font-bold text-slate-900 dark:text-white">{formatCurrency(user.walletBalance)}</span>
                    </div>
                </div>

                <div
                    className={`mt-4 p-3 rounded-xl border text-xs flex items-start gap-2.5 ${
                        isFreezing
                            ? "bg-cyan-50/70 dark:bg-cyan-950/30 border-cyan-200/70 dark:border-cyan-900/50 text-cyan-900 dark:text-cyan-300"
                            : "bg-emerald-50/70 dark:bg-emerald-950/30 border-emerald-200/70 dark:border-emerald-900/50 text-emerald-900 dark:text-emerald-300"
                    }`}
                >
                    <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                    <div>
                        {isFreezing ? (
                            <span>
                                Khi bị đóng băng, chủ ví <strong>KHÔNG THỂ</strong> nạp tiền, rút tiền hoặc chuyển tiền từ ví này.
                                Tuy nhiên người dùng vẫn có thể đăng nhập tra cứu lịch sử giao dịch.
                            </span>
                        ) : (
                            <span>
                                Mở băng ví sẽ khôi phục toàn bộ quyền giao dịch. Người dùng có thể tiếp tục nạp, rút và chuyển tiền bình thường.
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
                        onClick={() => onConfirm(user.walletId!)}
                        disabled={isPending}
                        className={`px-4 py-2 text-sm font-medium text-white rounded-lg transition-colors cursor-pointer flex items-center gap-2 ${
                            isFreezing
                                ? "bg-cyan-600 hover:bg-cyan-700"
                                : "bg-emerald-600 hover:bg-emerald-700"
                        }`}
                    >
                        {isPending
                            ? "Đang xử lý..."
                            : isFreezing
                              ? "Xác nhận đóng băng"
                              : "Xác nhận mở băng"}
                    </button>
                </div>
            </div>
        </div>
    );
}
