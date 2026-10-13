"use client";

import { useTranslations, useLocale } from "next-intl";

import { Wallet, PiggyBank, Luggage, MoreHorizontal, Plus, ArrowUpRight, Send } from "lucide-react";
import { Button } from "@/components/ui/button";

interface WalletCardsProps {
    mainBalance: number;
    currency?: string;
    walletId: string;
    onDepositClick: () => void;
    onWithdrawClick: () => void;
    onTransferClick: () => void;
}

export function WalletCards({
    mainBalance,
    currency = "VND",
    onDepositClick,
    onWithdrawClick,
    onTransferClick,
}: WalletCardsProps) {
    const t = useTranslations("dashboard");
    const locale = useLocale() === "vi" ? "vi-VN" : "en-US";
    const common = useTranslations("common");
    const formatVND = (value: number) =>
        new Intl.NumberFormat(locale, { style: "currency", currency }).format(value);

    return (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            {/* 1. Ví chính */}
            <div className="bg-white rounded-2xl border border-slate-200/90 p-5 sm:p-6 shadow-xs flex flex-col justify-between">
                <div>
                    <div className="flex items-center justify-between mb-3">
                        <div className="flex items-center space-x-2.5">
                            <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
                                <Wallet className="w-4 h-4 stroke-[1.75]" />
                            </div>
                            <span className="font-semibold text-xs text-slate-700">
                                {" "}
                                {t("mainWallet")}{" "}
                            </span>
                        </div>
                        <button
                            type="button"
                            aria-label={common("moreOptions")}
                            className="text-slate-400 hover:text-slate-600 p-1 rounded-md hover:bg-slate-50 transition-colors cursor-pointer"
                        >
                            <MoreHorizontal className="w-4 h-4" />
                        </button>
                    </div>

                    <div className="mt-2">
                        <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
                            {formatVND(mainBalance)}
                        </h2>
                    </div>
                </div>

                <div className="grid grid-cols-3 gap-2 mt-6">
                    <Button
                        size="sm"
                        variant="primary"
                        onClick={onDepositClick}
                        className="text-xs h-8.5 rounded-lg flex items-center justify-center gap-1 cursor-pointer"
                    >
                        <Plus className="w-3.5 h-3.5 stroke-[2]" /> {t("deposit")}{" "}
                    </Button>

                    <Button
                        variant="outline"
                        size="sm"
                        onClick={onWithdrawClick}
                        className="text-xs h-8.5 rounded-lg flex items-center justify-center gap-1 cursor-pointer"
                    >
                        <ArrowUpRight className="w-3.5 h-3.5 stroke-[2]" /> {t("withdraw")}{" "}
                    </Button>

                    <Button
                        variant="outline"
                        size="sm"
                        onClick={onTransferClick}
                        className="text-xs h-8.5 rounded-lg flex items-center justify-center gap-1 cursor-pointer"
                    >
                        <Send className="w-3.5 h-3.5 stroke-[2]" /> {t("transfer")}{" "}
                    </Button>
                </div>
            </div>

            {/* 2. Ví tiết kiệm */}
            <div className="bg-white rounded-2xl border border-slate-200/90 p-5 sm:p-6 shadow-xs flex flex-col justify-between">
                <div>
                    <div className="flex items-center justify-between mb-3">
                        <div className="flex items-center space-x-2.5">
                            <div className="w-8 h-8 rounded-lg bg-sky-50 text-sky-600 flex items-center justify-center">
                                <PiggyBank className="w-4 h-4 stroke-[1.75]" />
                            </div>
                            <span className="font-semibold text-xs text-slate-700">
                                {t("savingsWallet")}{" "}
                            </span>
                        </div>
                        <button
                            type="button"
                            className="text-slate-400 hover:text-slate-600 p-1 rounded-md hover:bg-slate-50 transition-colors cursor-pointer"
                        >
                            <MoreHorizontal className="w-4 h-4" />
                        </button>
                    </div>

                    <div className="mt-2">
                        <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
                            {formatVND(28300000)}
                        </h2>
                    </div>
                </div>

                <div className="mt-6 pt-2">
                    <p className="text-xs text-slate-500 font-normal">
                        {t("currentInterestRate")}{" "}
                        <span className="font-semibold text-slate-800">
                            {" "}
                            {t("yearlyInterest", {
                                rate: new Intl.NumberFormat(locale, {
                                    style: "percent",
                                    maximumFractionDigits: 1,
                                }).format(0.035),
                            })}{" "}
                        </span>
                    </p>
                </div>
            </div>

            {/* 3. Ví du lịch */}
            <div className="bg-white rounded-2xl border border-slate-200/90 p-5 sm:p-6 shadow-xs flex flex-col justify-between">
                <div>
                    <div className="flex items-center justify-between mb-3">
                        <div className="flex items-center space-x-2.5">
                            <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-500 flex items-center justify-center">
                                <Luggage className="w-4 h-4 stroke-[1.75]" />
                            </div>
                            <span className="font-semibold text-xs text-slate-700">
                                {" "}
                                {t("travelWallet")}{" "}
                            </span>
                        </div>
                        <button
                            type="button"
                            className="text-slate-400 hover:text-slate-600 p-1 rounded-md hover:bg-slate-50 transition-colors cursor-pointer"
                        >
                            <MoreHorizontal className="w-4 h-4" />
                        </button>
                    </div>

                    <div className="mt-2">
                        <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
                            {formatVND(5720000)}
                        </h2>
                    </div>
                </div>

                <div className="mt-6 pt-2">
                    <p className="text-xs text-slate-500 font-normal">{t("forYourTravels")} </p>
                </div>
            </div>
        </div>
    );
}
