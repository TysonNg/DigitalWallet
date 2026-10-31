"use client";

import { Send, ArrowRight, ShieldCheck } from "lucide-react";
import { DraftTransferPayload } from "../types/ai.types";

interface DraftTransferCardProps {
    payload: DraftTransferPayload;
    onConfirm: (payload: DraftTransferPayload) => void;
}

export function DraftTransferCard({ payload, onConfirm }: DraftTransferCardProps) {
    const formattedAmount = Number(payload.amount).toLocaleString("vi-VN");

    return (
        <div className="mt-3 p-4 bg-gradient-to-br from-indigo-50/90 to-blue-50/80 border border-indigo-200/90 rounded-2xl shadow-sm text-slate-800">
            <div className="flex items-center justify-between pb-3 border-b border-indigo-100">
                <div className="flex items-center gap-2">
                    <div className="w-8 h-8 rounded-xl bg-indigo-600 text-white flex items-center justify-center shadow-sm shadow-indigo-200">
                        <Send className="w-4 h-4" />
                    </div>
                    <div>
                        <h4 className="text-xs font-bold uppercase tracking-wider text-indigo-900">
                            Phiếu Chuyển Tiền Nháp
                        </h4>
                        <p className="text-[11px] text-slate-500">Được soạn tự động bởi Trợ lý AI</p>
                    </div>
                </div>
                <span className="px-2 py-0.5 text-[10px] font-semibold bg-amber-100 text-amber-800 rounded-full border border-amber-200">
                    Chờ bạn duyệt
                </span>
            </div>

            <div className="py-3 space-y-2 text-xs">
                <div className="flex justify-between items-center">
                    <span className="text-slate-500">Người nhận:</span>
                    <span className="font-semibold text-slate-900 text-right">
                        {payload.recipientName} ({payload.recipientPhoneOrEmail})
                    </span>
                </div>

                <div className="flex justify-between items-center">
                    <span className="text-slate-500">Số tiền:</span>
                    <span className="font-bold text-base text-indigo-600">
                        {formattedAmount} VNĐ
                    </span>
                </div>

                <div className="flex justify-between items-center">
                    <span className="text-slate-500">Phí giao dịch:</span>
                    <span className="font-medium text-emerald-600">0 VNĐ (Miễn phí)</span>
                </div>

                <div className="flex justify-between items-start">
                    <span className="text-slate-500">Lời nhắn:</span>
                    <span className="font-normal text-slate-700 italic max-w-[200px] text-right truncate">
                        &quot;{payload.note}&quot;
                    </span>
                </div>
            </div>

            <div className="pt-2 border-t border-indigo-100">
                <div className="flex items-center gap-1.5 mb-2.5 text-[11px] text-slate-500">
                    <ShieldCheck className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                    <span>Yêu cầu nhập mã PIN 6 số an toàn trước khi trừ tiền thật</span>
                </div>

                <button
                    onClick={() => onConfirm(payload)}
                    className="w-full flex items-center justify-center gap-2 py-2.5 px-4 bg-gradient-to-r from-indigo-600 to-blue-600 hover:from-indigo-700 hover:to-blue-700 text-white font-medium text-xs rounded-xl shadow-md shadow-indigo-200 transition-all cursor-pointer active:scale-[0.98]"
                >
                    <span>Xác nhận & Nhập mã PIN</span>
                    <ArrowRight className="w-4 h-4" />
                </button>
            </div>
        </div>
    );
}
