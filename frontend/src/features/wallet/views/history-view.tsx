"use client";

import { useTranslations, useLocale } from "next-intl";
import { useState, useMemo } from "react";
import { useSearchParams } from "next/navigation";
import {
    Clock,
    ArrowDownLeft,
    ArrowUpRight,
    Search,
    X,
    Filter,
    Calendar,
    ChevronDown,
    ChevronUp,
    Copy,
    Check,
    CreditCard,
    FileText,
    Loader2,
} from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useMyWalletQuery, useTransactionsQuery } from "../hooks/use-wallet";
import { TransactionDto } from "../types/wallet.types";
import { toast } from "sonner";

type FilterType = "ALL" | "DEPOSIT" | "WITHDRAW" | "TRANSFER";

interface TimelineGroup {
    groupKey: string;
    groupLabel: string;
    items: TransactionDto[];
}

export function HistoryView() {
    const t = useTranslations("dashboard");
    const walletT = useTranslations("wallet");
    const common = useTranslations("common");
    const locale = useLocale() === "vi" ? "vi-VN" : "en-US";
    const searchParams = useSearchParams();

    const { data: wallet, isLoading: isWalletLoading } = useMyWalletQuery();
    const { data: transactions = [], isLoading: isTxLoading } = useTransactionsQuery(wallet?.id);

    const currency = wallet?.currency ?? "VND";
    const formatVND = (value: number) =>
        new Intl.NumberFormat(locale, { style: "currency", currency }).format(value);

    // Filter states
    const [filterType, setFilterType] = useState<FilterType>("ALL");
    const [searchQuery, setSearchQuery] = useState<string>(searchParams.get("search") || "");
    const [expandedTxId, setExpandedTxId] = useState<string | null>(null);
    const [copiedId, setCopiedId] = useState<string | null>(null);

    // Fallback mock transactions if real list is empty
    const fallbackList: TransactionDto[] = useMemo(() => {
        if (transactions.length > 0) return transactions;
        const now = new Date();
        const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);
        const threeDaysAgo = new Date(now.getTime() - 3 * 24 * 60 * 60 * 1000);

        return [
            {
                id: "tx-mock-1",
                walletId: wallet?.id || "w-1",
                amount: 500000,
                type: "DEPOSIT",
                status: "SUCCESS",
                description: "Nạp tiền qua PayOS VietQR",
                createdAt: now.toISOString(),
            },
            {
                id: "tx-mock-2",
                walletId: wallet?.id || "w-1",
                amount: 200000,
                type: "TRANSFER",
                status: "SUCCESS",
                description: "Chuyển tiền cho Tran Van Nam",
                createdAt: new Date(now.getTime() - 2 * 60 * 60 * 1000).toISOString(),
            },
            {
                id: "tx-mock-3",
                walletId: wallet?.id || "w-1",
                amount: 1000000,
                type: "WITHDRAW",
                status: "SUCCESS",
                description: "Rút tiền về MB Bank",
                createdAt: yesterday.toISOString(),
            },
            {
                id: "tx-mock-4",
                walletId: wallet?.id || "w-1",
                amount: 350000,
                type: "TRANSFER",
                status: "SUCCESS",
                description: "Thanh toán hóa đơn ăn uống",
                createdAt: threeDaysAgo.toISOString(),
            },
        ];
    }, [transactions, wallet?.id]);

    // Filter transactions
    const filteredTransactions = useMemo(() => {
        return fallbackList.filter((tx) => {
            // Type filter
            if (filterType !== "ALL" && tx.type !== filterType) {
                return false;
            }

            // Search query filter
            if (searchQuery.trim()) {
                const query = searchQuery.toLowerCase().trim();
                const desc = (tx.description || "").toLowerCase();
                const id = tx.id.toLowerCase();
                const amt = tx.amount.toString();
                if (!desc.includes(query) && !id.includes(query) && !amt.includes(query)) {
                    return false;
                }
            }

            return true;
        });
    }, [fallbackList, filterType, searchQuery]);

    // Group transactions by date timeline (Hôm nay, Hôm qua, Tháng này, Cũ hơn)
    const groupedTransactions: TimelineGroup[] = useMemo(() => {
        const now = new Date();
        const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
        const startOfYesterday = startOfToday - 24 * 60 * 60 * 1000;
        const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1).getTime();

        const todayItems: TransactionDto[] = [];
        const yesterdayItems: TransactionDto[] = [];
        const thisMonthItems: TransactionDto[] = [];
        const olderItems: TransactionDto[] = [];

        filteredTransactions.forEach((tx) => {
            const txTime = new Date(tx.createdAt).getTime();
            if (txTime >= startOfToday) {
                todayItems.push(tx);
            } else if (txTime >= startOfYesterday) {
                yesterdayItems.push(tx);
            } else if (txTime >= startOfMonth) {
                thisMonthItems.push(tx);
            } else {
                olderItems.push(tx);
            }
        });

        const groups: TimelineGroup[] = [];
        if (todayItems.length > 0) {
            groups.push({ groupKey: "today", groupLabel: "Hôm nay", items: todayItems });
        }
        if (yesterdayItems.length > 0) {
            groups.push({ groupKey: "yesterday", groupLabel: "Hôm qua", items: yesterdayItems });
        }
        if (thisMonthItems.length > 0) {
            groups.push({ groupKey: "thisMonth", groupLabel: "Tháng này", items: thisMonthItems });
        }
        if (olderItems.length > 0) {
            groups.push({ groupKey: "older", groupLabel: "Trước đó", items: olderItems });
        }

        return groups;
    }, [filteredTransactions]);

    const formatTimeOnly = (dateStr: string) => {
        try {
            return new Date(dateStr).toLocaleTimeString(locale, {
                timeZone: "Asia/Ho_Chi_Minh",
                hour: "2-digit",
                minute: "2-digit",
            });
        } catch {
            return "";
        }
    };

    const formatDateFull = (dateStr: string) => {
        try {
            return new Date(dateStr).toLocaleString(locale, {
                timeZone: "Asia/Ho_Chi_Minh",
                day: "2-digit",
                month: "2-digit",
                year: "numeric",
                hour: "2-digit",
                minute: "2-digit",
            });
        } catch {
            return dateStr;
        }
    };

    const handleCopyId = (id: string, e: React.MouseEvent) => {
        e.stopPropagation();
        navigator.clipboard.writeText(id);
        setCopiedId(id);
        toast.success("Đã sao chép mã giao dịch");
        setTimeout(() => setCopiedId(null), 2000);
    };

    return (
        <main className="flex-1 p-4 sm:p-8 max-w-4xl w-full mx-auto space-y-6">
            {/* Header */}
            <div>
                <h1 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
                    {t("transactionHistory")}
                </h1>
                <p className="text-xs sm:text-sm text-slate-500 mt-1">
                    Xem dòng thời gian thu chi và lịch sử mọi biến động số dư trong ví
                </p>
            </div>

            {/* Filter and Search Bar */}
            <div className="bg-white rounded-2xl border border-slate-200/90 p-4 shadow-xs space-y-3">
                {/* Search Input */}
                <div className="relative">
                    <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <Input
                        placeholder="Tìm theo nội dung, số tiền hoặc mã giao dịch..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="pl-9 pr-8 text-xs h-9.5 rounded-xl"
                    />
                    {searchQuery && (
                        <button
                            onClick={() => setSearchQuery("")}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 cursor-pointer"
                        >
                            <X className="w-3.5 h-3.5" />
                        </button>
                    )}
                </div>

                {/* Filter Chips */}
                <div className="flex flex-wrap items-center gap-1.5 pt-1">
                    <button
                        type="button"
                        onClick={() => setFilterType("ALL")}
                        className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                            filterType === "ALL"
                                ? "bg-blue-600 text-white shadow-xs"
                                : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                        }`}
                    >
                        Tất cả
                    </button>
                    <button
                        type="button"
                        onClick={() => setFilterType("DEPOSIT")}
                        className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                            filterType === "DEPOSIT"
                                ? "bg-emerald-600 text-white shadow-xs"
                                : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                        }`}
                    >
                        Nạp tiền
                    </button>
                    <button
                        type="button"
                        onClick={() => setFilterType("WITHDRAW")}
                        className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                            filterType === "WITHDRAW"
                                ? "bg-rose-600 text-white shadow-xs"
                                : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                        }`}
                    >
                        Rút tiền
                    </button>
                    <button
                        type="button"
                        onClick={() => setFilterType("TRANSFER")}
                        className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                            filterType === "TRANSFER"
                                ? "bg-blue-600 text-white shadow-xs"
                                : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                        }`}
                    >
                        Chuyển tiền
                    </button>
                </div>
            </div>

            {/* Timeline List */}
            {isTxLoading || isWalletLoading ? (
                <div className="py-20 text-center text-xs text-slate-400 flex flex-col items-center justify-center gap-2">
                    <Loader2 className="w-6 h-6 animate-spin text-blue-600" />
                    <span>Đang tải lịch sử giao dịch...</span>
                </div>
            ) : groupedTransactions.length === 0 ? (
                <div className="py-20 bg-white rounded-2xl border border-slate-200/90 text-center p-8 space-y-3 shadow-xs">
                    <div className="w-14 h-14 rounded-2xl bg-slate-50 text-slate-400 flex items-center justify-center mx-auto">
                        <Clock className="w-7 h-7 stroke-[1.5]" />
                    </div>
                    <div>
                        <h3 className="text-sm font-bold text-slate-900">Không có giao dịch nào phù hợp</h3>
                        <p className="text-xs text-slate-500 mt-1 max-w-md mx-auto">
                            Hãy thử đổi bộ lọc hoặc từ khóa tìm kiếm để xem các giao dịch khác.
                        </p>
                    </div>
                    {(searchQuery || filterType !== "ALL") && (
                        <Button
                            onClick={() => {
                                setSearchQuery("");
                                setFilterType("ALL");
                            }}
                            variant="outline"
                            size="sm"
                            className="cursor-pointer"
                        >
                            Đặt lại bộ lọc
                        </Button>
                    )}
                </div>
            ) : (
                <div className="space-y-6">
                    {groupedTransactions.map((group) => (
                        <div key={group.groupKey} className="space-y-2.5">
                            {/* Group Date Header */}
                            <div className="flex items-center space-x-2 px-1">
                                <span className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                                    {group.groupLabel}
                                </span>
                                <span className="text-[10px] text-slate-400 font-semibold bg-slate-100 px-2 py-0.5 rounded-full">
                                    {group.items.length}
                                </span>
                            </div>

                            {/* Group Card Container */}
                            <div className="bg-white rounded-2xl border border-slate-200/90 shadow-xs divide-y divide-slate-100 overflow-hidden">
                                {group.items.map((tx) => {
                                    const isDeposit = tx.type === "DEPOSIT";
                                    const isPositive = isDeposit;
                                    const isExpanded = expandedTxId === tx.id;

                                    return (
                                        <div
                                            key={tx.id}
                                            onClick={() => setExpandedTxId(isExpanded ? null : tx.id)}
                                            className="p-4 hover:bg-slate-50/80 transition-colors cursor-pointer"
                                        >
                                            <div className="flex items-center justify-between gap-3">
                                                {/* Left Icon & Description */}
                                                <div className="flex items-center space-x-3 min-w-0">
                                                    <div
                                                        className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
                                                            isDeposit
                                                                ? "bg-emerald-50 text-emerald-600"
                                                                : tx.type === "WITHDRAW"
                                                                ? "bg-rose-50 text-rose-600"
                                                                : "bg-blue-50 text-blue-600"
                                                        }`}
                                                    >
                                                        {isPositive ? (
                                                            <ArrowDownLeft className="w-5 h-5 stroke-[2]" />
                                                        ) : (
                                                            <ArrowUpRight className="w-5 h-5 stroke-[2]" />
                                                        )}
                                                    </div>
                                                    <div className="min-w-0">
                                                        <p className="text-xs font-bold text-slate-900 truncate">
                                                            {tx.description ||
                                                                (isDeposit
                                                                    ? "Nạp tiền vào ví"
                                                                    : tx.type === "WITHDRAW"
                                                                    ? "Rút tiền về ngân hàng"
                                                                    : "Chuyển tiền")}
                                                        </p>
                                                        <p className="text-[11px] text-slate-400 mt-0.5">
                                                            {formatTimeOnly(tx.createdAt)}
                                                        </p>
                                                    </div>
                                                </div>

                                                {/* Right Amount & Status */}
                                                <div className="text-right shrink-0">
                                                    <p
                                                        className={`text-xs sm:text-sm font-bold tracking-tight ${
                                                            isPositive ? "text-emerald-600" : "text-slate-900"
                                                        }`}
                                                    >
                                                        {isPositive ? "+" : "-"}
                                                        {formatVND(Math.abs(tx.amount))}
                                                    </p>
                                                    <span className="inline-block mt-0.5 text-[10px] text-emerald-700 bg-emerald-50 px-1.5 py-0.2 rounded font-medium border border-emerald-100">
                                                        {t("successful")}
                                                    </span>
                                                </div>
                                            </div>

                                            {/* Expandable Details Receipt */}
                                            {isExpanded && (
                                                <div className="mt-3 pt-3 border-t border-slate-100 text-xs text-slate-500 space-y-2 bg-slate-50/70 p-3 rounded-xl animate-in fade-in duration-100">
                                                    <div className="flex justify-between items-center">
                                                        <span>Mã giao dịch:</span>
                                                        <div className="flex items-center space-x-1.5 font-mono text-[11px] text-slate-800">
                                                            <span>{tx.id}</span>
                                                            <button
                                                                onClick={(e) => handleCopyId(tx.id, e)}
                                                                className="text-slate-400 hover:text-blue-600 p-0.5 cursor-pointer"
                                                                title="Sao chép mã"
                                                            >
                                                                {copiedId === tx.id ? (
                                                                    <Check className="w-3 h-3 text-emerald-600" />
                                                                ) : (
                                                                    <Copy className="w-3 h-3" />
                                                                )}
                                                            </button>
                                                        </div>
                                                    </div>
                                                    <div className="flex justify-between">
                                                        <span>Thời gian đầy đủ:</span>
                                                        <span className="font-medium text-slate-800">
                                                            {formatDateFull(tx.createdAt)}
                                                        </span>
                                                    </div>
                                                    <div className="flex justify-between">
                                                        <span>Hình thức:</span>
                                                        <span className="font-semibold text-slate-800">
                                                            {isDeposit
                                                                ? "Nạp tiền (PayOS / QR)"
                                                                : tx.type === "WITHDRAW"
                                                                ? "Rút tiền ngân hàng"
                                                                : "Chuyển khoản nội bộ"}
                                                        </span>
                                                    </div>
                                                </div>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </main>
    );
}
