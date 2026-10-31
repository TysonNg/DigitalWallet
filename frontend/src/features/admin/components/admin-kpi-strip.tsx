"use client";

import { AdminStats } from "../types/admin.types";
import { Users, Wallet, ArrowLeftRight, CircleDollarSign } from "lucide-react";

interface AdminKpiStripProps {
    stats?: AdminStats | null;
    isPending?: boolean;
}

export function AdminKpiStrip({ stats, isPending }: AdminKpiStripProps) {
    const formatCurrency = (amount?: number) => {
        if (amount == null) return "0 ₫";
        return new Intl.NumberFormat("vi-VN", {
            style: "currency",
            currency: "VND",
            maximumFractionDigits: 0,
        }).format(amount);
    };

    const metrics = [
        {
            label: "Tổng người dùng",
            value: stats?.totalUsers?.toLocaleString("vi-VN") ?? "0",
            subtext: `${stats?.activeUsers ?? 0} đang hoạt động • ${stats?.suspendedUsers ?? 0} bị khóa`,
            icon: Users,
            color: "text-blue-600 bg-blue-50 dark:bg-blue-950/40 dark:text-blue-400",
        },
        {
            label: "Tổng ví điện tử",
            value: stats?.totalWallets?.toLocaleString("vi-VN") ?? "0",
            subtext: "Hệ thống ví số thực tế",
            icon: Wallet,
            color: "text-indigo-600 bg-indigo-50 dark:bg-indigo-950/40 dark:text-indigo-400",
        },
        {
            label: "Tổng thanh khoản hệ thống",
            value: formatCurrency(stats?.totalWalletBalance),
            subtext: "Số dư khả dụng toàn hệ thống",
            icon: CircleDollarSign,
            color: "text-emerald-600 bg-emerald-50 dark:bg-emerald-950/40 dark:text-emerald-400",
        },
        {
            label: "Tổng giao dịch thành công",
            value: stats?.totalTransactions?.toLocaleString("vi-VN") ?? "0",
            subtext: `Volume: ${formatCurrency(stats?.totalTransactionVolume)}`,
            icon: ArrowLeftRight,
            color: "text-amber-600 bg-amber-50 dark:bg-amber-950/40 dark:text-amber-400",
        },
    ];

    return (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {metrics.map((metric, idx) => {
                const Icon = metric.icon;
                return (
                    <div
                        key={idx}
                        className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-xl p-4 shadow-xs transition-all hover:shadow-md"
                    >
                        <div className="flex items-center justify-between">
                            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
                                {metric.label}
                            </span>
                            <div className={`p-2 rounded-lg ${metric.color}`}>
                                <Icon className="w-4 h-4" />
                            </div>
                        </div>
                        <div className="mt-2 flex items-baseline gap-2">
                            <span className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
                                {isPending ? "..." : metric.value}
                            </span>
                        </div>
                        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400 truncate">
                            {metric.subtext}
                        </p>
                    </div>
                );
            })}
        </div>
    );
}
