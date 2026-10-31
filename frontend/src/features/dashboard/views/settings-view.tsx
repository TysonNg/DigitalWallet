"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import {
    User as UserIcon,
    Mail,
    Phone,
    Shield,
    KeyRound,
    CheckCircle2,
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
import {
    useMyWalletQuery,
    useSetupPinMutation,
    useChangePinMutation,
    ActionError,
} from "@/features/wallet/hooks/use-wallet";
import { toast } from "sonner";

interface SettingsViewProps {
    user: User;
}

export function SettingsView({ user }: SettingsViewProps) {
    const t = useTranslations("dashboard");
    const apiErr = useTranslations("apiErrors");

    const { data: myWallet, refetch: refetchWallet } = useMyWalletQuery();
    const setupPinMutation = useSetupPinMutation();
    const changePinMutation = useChangePinMutation();

    // Setup PIN form state
    const [setupPin, setSetupPin] = useState("");
    const [setupPinConfirm, setSetupPinConfirm] = useState("");
    const [setupPinError, setSetupPinError] = useState<string | null>(null);
    const [isSavingPin, setIsSavingPin] = useState(false);

    // Change PIN form state
    const [changeOldPin, setChangeOldPin] = useState("");
    const [changeNewPin, setChangeNewPin] = useState("");
    const [changeNewPinConfirm, setChangeNewPinConfirm] = useState("");
    const [changePinError, setChangePinError] = useState<string | null>(null);
    const [isChangingPin, setIsChangingPin] = useState(false);
    const [showChangePinForm, setShowChangePinForm] = useState(false);

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

    const validatePinFormat = (pin: string) => {
        if (!/^[0-9]{6}$/.test(pin)) {
            return "Mã PIN phải bao gồm đúng 6 chữ số";
        }
        return null;
    };

    const handleSetupPinSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setSetupPinError(null);

        const formatError = validatePinFormat(setupPin);
        if (formatError) {
            setSetupPinError(formatError);
            return;
        }

        if (setupPin !== setupPinConfirm) {
            setSetupPinError("Mã PIN xác nhận không khớp");
            return;
        }

        setIsSavingPin(true);
        try {
            await setupPinMutation.mutateAsync({ pin: setupPin });
            toast.success("Thiết lập mã PIN giao dịch thành công!");
            setSetupPin("");
            setSetupPinConfirm("");
            refetchWallet();
        } catch (err: unknown) {
            let msg = "Thiết lập mã PIN thất bại";
            if (err instanceof ActionError && err.errorCode && apiErr.has(err.errorCode)) {
                msg = apiErr(err.errorCode);
            } else if (err instanceof Error) {
                msg = err.message;
            }
            setSetupPinError(msg);
            toast.error(msg);
        } finally {
            setIsSavingPin(false);
        }
    };

    const handleChangePinSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setChangePinError(null);

        if (!/^[0-9]{6}$/.test(changeOldPin)) {
            setChangePinError("Mã PIN hiện tại phải gồm đúng 6 chữ số");
            return;
        }

        const formatError = validatePinFormat(changeNewPin);
        if (formatError) {
            setChangePinError(formatError);
            return;
        }

        if (changeNewPin !== changeNewPinConfirm) {
            setChangePinError("Mã PIN mới xác nhận không khớp");
            return;
        }

        if (changeOldPin === changeNewPin) {
            setChangePinError("Mã PIN mới không được trùng với mã PIN hiện tại");
            return;
        }

        setIsChangingPin(true);
        try {
            await changePinMutation.mutateAsync({ oldPin: changeOldPin, newPin: changeNewPin });
            toast.success("Đổi mã PIN giao dịch thành công!");
            setChangeOldPin("");
            setChangeNewPin("");
            setChangeNewPinConfirm("");
            setShowChangePinForm(false);
            refetchWallet();
        } catch (err: unknown) {
            let msg = "Đổi mã PIN thất bại";
            if (err instanceof ActionError && err.errorCode && apiErr.has(err.errorCode)) {
                msg = apiErr(err.errorCode);
            } else if (err instanceof Error) {
                msg = err.message;
            }
            setChangePinError(msg);
            toast.error(msg);
        } finally {
            setIsChangingPin(false);
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

                {/* 3. Wallet Transaction PIN Card */}
                <div className="bg-white rounded-2xl border border-slate-200/90 p-6 sm:p-7 shadow-xs space-y-6 md:col-span-2">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-100 gap-3">
                        <div className="flex items-center space-x-3">
                            <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
                                <Shield className="w-5 h-5 stroke-[1.75]" />
                            </div>
                            <div>
                                <h2 className="text-sm font-bold text-slate-900">
                                    Mã PIN Giao Dịch Ví (Transaction PIN)
                                </h2>
                                <p className="text-xs text-slate-500">
                                    Mã PIN 6 số bảo vệ số dư khi thực hiện giao dịch chuyển tiền hoặc rút tiền
                                </p>
                            </div>
                        </div>

                        <div>
                            {myWallet?.hasPin ? (
                                <span className="inline-flex items-center gap-1.5 text-xs text-emerald-700 font-semibold bg-emerald-50 px-3 py-1 rounded-full border border-emerald-100">
                                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                                    Đã kích hoạt bảo vệ
                                </span>
                            ) : (
                                <span className="inline-flex items-center gap-1.5 text-xs text-amber-700 font-semibold bg-amber-50 px-3 py-1 rounded-full border border-amber-200/80">
                                    <AlertCircle className="w-3.5 h-3.5 text-amber-600" />
                                    Chưa thiết lập mã PIN
                                </span>
                            )}
                        </div>
                    </div>

                    {!myWallet?.hasPin ? (
                        /* Setup PIN Form */
                        <form onSubmit={handleSetupPinSubmit} className="space-y-4 max-w-lg">
                            <div className="p-3.5 rounded-xl bg-blue-50/60 border border-blue-100 text-xs text-blue-800 space-y-1">
                                <p className="font-semibold">Bảo vệ ví của bạn</p>
                                <p className="text-blue-700/90">
                                    Thiết lập mã PIN 6 số tùy chọn để xác thực các giao dịch chuyển tiền và rút tiền nhạy cảm.
                                </p>
                            </div>

                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                <div className="space-y-1.5">
                                    <Label className="text-xs font-semibold text-slate-700">
                                        Mã PIN mới (6 chữ số)
                                    </Label>
                                    <Input
                                        type="password"
                                        inputMode="numeric"
                                        maxLength={6}
                                        value={setupPin}
                                        onChange={(e) => setSetupPin(e.target.value.replace(/\D/g, "").slice(0, 6))}
                                        placeholder="••••••"
                                        className="text-xs font-mono tracking-widest"
                                        required
                                    />
                                </div>

                                <div className="space-y-1.5">
                                    <Label className="text-xs font-semibold text-slate-700">
                                        Xác nhận mã PIN
                                    </Label>
                                    <Input
                                        type="password"
                                        inputMode="numeric"
                                        maxLength={6}
                                        value={setupPinConfirm}
                                        onChange={(e) => setSetupPinConfirm(e.target.value.replace(/\D/g, "").slice(0, 6))}
                                        placeholder="••••••"
                                        className="text-xs font-mono tracking-widest"
                                        required
                                    />
                                </div>
                            </div>

                            {setupPinError && (
                                <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-xs text-rose-600 flex items-center space-x-2">
                                    <AlertCircle className="w-4 h-4 shrink-0" />
                                    <span>{setupPinError}</span>
                                </div>
                            )}

                            <div>
                                <Button
                                    type="submit"
                                    variant="primary"
                                    disabled={isSavingPin || setupPin.length !== 6 || setupPinConfirm.length !== 6}
                                    className="text-xs font-semibold h-10 rounded-xl px-5 cursor-pointer shadow-xs"
                                >
                                    {isSavingPin ? "Đang thiết lập..." : "Kích hoạt mã PIN ví"}
                                </Button>
                            </div>
                        </form>
                    ) : (
                        /* Change PIN Section */
                        <div className="space-y-4 max-w-lg">
                            {!showChangePinForm ? (
                                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 p-4 rounded-xl bg-slate-50 border border-slate-100">
                                    <div>
                                        <p className="text-xs font-semibold text-slate-800">
                                            Mã PIN đang hoạt động
                                        </p>
                                        <p className="text-[11px] text-slate-500 mt-0.5">
                                            Mỗi giao dịch chuyển tiền và rút tiền đều yêu cầu xác thực bằng mã PIN này.
                                        </p>
                                    </div>
                                    <Button
                                        type="button"
                                        variant="outline"
                                        onClick={() => setShowChangePinForm(true)}
                                        className="text-xs h-9 rounded-xl cursor-pointer shrink-0"
                                    >
                                        Đổi mã PIN
                                    </Button>
                                </div>
                            ) : (
                                <form onSubmit={handleChangePinSubmit} className="space-y-4">
                                    <div className="space-y-1.5">
                                        <Label className="text-xs font-semibold text-slate-700">
                                            Mã PIN hiện tại
                                        </Label>
                                        <Input
                                            type="password"
                                            inputMode="numeric"
                                            maxLength={6}
                                            value={changeOldPin}
                                            onChange={(e) => setChangeOldPin(e.target.value.replace(/\D/g, "").slice(0, 6))}
                                            placeholder="••••••"
                                            className="text-xs font-mono tracking-widest max-w-xs"
                                            required
                                        />
                                    </div>

                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                        <div className="space-y-1.5">
                                            <Label className="text-xs font-semibold text-slate-700">
                                                Mã PIN mới (6 chữ số)
                                            </Label>
                                            <Input
                                                type="password"
                                                inputMode="numeric"
                                                maxLength={6}
                                                value={changeNewPin}
                                                onChange={(e) => setChangeNewPin(e.target.value.replace(/\D/g, "").slice(0, 6))}
                                                placeholder="••••••"
                                                className="text-xs font-mono tracking-widest"
                                                required
                                            />
                                        </div>

                                        <div className="space-y-1.5">
                                            <Label className="text-xs font-semibold text-slate-700">
                                                Xác nhận mã PIN mới
                                            </Label>
                                            <Input
                                                type="password"
                                                inputMode="numeric"
                                                maxLength={6}
                                                value={changeNewPinConfirm}
                                                onChange={(e) => setChangeNewPinConfirm(e.target.value.replace(/\D/g, "").slice(0, 6))}
                                                placeholder="••••••"
                                                className="text-xs font-mono tracking-widest"
                                                required
                                            />
                                        </div>
                                    </div>

                                    {changePinError && (
                                        <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-xs text-rose-600 flex items-center space-x-2">
                                            <AlertCircle className="w-4 h-4 shrink-0" />
                                            <span>{changePinError}</span>
                                        </div>
                                    )}

                                    <div className="flex items-center gap-3 pt-1">
                                        <Button
                                            type="submit"
                                            variant="primary"
                                            disabled={
                                                isChangingPin ||
                                                changeOldPin.length !== 6 ||
                                                changeNewPin.length !== 6 ||
                                                changeNewPinConfirm.length !== 6
                                            }
                                            className="text-xs font-semibold h-10 rounded-xl px-5 cursor-pointer shadow-xs"
                                        >
                                            {isChangingPin ? "Đang lưu..." : "Lưu thay đổi mã PIN"}
                                        </Button>
                                        <Button
                                            type="button"
                                            variant="outline"
                                            onClick={() => {
                                                setShowChangePinForm(false);
                                                setChangePinError(null);
                                                setChangeOldPin("");
                                                setChangeNewPin("");
                                                setChangeNewPinConfirm("");
                                            }}
                                            className="text-xs h-10 rounded-xl cursor-pointer"
                                        >
                                            Hủy
                                        </Button>
                                    </div>
                                </form>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </main>
    );
}
