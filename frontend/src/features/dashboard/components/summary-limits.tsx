"use client";

import { useTranslations, useLocale } from "next-intl";
import type { TransactionDto } from "@/features/wallet/types/wallet.types";
import { monthlySpending } from "../dashboard-data";

export function SummaryLimitsCard({
    currency = "VND",
    transactions,
    walletId,
}: {
    currency?: string;
    transactions: TransactionDto[];
    walletId: string;
}) {
    const t = useTranslations("dashboard");
    const locale = useLocale() === "vi" ? "vi-VN" : "en-US";
    const spending = monthlySpending(transactions, walletId);
    const money = (value: number) =>
        new Intl.NumberFormat(locale, {
            style: "currency",
            currency,
            maximumFractionDigits: 0,
        }).format(value);

    // Standard banking and e-wallet regulatory limits
    const txLimit = currency === "VND" ? 20000000 : 1000;
    const withdrawLimit = currency === "VND" ? 50000000 : 2500;

    return (
        <div className="bg-white rounded-2xl border border-slate-200/90 p-5 sm:p-6 shadow-xs space-y-4">
            <h3 className="font-bold text-base text-slate-900">{t("limitsSummary")}</h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 space-y-2">
                    <p className="text-xs font-semibold text-slate-700">{t("transactionLimit")}</p>
                    <p className="text-base font-bold text-slate-900">{money(txLimit)}</p>
                    <p className="text-[11px] text-slate-500">
                        {locale === "vi-VN" ? "Tối đa mỗi giao dịch" : "Max per single transaction"}
                    </p>
                </div>
                <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 space-y-2">
                    <p className="text-xs font-semibold text-slate-700">{t("withdrawalLimit")}</p>
                    <p className="text-base font-bold text-slate-900">{money(withdrawLimit)}</p>
                    <p className="text-[11px] text-slate-500">
                        {locale === "vi-VN" ? "Hạn mức tối đa mỗi ngày" : "Daily cumulative limit"}
                    </p>
                </div>
                <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 space-y-2">
                    <p className="text-xs font-semibold text-slate-700">{t("spendingThisMonth")}</p>
                    <p className="text-base font-bold text-slate-900">{money(spending.current)}</p>
                    {spending.changePercent !== null ? (
                        <p className="text-xs text-slate-500">
                            {new Intl.NumberFormat(locale, {
                                style: "percent",
                                maximumFractionDigits: 1,
                                signDisplay: "exceptZero",
                            }).format(spending.changePercent / 100)}{" "}
                            {t("comparedWithLastMonth")}
                        </p>
                    ) : (
                        <p className="text-[11px] text-slate-400">
                            {locale === "vi-VN"
                                ? "Dữ liệu tháng hiện tại"
                                : "Current month activity"}
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
}
