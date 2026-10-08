"use client";

import { X, User as UserIcon, Mail, Phone, Calendar } from "lucide-react";
import { User } from "@/features/auth/types/auth.types";
import { Button } from "@/components/ui/button";

interface SettingsModalProps {
    isOpen: boolean;
    onClose: () => void;
    user: User;
}

export function SettingsModal({ isOpen, onClose, user }: SettingsModalProps) {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto">
            <div
                className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs transition-opacity duration-150"
                onClick={onClose}
            />

            <div className="relative w-full max-w-sm sm:max-w-md bg-white rounded-2xl shadow-xl border border-slate-200/90 p-6 sm:p-7 z-10 duration-150 my-auto">
                <button
                    onClick={onClose}
                    className="absolute top-4 right-4 p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
                >
                    <X className="w-4 h-4 stroke-[1.75]" />
                </button>

                <div className="flex items-center space-x-3 mb-5 pr-6">
                    <div className="w-9 h-9 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
                        <UserIcon className="w-4.5 h-4.5 stroke-[1.75]" />
                    </div>
                    <div>
                        <h3 className="text-base font-bold text-slate-900 tracking-tight">Hồ sơ tài khoản</h3>
                        <p className="text-xs text-slate-500">Thông tin cá nhân liên kết ví điện tử</p>
                    </div>
                </div>

                <div className="space-y-3.5">
                    <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 space-y-2.5">
                        <div className="flex items-center justify-between text-xs pb-2 border-b border-slate-200/70">
                            <span className="text-slate-500 flex items-center gap-1.5">
                                <UserIcon className="w-3.5 h-3.5 text-slate-400 stroke-[1.75]" /> Họ và tên:
                            </span>
                            <span className="font-semibold text-slate-900">
                                {user.fullName || "Nguyễn Minh Anh"}
                            </span>
                        </div>

                        <div className="flex items-center justify-between text-xs pb-2 border-b border-slate-200/70">
                            <span className="text-slate-500 flex items-center gap-1.5">
                                <Mail className="w-3.5 h-3.5 text-slate-400 stroke-[1.75]" /> Email:
                            </span>
                            <span className="font-medium text-slate-800">{user.email}</span>
                        </div>

                        <div className="flex items-center justify-between text-xs pb-2 border-b border-slate-200/70">
                            <span className="text-slate-500 flex items-center gap-1.5">
                                <Phone className="w-3.5 h-3.5 text-slate-400 stroke-[1.75]" /> Số điện thoại:
                            </span>
                            <span className="font-medium text-slate-800">
                                {user.phoneNumber || "0912345678"}
                            </span>
                        </div>

                        <div className="flex items-center justify-between text-xs">
                            <span className="text-slate-500 flex items-center gap-1.5">
                                <Calendar className="w-3.5 h-3.5 text-slate-400 stroke-[1.75]" /> Ngày sinh:
                            </span>
                            <span className="font-medium text-slate-800">
                                {user.dateOfBirth || "2000-01-01"}
                            </span>
                        </div>
                    </div>
                </div>

                <div className="mt-5">
                    <Button
                        type="button"
                        variant="secondary"
                        onClick={onClose}
                        className="w-full text-xs"
                    >
                        Đóng cửa sổ
                    </Button>
                </div>
            </div>
        </div>
    );
}
