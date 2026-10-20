"use client";

import { TrialBalance } from "../types/admin.types";
import { BookOpenCheck, CheckCircle2, AlertTriangle, RefreshCw } from "lucide-react";

interface LedgerTrialBalanceCardProps {
    data?: TrialBalance | null;
    isPending?: boolean;
    isRefetching?: boolean;
    onRefresh: () => void;
}

export function LedgerTrialBalanceCard({
    data,
    isPending,
    isRefetching,
    onRefresh,
}: LedgerTrialBalanceCardProps) {
    const formatCurrency = (amount?: number) => {
        if (amount == null) return "0 ₫";
        return new Intl.NumberFormat("vi-VN", {
            style: "currency",
            currency: "VND",
            maximumFractionDigits: 0,
        }).format(amount);
    };

    const isBalanced = data?.balanced ?? true;

    return (
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-xs">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-100 dark:border-slate-800">
                <div className="flex items-center gap-3">
                    <div className="p-2 rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
                        <BookOpenCheck className="w-5 h-5" />
                    </div>
                    <div>
                        <div className="flex items-center gap-2">
                            <h3 className="text-sm font-semibold text-slate-900 dark:text-white">
                                Sổ cái ghi kép & Bảng cân đối thử (Double-Entry Ledger)
                            </h3>
                            {isPending ? (
                                <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400">
                                    Đang kiểm tra...
                                </span>
                            ) : isBalanced ? (
                                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-medium bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800/80">
                                    <CheckCircle2 className="w-3.5 h-3.5" />
                                    Cân bằng tuyệt đối (Zero-Sum)
                                </span>
                            ) : (
                                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-medium bg-red-50 text-red-700 dark:bg-red-950/60 dark:text-red-400 border border-red-200 dark:border-red-800/80">
                                    <AlertTriangle className="w-3.5 h-3.5" />
                                    Cảnh báo lệch sổ cái ({formatCurrency(data?.difference)})
                                </span>
                            )}
                        </div>
                        <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                            Tuân thủ nguyên tắc kế toán tài chính: Tổng Nợ (Debit) luôn bằng Tổng Có (Credit)
                        </p>
                    </div>
                </div>

                <button
                    type="button"
                    onClick={onRefresh}
                    disabled={isPending || isRefetching}
                    className="self-start sm:self-auto inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-slate-700 dark:text-slate-300 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 rounded-lg transition-colors disabled:opacity-50 cursor-pointer"
                    title="Đối soát lại sổ cái"
                >
                    <RefreshCw className={`w-3.5 h-3.5 ${isRefetching ? "animate-spin" : ""}`} />
                    Đối soát lại
                </button>
            </div>

            {/* Metrics Breakdown */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-4">
                <div className="bg-slate-50/70 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800 rounded-lg p-3.5">
                    <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
                        Tổng phát sinh Nợ (Debit)
                    </span>
                    <p className="text-lg font-bold text-slate-900 dark:text-white mt-1">
                        {isPending ? "..." : formatCurrency(data?.totalDebit)}
                    </p>
                    <span className="text-[11px] text-slate-400 dark:text-slate-500">
                        Tăng tài sản / Giảm nợ ví
                    </span>
                </div>

                <div className="bg-slate-50/70 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800 rounded-lg p-3.5">
                    <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
                        Tổng phát sinh Có (Credit)
                    </span>
                    <p className="text-lg font-bold text-slate-900 dark:text-white mt-1">
                        {isPending ? "..." : formatCurrency(data?.totalCredit)}
                    </p>
                    <span className="text-[11px] text-slate-400 dark:text-slate-500">
                        Tăng nợ ví / Tăng doanh thu
                    </span>
                </div>

                <div className="bg-slate-50/70 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800 rounded-lg p-3.5">
                    <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
                        Tổng số dòng bút toán
                    </span>
                    <p className="text-lg font-bold text-slate-900 dark:text-white mt-1">
                        {isPending ? "..." : (data?.totalEntries?.toLocaleString("vi-VN") ?? "0")} dòng
                    </p>
                    <span className="text-[11px] text-slate-400 dark:text-slate-500">
                        Lưu trữ dạng Append-Only
                    </span>
                </div>
            </div>
        </div>
    );
}
