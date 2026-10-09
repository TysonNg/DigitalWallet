"use client";

import { useState, useRef, useEffect } from "react";
import {
    Search,
    Bell,
    ChevronDown,
    LogOut,
    Copy,
    Check,
    X,
} from "lucide-react";
import { User } from "@/features/auth/types/auth.types";
import { logoutAction } from "@/features/auth/actions/auth.actions";
import { toast } from "sonner";

import { NotificationItem } from "../types/notification.types";

interface TopHeaderProps {
    user: User;
    walletId?: string;
    searchQuery: string;
    onSearchChange: (query: string) => void;
    notifications?: NotificationItem[];
    hasUnread?: boolean;
    onOpenNotifications?: () => void;
}

export function TopHeader({
    user,
    walletId,
    searchQuery,
    onSearchChange,
    notifications = [],
    hasUnread = false,
    onOpenNotifications,
}: TopHeaderProps) {
    const [isProfileOpen, setIsProfileOpen] = useState(false);
    const [isNotifOpen, setIsNotifOpen] = useState(false);
    const [copied, setCopied] = useState(false);

    const profileRef = useRef<HTMLDivElement>(null);
    const notifRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleClickOutside = (e: MouseEvent) => {
            if (profileRef.current && !profileRef.current.contains(e.target as Node)) {
                setIsProfileOpen(false);
            }
            if (notifRef.current && !notifRef.current.contains(e.target as Node)) {
                setIsNotifOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    const handleCopyWalletId = () => {
        if (!walletId) return;
        navigator.clipboard.writeText(walletId);
        setCopied(true);
        toast.success("Đã sao chép mã ví của bạn!");
        setTimeout(() => setCopied(false), 2000);
    };

    const handleLogout = async () => {
        await logoutAction();
        window.location.href = "/";
    };

    const handleToggleNotifications = () => {
        const nextState = !isNotifOpen;
        setIsNotifOpen(nextState);
        if (nextState && onOpenNotifications) {
            onOpenNotifications();
        }
    };

    const formatNotificationTime = (dateStr: string) => {
        try {
            const date = new Date(dateStr);
            return date.toLocaleTimeString("vi-VN", {
                hour: "2-digit",
                minute: "2-digit",
            });
        } catch {
            return "";
        }
    };

    return (
        <header className="sticky top-0 z-30 w-full h-14 bg-white/95 backdrop-blur-md border-b border-slate-200/80 px-4 sm:px-8 flex items-center justify-between gap-4">
            {/* Search Bar */}
            <div className="flex-1 max-w-md">
                <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400 stroke-[1.75]" />
                    <input
                        type="text"
                        placeholder="Tìm kiếm giao dịch, người nhận, ..."
                        value={searchQuery}
                        onChange={(e) => onSearchChange(e.target.value)}
                        className="w-full bg-slate-50 hover:bg-slate-100/60 focus:bg-white text-xs text-slate-800 placeholder-slate-400 pl-8.5 pr-8 py-1.5 rounded-lg border border-slate-200/90 focus:outline-none focus:ring-1 focus:ring-blue-600 focus:border-blue-600 transition-all"
                    />
                    {searchQuery && (
                        <button
                            type="button"
                            onClick={() => onSearchChange("")}
                            className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 cursor-pointer"
                        >
                            <X className="w-3.5 h-3.5" />
                        </button>
                    )}
                </div>
            </div>

            {/* Right Action Icons: Notification & Profile */}
            <div className="flex items-center space-x-2">
                {/* Notification Bell */}
                <div className="relative" ref={notifRef}>
                    <button
                        type="button"
                        onClick={handleToggleNotifications}
                        className="relative p-2 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-50 transition-colors cursor-pointer"
                        aria-label="Thông báo"
                    >
                        <Bell className="w-4 h-4 stroke-[1.75]" />
                        {hasUnread && (
                            <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-rose-500 ring-2 ring-white animate-pulse" />
                        )}
                    </button>

                    {/* Notification Popover */}
                    {isNotifOpen && (
                        <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-lg border border-slate-200/80 p-3.5 z-50 animate-in fade-in zoom-in-95 duration-100">
                            <div className="flex items-center justify-between pb-2.5 border-b border-slate-100 mb-2">
                                <span className="font-semibold text-xs text-slate-900">
                                    Thông báo giao dịch
                                </span>
                                <span className="text-[10px] font-medium text-slate-500 bg-slate-50 px-2 py-0.5 rounded-md border border-slate-100">
                                    {notifications.length > 0 ? `${notifications.length} tin` : "Mới nhất"}
                                </span>
                            </div>

                            <div className="space-y-1.5 max-h-64 overflow-y-auto">
                                {notifications.length === 0 ? (
                                    <div className="py-6 text-center text-slate-400 text-xs">
                                        Chưa có thông báo nào
                                    </div>
                                ) : (
                                    notifications.map((item) => (
                                        <div
                                            key={item.id}
                                            className={`p-2 rounded-lg transition-colors text-xs ${
                                                !item.isRead
                                                    ? "bg-blue-50/60 hover:bg-blue-50/90 border border-blue-100/60"
                                                    : "bg-slate-50/70 hover:bg-slate-100/60"
                                            }`}
                                        >
                                            <div className="flex justify-between items-center mb-0.5">
                                                <p className="font-semibold text-slate-800 text-[11px] truncate">
                                                    {item.title}
                                                </p>
                                                <span className="text-[10px] text-slate-400 ml-2 whitespace-nowrap">
                                                    {formatNotificationTime(item.createdAt)}
                                                </span>
                                            </div>
                                            <p className="text-slate-500 text-[11px] leading-relaxed">
                                                {item.content}
                                            </p>
                                        </div>
                                    ))
                                )}
                            </div>
                        </div>
                    )}
                </div>

                {/* Profile Pill Dropdown */}
                <div className="relative" ref={profileRef}>
                    <button
                        type="button"
                        onClick={() => setIsProfileOpen(!isProfileOpen)}
                        className="flex items-center space-x-2 p-1 sm:px-2 rounded-lg hover:bg-slate-50 transition-colors cursor-pointer"
                    >
                        <div className="w-7 h-7 rounded-full bg-slate-800 text-white font-medium text-[11px] flex items-center justify-center">
                            {user.fullName ? user.fullName.slice(0, 2).toUpperCase() : "NA"}
                        </div>
                        <span className="hidden sm:inline font-medium text-xs text-slate-700">
                            {user.fullName || "Nguyễn Minh Anh"}
                        </span>
                        <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
                    </button>

                    {/* Profile Dropdown Menu */}
                    {isProfileOpen && (
                        <div className="absolute right-0 mt-2 w-60 bg-white rounded-xl shadow-lg border border-slate-200/80 p-2.5 z-50 animate-in fade-in zoom-in-95 duration-100">
                            <div className="p-2 rounded-lg bg-slate-50 border border-slate-100 mb-1.5">
                                <p className="font-semibold text-xs text-slate-900">
                                    {user.fullName || "Nguyễn Minh Anh"}
                                </p>
                                <p className="text-[11px] text-slate-500 truncate">{user.email}</p>
                                {walletId && (
                                    <button
                                        type="button"
                                        onClick={handleCopyWalletId}
                                        className="mt-1.5 flex items-center justify-between w-full text-[10px] bg-white border border-slate-200 px-2 py-1 rounded-md text-slate-600 hover:text-blue-600 cursor-pointer"
                                    >
                                        <span className="truncate">ID: {walletId.slice(0, 10)}...</span>
                                        {copied ? (
                                            <Check className="w-3 h-3 text-emerald-600" />
                                        ) : (
                                            <Copy className="w-3 h-3" />
                                        )}
                                    </button>
                                )}
                            </div>

                            <div className="pt-1 border-t border-slate-100">
                                <button
                                    type="button"
                                    onClick={handleLogout}
                                    className="w-full flex items-center space-x-2 px-2.5 py-1.5 text-xs text-rose-600 hover:bg-rose-50 rounded-md transition-colors cursor-pointer font-medium"
                                >
                                    <LogOut className="w-3.5 h-3.5 stroke-[1.75]" />
                                    <span>Đăng xuất ví</span>
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </header>
    );
}
