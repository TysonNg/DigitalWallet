"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import {
    User as UserIcon,
    Mail,
    Phone,
    Shield,
    KeyRound,
    Lock,
    CheckCircle2,
    Calendar,
    BadgeCheck,
    Globe,
    AlertCircle,
    Eye,
    EyeOff,
} from "lucide-react";
import { User } from "@/features/auth/types/auth.types";
import { changePasswordAction } from "@/features/auth/actions/auth.actions";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { LanguageSwitcher } from "@/components/language-switcher";
import { toast } from "sonner";

interface SettingsViewProps {
    user: User;
}

export function SettingsView({ user }: SettingsViewProps) {
    const t = useTranslations("dashboard");
    const common = useTranslations("common");

    // Change password form state
    const [currentPassword, setCurrentPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [showCurrent, setShowCurrent] = useState(false);
    const [showNew, setShowNew] = useState(false);
    const [passwordError, setPasswordError] = useState<string | null>(null);
    const [isSaving, setIsSaving] = useState(false);

    const handlePasswordSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setPasswordError(null);

        if (newPassword.length < 6) {
            setPasswordError("Mật khẩu mới phải có ít nhất 6 ký tự");
            return;
        }

        if (newPassword !== confirmPassword) {
            setPasswordError("Mật khẩu xác nhận không khớp");
            return;
        }

        if (currentPassword === newPassword) {
            setPasswordError("Mật khẩu mới không được trùng với mật khẩu hiện tại");
            return;
        }

        setIsSaving(true);
        try {
            const res = await changePasswordAction({
                currentPassword,
                newPassword,
            });

            if (!res.success) {
                setPasswordError(res.message);
                toast.error(res.message);
                return;
            }

            toast.success(res.message || "Cập nhật mật khẩu thành công!");
            setCurrentPassword("");
            setNewPassword("");
            setConfirmPassword("");
        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : "Đã xảy ra lỗi khi đổi mật khẩu";
            setPasswordError(message);
            toast.error("Không thể đổi mật khẩu. Vui lòng thử lại!");
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <main className="flex-1 p-4 sm:p-8 max-w-4xl w-full mx-auto space-y-6">
            {/* Header */}
            <div>
                <h1 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
                    {t("settings")}
                </h1>
                <p className="text-xs sm:text-sm text-slate-500 mt-1">
                    Quản lý thông tin hồ sơ tài khoản, bảo mật và tùy chọn hệ thống
                </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-start">
                {/* 1. Profile Information Card */}
                <div className="bg-white rounded-2xl border border-slate-200/90 p-6 sm:p-7 shadow-xs space-y-6">
                    <div className="flex items-center space-x-3.5 pb-4 border-b border-slate-100">
                        <div className="w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold text-base shadow-2xs">
                            {user.fullName ? user.fullName.slice(0, 2).toUpperCase() : "NA"}
                        </div>
                        <div>
                            <div className="flex items-center space-x-1.5">
                                <h2 className="text-sm font-bold text-slate-900">
                                    {user.fullName || "Người dùng ví"}
                                </h2>
                                <BadgeCheck className="w-4 h-4 text-blue-600" />
                            </div>
                            <p className="text-xs text-slate-500">{user.email}</p>
                        </div>
                    </div>

                    <div className="space-y-4">
                        <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                            {t("accountProfile")}
                        </h3>

                        {/* Full Name */}
                        <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 flex items-center justify-between text-xs">
                            <span className="text-slate-500 flex items-center gap-2">
                                <UserIcon className="w-4 h-4 text-slate-400 stroke-[1.75]" />
                                Họ và tên
                            </span>
                            <span className="font-semibold text-slate-800">
                                {user.fullName || "Chưa cập nhật"}
                            </span>
                        </div>

                        {/* Email */}
                        <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 flex items-center justify-between text-xs">
                            <span className="text-slate-500 flex items-center gap-2">
                                <Mail className="w-4 h-4 text-slate-400 stroke-[1.75]" />
                                Email
                            </span>
                            <span className="font-semibold text-slate-800 font-mono">
                                {user.email}
                            </span>
                        </div>

                        {/* Phone */}
                        <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 flex items-center justify-between text-xs">
                            <span className="text-slate-500 flex items-center gap-2">
                                <Phone className="w-4 h-4 text-slate-400 stroke-[1.75]" />
                                Số điện thoại
                            </span>
                            <span className="font-semibold text-slate-800 font-mono">
                                {user.phoneNumber || "0912 345 678"}
                            </span>
                        </div>

                        {/* Status */}
                        <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 flex items-center justify-between text-xs">
                            <span className="text-slate-500 flex items-center gap-2">
                                <Shield className="w-4 h-4 text-slate-400 stroke-[1.75]" />
                                Trạng thái tài khoản
                            </span>
                            <span className="text-emerald-700 font-semibold bg-emerald-50 px-2.5 py-0.5 rounded-full border border-emerald-100">
                                Đã kích hoạt
                            </span>
                        </div>

                        {/* Language setting */}
                        <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 flex items-center justify-between text-xs">
                            <span className="text-slate-500 flex items-center gap-2">
                                <Globe className="w-4 h-4 text-slate-400 stroke-[1.75]" />
                                Ngôn ngữ hiển thị
                            </span>
                            <LanguageSwitcher />
                        </div>
                    </div>
                </div>

                {/* 2. Security & Password Card */}
                <div className="bg-white rounded-2xl border border-slate-200/90 p-6 sm:p-7 shadow-xs space-y-6">
                    <div className="flex items-center space-x-3 pb-4 border-b border-slate-100">
                        <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
                            <KeyRound className="w-5 h-5 stroke-[1.75]" />
                        </div>
                        <div>
                            <h2 className="text-sm font-bold text-slate-900">Bảo Mật & Mật Khẩu</h2>
                            <p className="text-xs text-slate-500">Cập nhật mật khẩu định kỳ để an toàn</p>
                        </div>
                    </div>

                    <form onSubmit={handlePasswordSubmit} className="space-y-4">
                        {/* Current Password */}
                        <div className="space-y-1.5">
                            <Label className="text-xs font-semibold text-slate-700">
                                Mật khẩu hiện tại
                            </Label>
                            <div className="relative">
                                <Input
                                    type={showCurrent ? "text" : "password"}
                                    value={currentPassword}
                                    onChange={(e) => setCurrentPassword(e.target.value)}
                                    placeholder="••••••••"
                                    className="text-xs pr-9"
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowCurrent(!showCurrent)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 cursor-pointer"
                                >
                                    {showCurrent ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                </button>
                            </div>
                        </div>

                        {/* New Password */}
                        <div className="space-y-1.5">
                            <Label className="text-xs font-semibold text-slate-700">
                                Mật khẩu mới
                            </Label>
                            <div className="relative">
                                <Input
                                    type={showNew ? "text" : "password"}
                                    value={newPassword}
                                    onChange={(e) => setNewPassword(e.target.value)}
                                    placeholder="Tối thiểu 6 ký tự"
                                    className="text-xs pr-9"
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowNew(!showNew)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 cursor-pointer"
                                >
                                    {showNew ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                </button>
                            </div>
                        </div>

                        {/* Confirm New Password */}
                        <div className="space-y-1.5">
                            <Label className="text-xs font-semibold text-slate-700">
                                Xác nhận mật khẩu mới
                            </Label>
                            <Input
                                type="password"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                placeholder="Nhập lại mật khẩu mới"
                                className="text-xs"
                                required
                            />
                        </div>

                        {passwordError && (
                            <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-xs text-rose-600 flex items-center space-x-2">
                                <AlertCircle className="w-4 h-4 shrink-0" />
                                <span>{passwordError}</span>
                            </div>
                        )}

                        <div className="pt-2">
                            <Button
                                type="submit"
                                variant="primary"
                                disabled={isSaving || !currentPassword || !newPassword}
                                className="w-full text-xs font-semibold h-10 rounded-xl cursor-pointer shadow-xs"
                            >
                                {isSaving ? "Đang cập nhật..." : "Lưu thay đổi mật khẩu"}
                            </Button>
                        </div>
                    </form>
                </div>
            </div>
        </main>
    );
}
