"use client";

import { useTranslations } from "next-intl";

import { Home, ArrowLeftRight, CreditCard, Clock, Settings, BookUser } from "lucide-react";

interface SidebarProps {
    activeTab: "overview" | "transfer" | "deposit-withdraw" | "beneficiaries" | "history" | "settings";
    onTabSelect: (
        tab: "overview" | "transfer" | "deposit-withdraw" | "beneficiaries" | "history" | "settings"
    ) => void;
}

export function Sidebar({ activeTab, onTabSelect }: SidebarProps) {
    const t = useTranslations("dashboard");

    const navItems = [
        {
            id: "overview" as const,
            label: t("overview"),
            icon: Home,
        },
        {
            id: "transfer" as const,
            label: t("transfer"),
            icon: ArrowLeftRight,
        },
        {
            id: "deposit-withdraw" as const,
            label: t("depositWithdraw"),
            icon: CreditCard,
        },
        {
            id: "beneficiaries" as const,
            label: t("beneficiaries"),
            icon: BookUser,
        },
        {
            id: "history" as const,
            label: t("transactionHistory"),
            icon: Clock,
        },
        {
            id: "settings" as const,
            label: t("settings"),
            icon: Settings,
        },
    ];

    return (
        <aside className="w-60 bg-white border-r border-slate-200/80 flex flex-col shrink-0 sticky top-0 h-screen select-none">
            {/* Brand Logo & Name */}
            <div className="h-16 flex items-center px-6 border-b border-slate-100">
                <div className="flex items-center space-x-2.5">
                    <div className="w-7 h-7 rounded-lg bg-blue-600 text-white flex items-center justify-center font-bold text-xs">
                        W
                    </div>
                    <span className="font-bold text-sm tracking-tight text-slate-900">
                        Digital Wallet
                    </span>
                </div>
            </div>

            {/* Navigation Menu */}
            <div className="p-3 space-y-1 flex-1">
                {navItems.map((item) => {
                    const Icon = item.icon;
                    const isActive = activeTab === item.id;
                    return (
                        <button
                            key={item.id}
                            type="button"
                            onClick={() => onTabSelect(item.id)}
                            className={`w-full flex items-center space-x-3 px-3.5 py-2.5 rounded-lg text-xs transition-colors cursor-pointer ${
                                isActive
                                    ? "bg-blue-50 text-blue-600 font-semibold"
                                    : "text-slate-600 hover:text-slate-900 hover:bg-slate-50 font-medium"
                            }`}
                        >
                            <Icon
                                className={`w-4 h-4 stroke-[1.75] ${
                                    isActive ? "text-blue-600" : "text-slate-400"
                                }`}
                            />
                            <span>{item.label}</span>
                        </button>
                    );
                })}
            </div>

            {/* Bottom info */}
            <div className="p-4 border-t border-slate-100">
                <div className="px-3 py-2 rounded-lg bg-slate-50 text-[11px] text-slate-400">
                    <p className="font-medium text-slate-600">Digital Wallet v1.0</p>
                    <p className="text-[10px] text-slate-400 mt-0.5"> {t("secureWalletSystem")} </p>
                </div>
            </div>
        </aside>
    );
}
