"use client";

import { useTranslations, useLocale } from "next-intl";
import { User } from "@/features/auth/types/auth.types";
import { WalletDto, TransactionDto } from "@/features/wallet/types/wallet.types";
import { Link } from "@/i18n/navigation";
import { useMyWalletQuery, useTransactionsQuery } from "@/features/wallet/hooks/use-wallet";
import { WalletCards } from "./components/wallet-cards";
import { ActivityChart } from "./components/activity-chart";
import { SummaryLimitsCard } from "./components/summary-limits";
import { RecentTransactions } from "./components/recent-transactions";
import { AllocationChart } from "./components/allocation-chart";

interface DashboardViewProps {
    user?: User;
    initialWallet?: WalletDto | null;
    initialTransactions?: TransactionDto[];
}

export function DashboardView({ initialWallet, initialTransactions }: DashboardViewProps) {
    const t = useTranslations("dashboard");
    const locale = useLocale() === "vi" ? "vi-VN" : "en-US";

    const { data: wallet = initialWallet ?? null, isPending, error, refetch } = useMyWalletQuery();
    const currentWallet = wallet;
    const {
        data: transactions = initialTransactions ?? [],
        isPending: transactionsPending,
        error: transactionsError,
        refetch: refetchTransactions,
    } = useTransactionsQuery(currentWallet?.id);

    const mainBalance = currentWallet ? currentWallet.balance : 0;
    const walletId = currentWallet ? currentWallet.id : "";

    const getFormattedDate = () =>
        new Intl.DateTimeFormat(locale, {
            timeZone: "Asia/Ho_Chi_Minh",
            weekday: "long",
            day: "numeric",
            month: "long",
            year: "numeric",
        }).format(new Date());

    if (!currentWallet) {
        return (
            <main className="min-h-[60vh] flex flex-col items-center justify-center gap-4 p-6">
                <p role={error ? "alert" : "status"} className="text-sm text-slate-500">
                    {isPending ? t("walletLoading") : error?.message || t("walletUnavailable")}
                </p>
                {!isPending && (
                    <button
                        className="rounded-lg border border-slate-200 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50 cursor-pointer"
                        onClick={() => void refetch()}
                    >
                        {t("walletRetry")}
                    </button>
                )}
                {!isPending && (
                    <Link
                        href="/?auth=login"
                        className="text-xs text-blue-600 hover:underline font-medium"
                    >
                        {t("walletLogin")}
                    </Link>
                )}
            </main>
        );
    }

    return (
        <main className="flex-1 p-4 sm:p-8 max-w-7xl w-full mx-auto space-y-6">
            {/* Welcome Header & Date Banner */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                <div>
                    <h1 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
                        {t("walletOverview")}
                    </h1>
                    <p className="text-xs sm:text-sm text-slate-500 mt-1">
                        {t("welcomeBackSeeHowYourFinancesAreDoingToday")}
                    </p>
                </div>
                <div className="text-xs font-medium text-slate-400 sm:text-right">
                    {getFormattedDate()}
                </div>
            </div>

            {/* Wallet balance */}
            {error && (
                <p role="alert" className="text-sm text-red-600">
                    {error.message}
                </p>
            )}
            <WalletCards
                currency={currentWallet?.currency ?? "VND"}
                mainBalance={mainBalance}
                walletId={walletId}
            />

            {transactionsError ? (
                <div role="alert" className="rounded-xl border border-red-200 p-5 space-y-3">
                    <p>{t("transactionsUnavailable")}</p>
                    <button
                        className="text-blue-600 cursor-pointer"
                        onClick={() => void refetchTransactions()}
                    >
                        {t("walletRetry")}
                    </button>
                </div>
            ) : transactionsPending && !initialTransactions ? (
                <p role="status" className="text-sm text-slate-500">
                    {t("transactionsLoading")}
                </p>
            ) : (
                <>
                    {/* Row 2: 2 Columns Grid */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
                        {/* Left Column: Hoạt động 30 ngày + Hạn mức & tóm tắt */}
                        <div className="space-y-6">
                            <ActivityChart
                                currency={currentWallet.currency}
                                transactions={transactions}
                                walletId={walletId}
                            />
                            <SummaryLimitsCard
                                currency={currentWallet.currency}
                                transactions={transactions}
                                walletId={walletId}
                            />
                        </div>

                        {/* Right Column: Giao dịch gần đây + Phân bổ ví */}
                        <div className="space-y-6">
                            <RecentTransactions
                                currency={currentWallet?.currency ?? "VND"}
                                walletId={walletId}
                                realTransactions={transactions}
                            />
                            <AllocationChart
                                currency={currentWallet?.currency ?? "VND"}
                                mainBalance={mainBalance}
                            />
                        </div>
                    </div>
                </>
            )}
        </main>
    );
}
