"use client";

import { useState, useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useRouter } from "@/i18n/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { Menu, X } from "lucide-react";
import { User } from "@/features/auth/types/auth.types";
import { queryKeys } from "@/lib/query-keys";
import { useMyWalletQuery } from "@/features/wallet/hooks/use-wallet";
import { useNotificationSocket } from "../hooks/use-notification-socket";
import { Sidebar } from "./sidebar";
import { TopHeader } from "./top-header";

interface DashboardShellProps {
    user: User;
    children: React.ReactNode;
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

export function DashboardShell({ user, children }: DashboardShellProps) {
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
    const queryClient = useQueryClient();
    const common = useTranslations("common");

    const { data: wallet } = useMyWalletQuery();

    // Lắng nghe thông báo realtime qua WebSocket STOMP xuyên suốt toàn bộ ứng dụng
    useNotificationSocket({
        onNotificationReceived: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.wallet.all });
            queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all });
        },
    });

    return (
        <div className="min-h-screen bg-slate-50 flex text-slate-900">
            <Suspense fallback={null}>
                <PaymentFeedbackListener />
            </Suspense>

            {/* Desktop Sidebar (Persistent) */}
            <div className="hidden lg:block">
                <Sidebar />
            </div>

            {/* Mobile Sidebar Drawer */}
            {isMobileMenuOpen && (
                <div className="fixed inset-0 z-50 lg:hidden flex">
                    <div
                        className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs transition-opacity"
                        onClick={() => setIsMobileMenuOpen(false)}
                    />
                    <div className="relative z-10 w-64 bg-white h-full shadow-2xl flex flex-col">
                        <div className="flex justify-end p-2 border-b border-slate-100">
                            <button
                                aria-label={common("close")}
                                onClick={() => setIsMobileMenuOpen(false)}
                                className="p-2 text-slate-400 hover:text-slate-600 rounded-lg cursor-pointer"
                            >
                                <X className="w-5 h-5" />
                            </button>
                        </div>
                        <Sidebar onItemClick={() => setIsMobileMenuOpen(false)} />
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
                <TopHeader user={user} walletId={wallet?.id} />

                {/* Page Content Body */}
                <div className="flex-1 flex flex-col min-w-0">{children}</div>
            </div>
        </div>
    );
}
