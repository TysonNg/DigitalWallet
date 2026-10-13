"use client";

import { useTranslations, useLocale } from "next-intl";

import { useState, useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useRouter } from "@/i18n/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { User } from "@/features/auth/types/auth.types";
import { WalletDto, TransactionDto } from "@/features/wallet/types/wallet.types";
import { queryKeys } from "@/lib/query-keys";
import { useMyWalletQuery, useTransactionsQuery } from "@/features/wallet/hooks/use-wallet";
import { Sidebar } from "./components/sidebar";
import { TopHeader } from "./components/top-header";
import { WalletCards } from "./components/wallet-cards";
import { ActivityChart } from "./components/activity-chart";
import { SummaryLimitsCard } from "./components/summary-limits";
import { RecentTransactions } from "./components/recent-transactions";
import { AllocationChart } from "./components/allocation-chart";
import { DepositModal } from "@/features/wallet/components/deposit-modal";
import { WithdrawModal } from "@/features/wallet/components/withdraw-modal";
import { TransferModal } from "@/features/wallet/components/transfer-modal";
import { SettingsModal } from "./components/settings-modal";
import { useNotificationSocket } from "./hooks/use-notification-socket";
import { Menu, X } from "lucide-react";

interface DashboardViewProps {
    user: User;
    initialWallet?: WalletDto | null;
    initialTransactions?: TransactionDto[];
}

function PaymentFeedbackListener() {
    const t = useTranslations("dashboard");
    const router = useRouter();
    const searchParams = useSearchParams();
    const queryClient = useQueryClient();

    useEffect(() => {
        const payment = searchParams.get("payment");
        if (payment === "success") {
            toast.success(t("paymentSuccessfulYourWalletHasBeenCredited"));
            queryClient.invalidateQueries({ queryKey: queryKeys.wallet.all });
            router.replace("/dashboard", { scroll: false });
        } else if (payment === "cancelled") {
            toast.info(t("yourDepositWasCancelled"));
            router.replace("/dashboard", { scroll: false });
        }
    }, [searchParams, queryClient, router, t]);

    return null;
}

