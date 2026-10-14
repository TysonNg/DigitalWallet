"use client";

import { useTranslations } from "next-intl";
import { Link, usePathname } from "@/i18n/navigation";
import { Home, ArrowLeftRight, CreditCard, Clock, Settings, BookUser } from "lucide-react";

interface SidebarProps {
    activeTab?: string;
    onTabSelect?: (tab: string) => void;
    onItemClick?: () => void;
}

export function Sidebar({ onItemClick }: SidebarProps) {
    const t = useTranslations("dashboard");
    const pathname = usePathname();

    const mainNavItems = [
        {
            href: "/dashboard",
            label: t("overview"),
            icon: Home,
            isActive: pathname === "/dashboard",
        },
        {
            href: "/dashboard/transfer",
            label: t("transfer"),
            icon: ArrowLeftRight,
            isActive: pathname.startsWith("/dashboard/transfer"),
        },
        {
            href: "/dashboard/deposit-withdraw",
            label: t("depositWithdraw"),
            icon: CreditCard,
            isActive: pathname.startsWith("/dashboard/deposit-withdraw"),
        },
        {
            href: "/dashboard/beneficiaries",
            label: t("beneficiaries"),
            icon: BookUser,
            isActive: pathname.startsWith("/dashboard/beneficiaries"),
        },
        {
            href: "/dashboard/history",
            label: t("transactionHistory"),
            icon: Clock,
            isActive: pathname.startsWith("/dashboard/history"),
        },
    ];

    const systemNavItems = [
        {
            href: "/dashboard/settings",
            label: t("settings"),
            icon: Settings,
            isActive: pathname.startsWith("/dashboard/settings"),
        },
    ];

    return (
        <aside className="w-60 bg-white border-r border-slate-200/80 flex flex-col shrink-0 sticky top-0 h-screen select-none">
            {/* Brand Logo & Name */}
            <div className="h-16 flex items-center px-6 border-b border-slate-100">
                <Link href="/dashboard" className="flex items-center space-x-2.5">
                    <div className="w-7 h-7 rounded-lg bg-blue-600 text-white flex items-center justify-center font-bold text-xs shadow-xs">
                        W
                    </div>
                    <span className="font-bold text-sm tracking-tight text-slate-900">
                        Digital Wallet
                    </span>
                </Link>
            </div>

            {/* Navigation Menu */}
            <div className="p-3 space-y-4 flex-1 overflow-y-auto">
                {/* Main Navigation Section */}
                <div>
                    <div className="px-3 pb-1 text-[11px] font-semibold tracking-wider text-slate-400 uppercase">
                        {t("navMain")}
                    </div>
                    <nav className="space-y-1">
                        {mainNavItems.map((item) => {
                            const Icon = item.icon;
                            return (
                                <Link
                                    key={item.href}
                                    href={item.href}
                                    onClick={onItemClick}
                                    className={`w-full flex items-center space-x-3 px-3.5 py-2.5 rounded-lg text-xs transition-colors cursor-pointer ${
                                        item.isActive
                                            ? "bg-blue-50 text-blue-600 font-semibold"
                                            : "text-slate-600 hover:text-slate-900 hover:bg-slate-50 font-medium"
                                    }`}
                                >
                                    <Icon
                                        className={`w-4 h-4 stroke-[1.75] ${
                                            item.isActive ? "text-blue-600" : "text-slate-400"
                                        }`}
                                    />
                                    <span>{item.label}</span>
                                </Link>
                            );
                        })}
                    </nav>
                </div>

                {/* System / Preferences Section */}
                <div>
                    <div className="px-3 pb-1 text-[11px] font-semibold tracking-wider text-slate-400 uppercase">
                        {t("navSystem")}
                    </div>
                    <nav className="space-y-1">
                        {systemNavItems.map((item) => {
                            const Icon = item.icon;
                            return (
                                <Link
                                    key={item.href}
                                    href={item.href}
                                    onClick={onItemClick}
                                    className={`w-full flex items-center space-x-3 px-3.5 py-2.5 rounded-lg text-xs transition-colors cursor-pointer ${
                                        item.isActive
                                            ? "bg-blue-50 text-blue-600 font-semibold"
                                            : "text-slate-600 hover:text-slate-900 hover:bg-slate-50 font-medium"
                                    }`}
                                >
                                    <Icon
                                        className={`w-4 h-4 stroke-[1.75] ${
                                            item.isActive ? "text-blue-600" : "text-slate-400"
                                        }`}
                                    />
                                    <span>{item.label}</span>
                                </Link>
                            );
                        })}
                    </nav>
                </div>
            </div>

            {/* Bottom info */}
            <div className="p-4 border-t border-slate-100">
                <div className="px-3 py-2 rounded-lg bg-slate-50 text-[11px] text-slate-400">
                    <p className="font-medium text-slate-600">Digital Wallet v1.0</p>
                    <p className="text-[10px] text-slate-400 mt-0.5">{t("secureWalletSystem")}</p>
                </div>
            </div>
        </aside>
    );
}
