"use client";

import { useTranslations, useLocale } from "next-intl";

import { CreditCard, Wallet, ArrowDownRight, ArrowRight } from "lucide-react";

export function SummaryLimitsCard({ currency = "VND" }: { currency?: string } = {}) {
    const t = useTranslations("dashboard");
    const locale = useLocale() === "vi" ? "vi-VN" : "en-US";
    const money = (value: number) =>
        new Intl.NumberFormat(locale, { style: "currency", currency }).format(value);

    return (
        <div className="bg-white rounded-2xl border border-slate-200/90 p-5 sm:p-6 shadow-xs">
            <div className="flex items-center justify-between mb-5">
                <h3 className="font-bold text-base text-slate-900 tracking-tight">
                    {t("limitsSummary")}{" "}
                </h3>
                <button
                    type="button"
                    className="inline-flex items-center text-xs font-semibold text-blue-600 hover:text-blue-700 hover:underline cursor-pointer"
                >
                    {t("viewDetails")} <ArrowRight className="w-3.5 h-3.5 ml-1" />
                </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {/* 1. Hạn mức giao dịch */}
                <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 flex flex-col justify-between">
                    <div className="flex items-center space-x-2.5 mb-3">
                        <div className="w-7 h-7 rounded-md bg-blue-50 text-blue-600 flex items-center justify-center">
                            <Wallet className="w-3.5 h-3.5 stroke-[1.75]" />
                        </div>
                        <span className="text-xs font-semibold text-slate-700">
                            {t("transactionLimit")}{" "}
                        </span>
                    </div>

                    <div>
                        <div className="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden mb-2">
                            <div className="bg-blue-600 h-full rounded-full w-1/2" />
                        </div>
                        <p className="text-xs font-bold text-slate-900">
                            {money(50000000)}{" "}
                            <span className="text-slate-400 font-normal">/ {money(100000000)}</span>
                        </p>
                    </div>
                </div>

                {/* 2. Hạn mức rút tiền */}
                <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 flex flex-col justify-between">
                    <div className="flex items-center space-x-2.5 mb-3">
                        <div className="w-7 h-7 rounded-md bg-sky-50 text-sky-600 flex items-center justify-center">
                            <CreditCard className="w-3.5 h-3.5 stroke-[1.75]" />
                        </div>
                        <span className="text-xs font-semibold text-slate-700">
                            {t("withdrawalLimit")}{" "}
                        </span>
                    </div>

                    <div>
                        <div className="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden mb-2">
                            <div className="bg-blue-600 h-full rounded-full w-full" />
                        </div>
                        <p className="text-xs font-bold text-slate-900">
                            {money(20000000)}{" "}
                            <span className="text-slate-400 font-normal">/ {money(20000000)}</span>
                        </p>
                    </div>
                </div>

                {/* 3. Tổng chi tiêu tháng này */}
                <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 flex flex-col justify-between">
                    <div className="flex items-center space-x-2.5 mb-2">
                        <div className="w-7 h-7 rounded-md bg-indigo-50 text-indigo-600 flex items-center justify-center">
                            <ArrowDownRight className="w-3.5 h-3.5 stroke-[1.75]" />
                        </div>
                        <span className="text-xs font-semibold text-slate-700">
                            {t("spendingThisMonth")}{" "}
                        </span>
                    </div>

                    <div>
                        <p className="text-base font-bold text-slate-900">{money(6320000)}</p>
                        <div className="inline-flex items-center space-x-1 mt-1 text-[11px] font-semibold text-emerald-600">
                            <span>↓ 12%</span>
                            <span className="text-slate-400 font-normal">
                                {" "}
                                {t("comparedWithLastMonth")}{" "}
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
