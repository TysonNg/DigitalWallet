"use client";

import { CreditCard, Wallet, ArrowDownRight, ArrowRight } from "lucide-react";

export function SummaryLimitsCard() {
    return (
        <div className="bg-white rounded-2xl border border-slate-200/90 p-5 sm:p-6 shadow-xs">
            <div className="flex items-center justify-between mb-5">
                <h3 className="font-bold text-base text-slate-900 tracking-tight">
                    Hạn mức & tóm tắt
                </h3>
                <button
                    type="button"
                    className="inline-flex items-center text-xs font-semibold text-blue-600 hover:text-blue-700 hover:underline cursor-pointer"
                >
                    Xem chi tiết <ArrowRight className="w-3.5 h-3.5 ml-1" />
                </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {/* 1. Hạn mức giao dịch */}
                <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 flex flex-col justify-between">
                    <div className="flex items-center space-x-2.5 mb-3">
                        <div className="w-7 h-7 rounded-md bg-blue-50 text-blue-600 flex items-center justify-center">
                            <Wallet className="w-3.5 h-3.5 stroke-[1.75]" />
                        </div>
                        <span className="text-xs font-semibold text-slate-700">
                            Hạn mức giao dịch
                        </span>
                    </div>

                    <div>
                        <div className="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden mb-2">
                            <div className="bg-blue-600 h-full rounded-full w-1/2" />
                        </div>
                        <p className="text-xs font-bold text-slate-900">
                            50.000.000 đ{" "}
                            <span className="text-slate-400 font-normal">/ 100.000.000 đ</span>
                        </p>
                    </div>
                </div>

                {/* 2. Hạn mức rút tiền */}
                <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 flex flex-col justify-between">
                    <div className="flex items-center space-x-2.5 mb-3">
                        <div className="w-7 h-7 rounded-md bg-sky-50 text-sky-600 flex items-center justify-center">
                            <CreditCard className="w-3.5 h-3.5 stroke-[1.75]" />
                        </div>
                        <span className="text-xs font-semibold text-slate-700">
                            Hạn mức rút tiền
                        </span>
                    </div>

                    <div>
                        <div className="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden mb-2">
                            <div className="bg-blue-600 h-full rounded-full w-full" />
                        </div>
                        <p className="text-xs font-bold text-slate-900">
                            20.000.000 đ{" "}
                            <span className="text-slate-400 font-normal">/ 20.000.000 đ</span>
                        </p>
                    </div>
                </div>

                {/* 3. Tổng chi tiêu tháng này */}
                <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 flex flex-col justify-between">
                    <div className="flex items-center space-x-2.5 mb-2">
                        <div className="w-7 h-7 rounded-md bg-indigo-50 text-indigo-600 flex items-center justify-center">
                            <ArrowDownRight className="w-3.5 h-3.5 stroke-[1.75]" />
                        </div>
                        <span className="text-xs font-semibold text-slate-700">
                            Tổng chi tiêu tháng này
                        </span>
                    </div>

                    <div>
                        <p className="text-base font-bold text-slate-900">6.320.000 đ</p>
                        <div className="inline-flex items-center space-x-1 mt-1 text-[11px] font-semibold text-emerald-600">
                            <span>↓ 12%</span>
                            <span className="text-slate-400 font-normal">So với tháng trước</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
