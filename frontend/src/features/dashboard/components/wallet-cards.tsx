"use client";

import {
    Wallet,
    PiggyBank,
    Luggage,
    MoreHorizontal,
    Plus,
    ArrowUpRight,
    Send,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { formatVND } from "@/lib/utils";

interface WalletCardsProps {
    mainBalance: number;
    walletId: string;
    onDepositClick: () => void;
    onWithdrawClick: () => void;
    onTransferClick: () => void;
}

export function WalletCards({
    mainBalance,
    onDepositClick,
    onWithdrawClick,
    onTransferClick,
}: WalletCardsProps) {
    return (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            {/* 1. Ví chính */}
            <div className="bg-white rounded-2xl border border-slate-200/90 p-5 sm:p-6 shadow-xs flex flex-col justify-between">
                <div>
                    <div className="flex items-center justify-between mb-3">
                        <div className="flex items-center space-x-2.5">
                            <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
                                <Wallet className="w-4 h-4 stroke-[1.75]" />
                            </div>
                            <span className="font-semibold text-xs text-slate-700">Ví chính</span>
                        </div>
                        <button
                            type="button"
                            className="text-slate-400 hover:text-slate-600 p-1 rounded-md hover:bg-slate-50 transition-colors cursor-pointer"
                        >
                            <MoreHorizontal className="w-4 h-4" />
                        </button>
                    </div>

                    <div className="mt-2">
                        <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
                            {formatVND(mainBalance)}
                        </h2>
                    </div>
                </div>

                <div className="grid grid-cols-3 gap-2 mt-6">
                    <Button
                        size="sm"
                        variant="primary"
                        onClick={onDepositClick}
                        className="text-xs h-8.5 rounded-lg flex items-center justify-center gap-1 cursor-pointer"
                    >
                        <Plus className="w-3.5 h-3.5 stroke-[2]" /> Nạp tiền
                    </Button>

                    <Button
                        variant="outline"
                        size="sm"
                        onClick={onWithdrawClick}
                        className="text-xs h-8.5 rounded-lg flex items-center justify-center gap-1 cursor-pointer"
                    >
                        <ArrowUpRight className="w-3.5 h-3.5 stroke-[2]" /> Rút tiền
                    </Button>

                    <Button
                        variant="outline"
                        size="sm"
                        onClick={onTransferClick}
                        className="text-xs h-8.5 rounded-lg flex items-center justify-center gap-1 cursor-pointer"
                    >
                        <Send className="w-3.5 h-3.5 stroke-[2]" /> Chuyển tiền
                    </Button>
                </div>
            </div>

            {/* 2. Ví tiết kiệm */}
            <div className="bg-white rounded-2xl border border-slate-200/90 p-5 sm:p-6 shadow-xs flex flex-col justify-between">
                <div>
                    <div className="flex items-center justify-between mb-3">
                        <div className="flex items-center space-x-2.5">
                            <div className="w-8 h-8 rounded-lg bg-sky-50 text-sky-600 flex items-center justify-center">
                                <PiggyBank className="w-4 h-4 stroke-[1.75]" />
                            </div>
                            <span className="font-semibold text-xs text-slate-700">
                                Ví tiết kiệm
                            </span>
                        </div>
                        <button
                            type="button"
                            className="text-slate-400 hover:text-slate-600 p-1 rounded-md hover:bg-slate-50 transition-colors cursor-pointer"
                        >
                            <MoreHorizontal className="w-4 h-4" />
                        </button>
                    </div>

                    <div className="mt-2">
                        <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
                            28.300.000 đ
                        </h2>
                    </div>
                </div>

                <div className="mt-6 pt-2">
                    <p className="text-xs text-slate-500 font-normal">
                        Lãi suất hiện tại <span className="font-semibold text-slate-800">3,5%/năm</span>
                    </p>
                </div>
            </div>

            {/* 3. Ví du lịch */}
            <div className="bg-white rounded-2xl border border-slate-200/90 p-5 sm:p-6 shadow-xs flex flex-col justify-between">
                <div>
                    <div className="flex items-center justify-between mb-3">
                        <div className="flex items-center space-x-2.5">
                            <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-500 flex items-center justify-center">
                                <Luggage className="w-4 h-4 stroke-[1.75]" />
                            </div>
                            <span className="font-semibold text-xs text-slate-700">Ví du lịch</span>
                        </div>
                        <button
                            type="button"
                            className="text-slate-400 hover:text-slate-600 p-1 rounded-md hover:bg-slate-50 transition-colors cursor-pointer"
                        >
                            <MoreHorizontal className="w-4 h-4" />
                        </button>
                    </div>

                    <div className="mt-2">
                        <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
                            5.720.000 đ
                        </h2>
                    </div>
                </div>

                <div className="mt-6 pt-2">
                    <p className="text-xs text-slate-500 font-normal">
                        Dành cho những chuyến đi
                    </p>
                </div>
            </div>
        </div>
    );
}
