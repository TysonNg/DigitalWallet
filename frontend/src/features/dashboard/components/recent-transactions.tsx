"use client";

import {
    ArrowDownLeft,
    ArrowUpRight,
    CreditCard,
    ArrowRight,
    SearchX,
} from "lucide-react";
import { formatVND } from "@/lib/utils";
import { TransactionDto } from "@/features/wallet/types/wallet.types";

interface RecentTransactionsProps {
    searchQuery?: string;
    realTransactions?: TransactionDto[];
}

interface DisplayTx {
    id: string;
    title: string;
    date: string;
    amount: number;
    isPositive: boolean;
    iconType: "receive" | "transfer" | "payment" | "withdraw" | "deposit";
    status: string;
}

const DEFAULT_TRANSACTIONS: DisplayTx[] = [
    {
        id: "tx-1",
        title: "Nhận tiền từ Trần Văn Nam",
        date: "Hôm nay, 14:32",
        amount: 500000,
        isPositive: true,
        iconType: "receive",
        status: "Thành công",
    },
    {
        id: "tx-2",
        title: "Chuyển sang ví tiết kiệm",
        date: "Hôm nay, 10:15",
        amount: -2000000,
        isPositive: false,
        iconType: "transfer",
        status: "Thành công",
    },
    {
        id: "tx-3",
        title: "Thanh toán Shopee",
        date: "Hôm qua, 20:18",
        amount: -320000,
        isPositive: false,
        iconType: "payment",
        status: "Thành công",
    },
    {
        id: "tx-4",
        title: "Rút tiền về ngân hàng",
        date: "22/04/2026, 16:05",
        amount: -1000000,
        isPositive: false,
        iconType: "withdraw",
        status: "Thành công",
    },
    {
        id: "tx-5",
        title: "Nạp tiền từ Vietcombank",
        date: "21/04/2026, 11:20",
        amount: 3000000,
        isPositive: true,
        iconType: "deposit",
        status: "Thành công",
    },
];

export function RecentTransactions({
    searchQuery = "",
    realTransactions = [],
}: RecentTransactionsProps) {
    const mappedReal: DisplayTx[] = realTransactions.map((tx) => {
        const isPos = tx.type === "DEPOSIT" || (tx.type === "TRANSFER" && tx.amount > 0);
        const iconType: DisplayTx["iconType"] =
            tx.type === "DEPOSIT" ? "deposit" : tx.type === "WITHDRAW" ? "withdraw" : "transfer";
        const dateStr = new Date(tx.createdAt).toLocaleString("vi-VN", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        });

        return {
            id: tx.id,
            title: tx.description || (tx.type === "DEPOSIT" ? "Nạp tiền vào ví" : tx.type === "WITHDRAW" ? "Rút tiền từ ví" : "Chuyển tiền ví"),
            date: dateStr,
            amount: isPos ? Math.abs(tx.amount) : -Math.abs(tx.amount),
            isPositive: isPos,
            iconType,
            status: tx.status === "SUCCESS" ? "Thành công" : tx.status,
        };
    });

    const allTransactions = mappedReal.length > 0 ? [...mappedReal, ...DEFAULT_TRANSACTIONS] : DEFAULT_TRANSACTIONS;

    const filtered = allTransactions.filter((tx) =>
        tx.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        tx.date.toLowerCase().includes(searchQuery.toLowerCase()) ||
        tx.amount.toString().includes(searchQuery)
    );

    const getIcon = (type: DisplayTx["iconType"]) => {
        switch (type) {
            case "receive":
            case "deposit":
                return (
                    <div className="w-7 h-7 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
                        <ArrowDownLeft className="w-3.5 h-3.5 stroke-[1.75]" />
                    </div>
                );
            case "transfer":
                return (
                    <div className="w-7 h-7 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
                        <ArrowUpRight className="w-3.5 h-3.5 stroke-[1.75]" />
                    </div>
                );
            case "withdraw":
                return (
                    <div className="w-7 h-7 rounded-full bg-rose-50 text-rose-500 flex items-center justify-center shrink-0">
                        <ArrowUpRight className="w-3.5 h-3.5 stroke-[1.75]" />
                    </div>
                );
            case "payment":
                return (
                    <div className="w-7 h-7 rounded-full bg-purple-50 text-purple-600 flex items-center justify-center shrink-0">
                        <CreditCard className="w-3.5 h-3.5 stroke-[1.75]" />
                    </div>
                );
        }
    };

    return (
        <div id="transactions-section" className="bg-white rounded-2xl border border-slate-200/90 p-5 sm:p-6 shadow-xs flex flex-col justify-between">
            <div className="flex items-center justify-between mb-4">
                <h3 className="font-bold text-base text-slate-900 tracking-tight">
                    Giao dịch gần đây
                </h3>
                <button
                    type="button"
                    className="inline-flex items-center text-xs font-semibold text-blue-600 hover:text-blue-700 hover:underline cursor-pointer"
                >
                    Xem tất cả <ArrowRight className="w-3.5 h-3.5 ml-1" />
                </button>
            </div>

            {filtered.length === 0 ? (
                <div className="py-10 flex flex-col items-center justify-center text-slate-400 text-center">
                    <SearchX className="w-7 h-7 mb-2 stroke-[1.5]" />
                    <p className="text-xs">Không tìm thấy giao dịch nào phù hợp</p>
                </div>
            ) : (
                <div className="divide-y divide-slate-100">
                    {filtered.slice(0, 5).map((tx) => (
                        <div
                            key={tx.id}
                            className="py-2.5 flex items-center justify-between gap-3 hover:bg-slate-50/50 rounded-lg px-1 transition-colors"
                        >
                            <div className="flex items-center space-x-3 min-w-0">
                                {getIcon(tx.iconType)}
                                <div className="min-w-0">
                                    <p className="text-xs font-semibold text-slate-800 truncate">
                                        {tx.title}
                                    </p>
                                    <p className="text-[11px] text-slate-400 mt-0.5">
                                        {tx.date}
                                    </p>
                                </div>
                            </div>

                            <div className="flex items-center space-x-3 shrink-0">
                                <span
                                    className={`text-xs font-semibold ${
                                        tx.isPositive
                                            ? "text-emerald-600"
                                            : "text-slate-900"
                                    }`}
                                >
                                    {tx.isPositive ? "+" : ""}
                                    {formatVND(tx.amount)}
                                </span>
                                <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-medium bg-emerald-50 text-emerald-700 border border-emerald-100/70">
                                    {tx.status}
                                </span>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
