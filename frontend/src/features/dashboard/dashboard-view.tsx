"use client";

import { useState, useEffect } from "react";
import { User } from "@/features/auth/types/auth.types";
import { WalletDto, TransactionDto } from "@/features/wallet/types/wallet.types";
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
import { getMyWalletAction, getWalletTransactionsAction } from "@/features/wallet/actions/wallet.actions";
import { getNotificationsAction, markAllNotificationsReadAction } from "./actions/notification.actions";
import { useNotificationSocket } from "./hooks/use-notification-socket";
import { NotificationItem } from "./types/notification.types";
import { Menu, X } from "lucide-react";

interface DashboardViewProps {
    user: User;
    initialWallet: WalletDto | null;
    initialTransactions: TransactionDto[];
}

export function DashboardView({
    user,
    initialWallet,
    initialTransactions,
}: DashboardViewProps) {
    const [wallet, setWallet] = useState<WalletDto | null>(initialWallet);
    const [transactions, setTransactions] = useState<TransactionDto[]>(initialTransactions);
    const [searchQuery, setSearchQuery] = useState<string>("");
    const [activeTab, setActiveTab] = useState<"overview" | "transfer" | "deposit-withdraw" | "history" | "settings">("overview");

    // Notifications state
    const [notifications, setNotifications] = useState<NotificationItem[]>([]);
    const [hasUnread, setHasUnread] = useState(false);

    // Modal states
    const [isDepositOpen, setIsDepositOpen] = useState(false);
    const [isWithdrawOpen, setIsWithdrawOpen] = useState(false);
    const [isTransferOpen, setIsTransferOpen] = useState(false);
    const [isSettingsOpen, setIsSettingsOpen] = useState(false);
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

    const mainBalance = wallet ? wallet.balance : 12450000;
    const walletId = wallet ? wallet.id : "wallet-default-main";

    // Refresh wallet and transaction data after actions or on realtime event
    const refreshData = async () => {
        try {
            const walletRes = await getMyWalletAction();
            if (walletRes.success && walletRes.data) {
                setWallet(walletRes.data);
                const txRes = await getWalletTransactionsAction(walletRes.data.id);
                if (txRes.success && txRes.data) {
                    setTransactions(txRes.data);
                }
            }
        } catch {
            // keep current state
        }
    };

    // Load initial notifications from backend
    useEffect(() => {
        async function loadNotifications() {
            const res = await getNotificationsAction();
            if (res.success && res.data) {
                setNotifications(res.data);
                const unreadExists = res.data.some((n) => !n.isRead);
                setHasUnread(unreadExists);
            }
        }
        loadNotifications();
    }, []);

    // Lắng nghe thông báo realtime qua WebSocket STOMP
    useNotificationSocket({
        onNotificationReceived: (notification) => {
            setNotifications((prev) => [notification, ...prev]);
            setHasUnread(true);
            refreshData(); // Tự động cập nhật số dư ví và bảng lịch sử giao dịch!
        },
    });

    const handleOpenNotifications = async () => {
        if (hasUnread) {
            setHasUnread(false);
            setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
            await markAllNotificationsReadAction();
        }
    };

    const handleTabSelect = (tab: "overview" | "transfer" | "deposit-withdraw" | "history" | "settings") => {
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

    // Format current localized Vietnamese date
    const getFormattedDate = () => {
        const today = new Date();
        const days = ["Chủ nhật", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"];
        const dayOfWeek = days[today.getDay()];
        const day = today.getDate();
        const month = today.getMonth() + 1;
        const year = today.getFullYear();
        return `${dayOfWeek}, ${day} tháng ${month}, ${year}`;
    };

    return (
        <div className="min-h-screen bg-slate-50 flex text-slate-900">
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
                    notifications={notifications}
                    hasUnread={hasUnread}
                    onOpenNotifications={handleOpenNotifications}
                />

                {/* Dashboard Main Content Body */}
                <main className="flex-1 p-4 sm:p-8 max-w-7xl w-full mx-auto space-y-6">
                    {/* Welcome Header & Date Banner */}
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                        <div>
                            <h1 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
                                Tổng quan ví
                            </h1>
                            <p className="text-xs sm:text-sm text-slate-500 mt-1">
                                Chào mừng bạn trở lại! Cùng xem tình hình tài chính của bạn hôm nay.
                            </p>
                        </div>
                        <div className="text-xs font-medium text-slate-400 sm:text-right">
                            {getFormattedDate()}
                        </div>
                    </div>

                    {/* Row 1: 3 Wallet Cards */}
                    <WalletCards
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
                            <ActivityChart />
                            <SummaryLimitsCard />
                        </div>

                        {/* Right Column: Giao dịch gần đây + Phân bổ ví */}
                        <div className="space-y-6">
                            <RecentTransactions
                                searchQuery={searchQuery}
                                realTransactions={transactions}
                            />
                            <AllocationChart mainBalance={mainBalance} />
                        </div>
                    </div>
                </main>
            </div>

            {/* Modals */}
            <DepositModal
                isOpen={isDepositOpen}
                onClose={() => setIsDepositOpen(false)}
                walletId={walletId}
                onSuccess={refreshData}
            />

            <WithdrawModal
                isOpen={isWithdrawOpen}
                onClose={() => setIsWithdrawOpen(false)}
                walletId={walletId}
                currentBalance={mainBalance}
                onSuccess={refreshData}
            />

            <TransferModal
                isOpen={isTransferOpen}
                onClose={() => setIsTransferOpen(false)}
                senderWalletId={walletId}
                currentBalance={mainBalance}
                onSuccess={refreshData}
            />

            <SettingsModal
                isOpen={isSettingsOpen}
                onClose={() => setIsSettingsOpen(false)}
                user={user}
            />
        </div>
    );
}
