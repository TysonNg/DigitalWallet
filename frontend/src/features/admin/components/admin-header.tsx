"use client";

import { User } from "@/features/auth/types/auth.types";
import { Link } from "@/i18n/navigation";
import { ShieldAlert, RefreshCw, Wallet } from "lucide-react";

interface AdminHeaderProps {
    currentUser?: User;
    isRefreshing?: boolean;
    onRefresh: () => void;
}

export function AdminHeader({ currentUser, isRefreshing, onRefresh }: AdminHeaderProps) {
    return (
        <header className="bg-white dark:bg-slate-900 border-b border-slate-200/80 dark:border-slate-800 sticky top-0 z-40 backdrop-blur-md bg-white/90 dark:bg-slate-900/90">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
                {/* Brand Logo & Console Tag */}
                <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-600 flex items-center justify-center text-white shadow-md shadow-indigo-500/20">
                        <ShieldAlert className="w-5 h-5" />
                    </div>
                    <div>
                        <div className="flex items-center gap-2">
                            <span className="font-bold text-base text-slate-900 dark:text-white tracking-tight">
                                DigitalWallet
                            </span>
                            <span className="px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider rounded-md bg-indigo-50 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800">
                                Backoffice Console
                            </span>
                        </div>
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                            Cổng vận hành & Quản trị hệ thống thời gian thực
                        </p>
                    </div>
                </div>

                {/* Actions & Profile */}
                <div className="flex items-center gap-3">
                    <button
                        type="button"
                        onClick={onRefresh}
                        disabled={isRefreshing}
                        className="p-2 rounded-xl border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300 transition-colors cursor-pointer"
                        title="Làm mới dữ liệu"
                    >
                        <RefreshCw className={`w-4 h-4 ${isRefreshing ? "animate-spin" : ""}`} />
                    </button>

                    <Link
                        href="/dashboard"
                        className="flex items-center gap-2 px-3 py-2 text-xs font-semibold text-slate-700 dark:text-slate-200 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 rounded-xl transition-colors"
                    >
                        <Wallet className="w-4 h-4 text-slate-500" />
                        <span className="hidden sm:inline">Về Ví cá nhân</span>
                    </Link>

                    {/* Admin User Info */}
                    <div className="pl-3 border-l border-slate-200 dark:border-slate-800 flex items-center gap-2.5">
                        <div className="w-8 h-8 rounded-full bg-indigo-600 text-white font-bold text-xs flex items-center justify-center">
                            {currentUser?.fullName
                                ? currentUser.fullName
                                      .split(" ")
                                      .map((n) => n[0])
                                      .slice(0, 2)
                                      .join("")
                                      .toUpperCase()
                                : "AD"}
                        </div>
                        <div className="hidden md:block text-left">
                            <div className="text-xs font-semibold text-slate-900 dark:text-white leading-tight">
                                {currentUser?.fullName || "Quản trị viên"}
                            </div>
                            <div className="text-[11px] text-slate-400 truncate max-w-[140px]">
                                {currentUser?.email}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </header>
    );
}