export function DashboardView({ user, initialWallet, initialTransactions }: DashboardViewProps) {
    const t = useTranslations("dashboard");
    const locale = useLocale() === "vi" ? "vi-VN" : "en-US";

    const common = useTranslations("common");
    const queryClient = useQueryClient();

    // Queries từ React Query (Đã được hydrate dữ liệu từ SSR Server Component)
    const { data: wallet = initialWallet ?? null } = useMyWalletQuery();
    const currentWallet = wallet || initialWallet;
    const { data: transactions = initialTransactions ?? [] } = useTransactionsQuery(
        currentWallet?.id
    );

    const [searchQuery, setSearchQuery] = useState<string>("");
    const [activeTab, setActiveTab] = useState<
        "overview" | "transfer" | "deposit-withdraw" | "history" | "settings"
    >("overview");

    // Modal states
    const [isDepositOpen, setIsDepositOpen] = useState(false);
    const [isWithdrawOpen, setIsWithdrawOpen] = useState(false);
    const [isTransferOpen, setIsTransferOpen] = useState(false);
    const [isSettingsOpen, setIsSettingsOpen] = useState(false);
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

    const mainBalance = currentWallet ? currentWallet.balance : 0;
    const walletId = currentWallet ? currentWallet.id : "";

    // Lắng nghe thông báo realtime qua WebSocket STOMP
    useNotificationSocket({
        onNotificationReceived: () => {
            // Tự động invalidate và làm mới dữ liệu ví, giao dịch và thông báo ngầm
            queryClient.invalidateQueries({ queryKey: queryKeys.wallet.all });
            queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all });
        },
    });

    const handleTabSelect = (
        tab: "overview" | "transfer" | "deposit-withdraw" | "history" | "settings"
    ) => {
        setActiveTab(tab);
        setIsMobileMenuOpen(false);

        if (tab === "transfer") {
            setIsTransferOpen(true);
        } else if (tab === "deposit-withdraw") {
            setIsDepositOpen(true);
        } else if (tab === "settings") {
            setIsSettingsOpen(true);
        } else if (tab === "history") {
            const el = document.getElementById("transactions-section");
            if (el) el.scrollIntoView({ behavior: "smooth" });
        }
    };

    const getFormattedDate = () =>
        new Intl.DateTimeFormat(locale, {
            timeZone: "Asia/Ho_Chi_Minh",
            weekday: "long",
            day: "numeric",
            month: "long",
            year: "numeric",
        }).format(new Date());

    return (
        <div className="min-h-screen bg-slate-50 flex text-slate-900">
            <Suspense fallback={null}>
                <PaymentFeedbackListener />
            </Suspense>
            {/* Desktop Sidebar */}
            <div className="hidden lg:block">
                <Sidebar activeTab={activeTab} onTabSelect={handleTabSelect} />
            </div>

            {/* Mobile Sidebar Drawer */}
            {isMobileMenuOpen && (
                <div className="fixed inset-0 z-50 lg:hidden flex">
                    <div
                        className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs"
                        onClick={() => setIsMobileMenuOpen(false)}
                    />
                    <div className="relative z-10 w-64 bg-white h-full shadow-2xl flex flex-col">
                        <div className="flex justify-end p-2">
                            <button
                                aria-label={common("close")}
                                onClick={() => setIsMobileMenuOpen(false)}
                                className="p-2 text-slate-400 hover:text-slate-600 rounded-lg cursor-pointer"
                            >
                                <X className="w-5 h-5" />
                            </button>
                        </div>
                        <Sidebar activeTab={activeTab} onTabSelect={handleTabSelect} />
                    </div>
                </div>
            )}

            {/* Main Area */}
            <div className="flex-1 flex flex-col min-w-0">
                {/* Mobile Top Bar */}
                <div className="lg:hidden flex items-center justify-between p-3 bg-white border-b border-slate-200">
                    <button
                        aria-label={common("openMenu")}
                        onClick={() => setIsMobileMenuOpen(true)}
                        className="p-2 rounded-xl text-slate-600 hover:bg-slate-100 cursor-pointer"
                    >
                        <Menu className="w-5 h-5" />
                    </button>
                    <span className="font-bold text-sm text-slate-900">Digital Wallet</span>
                    <div className="w-9" />
                </div>

                {/* Desktop Top Header */}
                <TopHeader
                    user={user}
                    walletId={wallet?.id}
                    searchQuery={searchQuery}
                    onSearchChange={setSearchQuery}
                />

                {/* Dashboard Main Content Body */}
                <main className="flex-1 p-4 sm:p-8 max-w-7xl w-full mx-auto space-y-6">
                    {/* Welcome Header & Date Banner */}
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                        <div>
                            <h1 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
                                {t("walletOverview")}{" "}
                            </h1>
                            <p className="text-xs sm:text-sm text-slate-500 mt-1">
                                {t("welcomeBackSeeHowYourFinancesAreDoingToday")}{" "}
                            </p>
                        </div>
                        <div className="text-xs font-medium text-slate-400 sm:text-right">
                            {getFormattedDate()}
                        </div>
                    </div>

                    {/* Row 1: 3 Wallet Cards */}
                    <WalletCards
                        currency={currentWallet?.currency ?? "VND"}
                        mainBalance={mainBalance}
                        walletId={walletId}
                        onDepositClick={() => setIsDepositOpen(true)}
                        onWithdrawClick={() => setIsWithdrawOpen(true)}
                        onTransferClick={() => setIsTransferOpen(true)}
                    />

                    {/* Row 2: 2 Columns Grid */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
                        {/* Left Column: Hoạt động 30 ngày + Hạn mức & tóm tắt */}
                        <div className="space-y-6">
                            <ActivityChart currency={currentWallet?.currency ?? "VND"} />
                            <SummaryLimitsCard currency={currentWallet?.currency ?? "VND"} />
                        </div>

                        {/* Right Column: Giao dịch gần đây + Phân bổ ví */}
                        <div className="space-y-6">
                            <RecentTransactions
                                currency={currentWallet?.currency ?? "VND"}
                                searchQuery={searchQuery}
                                realTransactions={transactions}
                            />
                            <AllocationChart
                                currency={currentWallet?.currency ?? "VND"}
                                mainBalance={mainBalance}
                            />
                        </div>
                    </div>
                </main>
            </div>

            {/* Modals */}
            <DepositModal
                currency={currentWallet?.currency ?? "VND"}
                isOpen={isDepositOpen}
                onClose={() => setIsDepositOpen(false)}
                walletId={walletId}
            />

            <WithdrawModal
                currency={currentWallet?.currency ?? "VND"}
                isOpen={isWithdrawOpen}
                onClose={() => setIsWithdrawOpen(false)}
                walletId={walletId}
                currentBalance={mainBalance}
            />

            <TransferModal
                currency={currentWallet?.currency ?? "VND"}
                isOpen={isTransferOpen}
                onClose={() => setIsTransferOpen(false)}
                senderWalletId={walletId}
                currentBalance={mainBalance}
            />

            <SettingsModal
                isOpen={isSettingsOpen}
                onClose={() => setIsSettingsOpen(false)}
                user={user}
            />
        </div>
    );
}
