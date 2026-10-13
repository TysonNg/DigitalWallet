"use client";

import { useTranslations } from "next-intl";

import { LanguageSwitcher } from "@/components/language-switcher";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { AuthModal } from "@/features/auth/components/auth-modal";
import { Button } from "@/components/ui/button";
import { ArrowRight, ShieldCheck, Zap, BarChart2 } from "lucide-react";
import { Suspense } from "react";
import { WalletCards } from "../dashboard/components/wallet-cards";
import { ActivityChart } from "../dashboard/components/activity-chart";
import { SummaryLimitsCard } from "../dashboard/components/summary-limits";
import { RecentTransactions } from "../dashboard/components/recent-transactions";
import { AllocationChart } from "../dashboard/components/allocation-chart";

export function LandingView() {
    const t = useTranslations("landing");

    const openAuthModal = useAuthStore((state) => state.openAuthModal);

    return (
        <div className="min-h-screen flex flex-col bg-[#f8fafc] text-slate-900">
            {/* Top Minimalist Navigation */}
            <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200/80">
                <div className="max-w-7xl mx-auto flex h-14 items-center justify-between px-4 sm:px-8">
                    <div className="flex items-center space-x-2.5">
                        <div className="w-7 h-7 rounded-lg bg-blue-600 text-white flex items-center justify-center font-bold text-xs">
                            W
                        </div>
                        <span className="font-bold text-sm tracking-tight text-slate-900">
                            Digital Wallet
                        </span>
                    </div>

                    <div className="flex items-center space-x-2 sm:space-x-3">
                        <LanguageSwitcher />
                        <button
                            type="button"
                            onClick={() => openAuthModal("login")}
                            className="text-xs font-semibold text-slate-600 hover:text-slate-900 px-3 py-1.5 rounded-lg hover:bg-slate-50 transition-colors cursor-pointer"
                        >
                            {t("logIn")}{" "}
                        </button>
                        <button
                            type="button"
                            onClick={() => openAuthModal("register")}
                            className="text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 px-3.5 py-1.5 rounded-lg transition-colors cursor-pointer shadow-xs"
                        >
                            {t("openAWallet")}{" "}
                        </button>
                    </div>
                </div>
            </header>

            {/* Hero Section */}
            <main className="flex-1">
                <section className="pt-16 pb-12 sm:pt-20 sm:pb-16 text-center max-w-4xl mx-auto px-4 sm:px-6">
                    <h1 className="text-3xl sm:text-5xl font-extrabold text-slate-900 tracking-tight leading-[1.15]">
                        {t("yourNextgenerationPersonalWallet")}{" "}
                    </h1>

                    <p className="mt-4 text-sm sm:text-base text-slate-500 max-w-xl mx-auto leading-relaxed">
                        {t(
                            "manageYourBalanceSendAndReceiveMoneyInstantlyAndTrackYourSpendingWithASimpleSecureExperience"
                        )}{" "}
                    </p>

                    <div className="flex items-center justify-center gap-3 mt-7">
                        <Button
                            size="sm"
                            onClick={() => openAuthModal("register")}
                            className="h-10 px-5 text-xs font-semibold bg-blue-600 hover:bg-blue-700 text-white rounded-lg shadow-xs cursor-pointer flex items-center gap-1.5"
                        >
                            {t("openYourFreeWallet")} <ArrowRight className="w-3.5 h-3.5" />
                        </Button>
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={() => openAuthModal("login")}
                            className="h-10 px-5 text-xs font-semibold border-slate-200 text-slate-700 hover:bg-slate-50 rounded-lg cursor-pointer"
                        >
                            {t("logInToYourWallet")}{" "}
                        </Button>
                    </div>
                </section>

                {/* Live Real Dashboard Preview (Clean 1px Border, No fake macOS window) */}
                <section className="max-w-6xl mx-auto px-4 sm:px-6 pb-20">
                    <div className="rounded-2xl border border-slate-200/90 bg-white p-4 sm:p-7 shadow-xs">
                        <div className="flex items-center justify-between pb-5 border-b border-slate-100 mb-6">
                            <div>
                                <h2 className="text-lg font-bold text-slate-900 tracking-tight">
                                    {t("walletOverview")}{" "}
                                </h2>
                                <p className="text-xs text-slate-400 mt-0.5">
                                    {t("liveDashboardPreview")}{" "}
                                </p>
                            </div>
                            <span className="text-[11px] font-semibold text-slate-500 bg-slate-50 px-2.5 py-1 rounded-md border border-slate-200/60">
                                {t("previewMode")}{" "}
                            </span>
                        </div>

                        {/* Top 3 Cards */}
                        <div className="space-y-6">
                            <WalletCards
                                mainBalance={12450000}
                                walletId="wal-demo-preview"
                                onDepositClick={() => openAuthModal("login")}
                                onWithdrawClick={() => openAuthModal("login")}
                                onTransferClick={() => openAuthModal("login")}
                            />

                            {/* 2 Columns */}
                            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
                                <div className="space-y-6">
                                    <ActivityChart />
                                    <SummaryLimitsCard />
                                </div>
                                <div className="space-y-6">
                                    <RecentTransactions searchQuery="" />
                                    <AllocationChart mainBalance={12450000} />
                                </div>
                            </div>
                        </div>
                    </div>
                </section>

                {/* Minimalist 3 Values */}
                <section className="border-t border-slate-200/80 bg-white py-14">
                    <div className="max-w-5xl mx-auto px-4 sm:px-6">
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 text-left">
                            <div>
                                <div className="flex items-center space-x-2 text-slate-900 font-bold text-sm mb-2">
                                    <Zap className="w-4 h-4 text-blue-600 stroke-[2]" />
                                    <span> {t("instantFree")} </span>
                                </div>
                                <p className="text-xs text-slate-500 leading-relaxed">
                                    {t(
                                        "depositWithdrawAndTransferMoneyInternally247NoInternalTransactionOrBalanceMaintenanceFees"
                                    )}{" "}
                                </p>
                            </div>

                            <div>
                                <div className="flex items-center space-x-2 text-slate-900 font-bold text-sm mb-2">
                                    <ShieldCheck className="w-4 h-4 text-blue-600 stroke-[2]" />
                                    <span> {t("twolayerSecurity")} </span>
                                </div>
                                <p className="text-xs text-slate-500 leading-relaxed">
                                    {t(
                                        "securePasswordEncryptionAccountActivationAndVerificationOfChangesUsingAnOTPSentToYourEmail"
                                    )}{" "}
                                </p>
                            </div>

                            <div>
                                <div className="flex items-center space-x-2 text-slate-900 font-bold text-sm mb-2">
                                    <BarChart2 className="w-4 h-4 text-blue-600 stroke-[2]" />
                                    <span> {t("clearInsights")} </span>
                                </div>
                                <p className="text-xs text-slate-500 leading-relaxed">
                                    {t(
                                        "seeDailyBalanceChangesWalletAllocationsAndSmartSpendingLimitAlerts"
                                    )}{" "}
                                </p>
                            </div>
                        </div>
                    </div>
                </section>
            </main>

            {/* Clean Footer */}
            <footer className="border-t border-slate-200/80 bg-slate-50 py-6 text-center text-xs text-slate-400">
                <p> {t("digitalWallet2026SimpleAndSecureByDesign")} </p>
            </footer>

            {/* Auth Popup Modal */}
            <Suspense fallback={null}>
                <AuthModal />
            </Suspense>
        </div>
    );
}
