"use client";

import { useTranslations, useLocale } from "next-intl";

import { ArrowDownLeft, ArrowUpRight, CreditCard, ArrowRight, SearchX } from "lucide-react";
import { TransactionDto } from "@/features/wallet/types/wallet.types";

interface RecentTransactionsProps {
    currency?: string;
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

export function RecentTransactions({
    currency = "VND",
    searchQuery = "",
    realTransactions = [],
}: RecentTransactionsProps) {
    const t = useTranslations("dashboard");
    const locale = useLocale() === "vi" ? "vi-VN" : "en-US";
    const formatVND = (value: number) =>
        new Intl.NumberFormat(locale, { style: "currency", currency }).format(value);

    const DEFAULT_TRANSACTIONS: DisplayTx[] = [
        {
            id: "tx-1",
            title: t("receivedFromTrnVnNam"),
            date: t("todayAt", {
                time: new Intl.DateTimeFormat(locale, {
                    hour: "2-digit",
                    minute: "2-digit",
                    timeZone: "Asia/Ho_Chi_Minh",
                }).format(new Date(Date.UTC(2026, 3, 24, 7, 32))),
            }),
            amount: 500000,
            isPositive: true,
            iconType: "receive",
            status: t("successful"),
        },
        {
            id: "tx-2",
            title: t("transferredToSavingsWallet"),
            date: t("todayAt", {
                time: new Intl.DateTimeFormat(locale, {
                    hour: "2-digit",
                    minute: "2-digit",
                    timeZone: "Asia/Ho_Chi_Minh",
                }).format(new Date(Date.UTC(2026, 3, 24, 3, 15))),
            }),
            amount: -2000000,
            isPositive: false,
            iconType: "transfer",
            status: t("successful"),
        },
        {
            id: "tx-3",
            title: t("shopeePayment"),
            date: t("yesterdayAt", {
                time: new Intl.DateTimeFormat(locale, {
                    hour: "2-digit",
                    minute: "2-digit",
                    timeZone: "Asia/Ho_Chi_Minh",
                }).format(new Date(Date.UTC(2026, 3, 24, 13, 18))),
            }),
            amount: -320000,
            isPositive: false,
            iconType: "payment",
            status: t("successful"),
        },
        {
            id: "tx-4",
            title: t("withdrawToBank"),
            date: new Date("2026-04-22T16:05:00+07:00").toLocaleString(locale, {
                timeZone: "Asia/Ho_Chi_Minh",
            }),
            amount: -1000000,
            isPositive: false,
            iconType: "withdraw",
            status: t("successful"),
        },
        {
            id: "tx-5",
            title: t("depositFromVietcombank"),
            date: new Date("2026-04-21T11:20:00+07:00").toLocaleString(locale, {
                timeZone: "Asia/Ho_Chi_Minh",
            }),
            amount: 3000000,
            isPositive: true,
            iconType: "deposit",
            status: t("successful"),
        },
    ];

    const mappedReal: DisplayTx[] = realTransactions.map((tx) => {
        const isPos = tx.type === "DEPOSIT" || (tx.type === "TRANSFER" && tx.amount > 0);
        const iconType: DisplayTx["iconType"] =
            tx.type === "DEPOSIT" ? "deposit" : tx.type === "WITHDRAW" ? "withdraw" : "transfer";
        const dateStr = new Date(tx.createdAt).toLocaleString(locale, {
            timeZone: "Asia/Ho_Chi_Minh",
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        });

        return {
            id: tx.id,
            title:
                tx.description ||
                (tx.type === "DEPOSIT"
                    ? t("depositIntoWallet")
                    : tx.type === "WITHDRAW"
                      ? t("withdrawFromWallet")
                      : t("walletTransfer")),
            date: dateStr,
            amount: isPos ? Math.abs(tx.amount) : -Math.abs(tx.amount),
            isPositive: isPos,
            iconType,
            status:
                tx.status === "SUCCESS"
                    ? t("successful")
                    : tx.status === "PENDING"
                      ? t("pending")
                      : t("failed"),
        };
    });

    const allTransactions =
        mappedReal.length > 0 ? [...mappedReal, ...DEFAULT_TRANSACTIONS] : DEFAULT_TRANSACTIONS;

    const filtered = allTransactions.filter(
        (tx) =>
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
        <div
            id="transactions-section"
            className="bg-white rounded-2xl border border-slate-200/90 p-5 sm:p-6 shadow-xs flex flex-col justify-between"
        >
            <div className="flex items-center justify-between mb-4">
                <h3 className="font-bold text-base text-slate-900 tracking-tight">
                    {t("recentTransactions")}{" "}
                </h3>
                <button
                    type="button"
                    className="inline-flex items-center text-xs font-semibold text-blue-600 hover:text-blue-700 hover:underline cursor-pointer"
                >
                    {t("viewAll")} <ArrowRight className="w-3.5 h-3.5 ml-1" />
                </button>
            </div>

            {filtered.length === 0 ? (
                <div className="py-10 flex flex-col items-center justify-center text-slate-400 text-center">
                    <SearchX className="w-7 h-7 mb-2 stroke-[1.5]" />
                    <p className="text-xs"> {t("noMatchingTransactionsFound")} </p>
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
                                    <p className="text-[11px] text-slate-400 mt-0.5">{tx.date}</p>
                                </div>
                            </div>

                            <div className="flex items-center space-x-3 shrink-0">
                                <span
                                    className={`text-xs font-semibold ${
                                        tx.isPositive ? "text-emerald-600" : "text-slate-900"
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
