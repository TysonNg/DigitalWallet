"use client";

import { useState } from "react";
import { X, ArrowUpRight, Landmark } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { formatVND } from "@/lib/utils";
import { withdrawMoneyAction } from "../actions/wallet.actions";
import { toast } from "sonner";

interface WithdrawModalProps {
    isOpen: boolean;
    onClose: () => void;
    walletId: string;
    currentBalance: number;
    onSuccess?: () => void;
}

export function WithdrawModal({
    isOpen,
    onClose,
    walletId,
    currentBalance,
    onSuccess,
}: WithdrawModalProps) {
    const [amount, setAmount] = useState<number>(500000);
    const [bankNumber, setBankNumber] = useState<string>("1029384756");
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);

        if (amount <= 0) {
            setError("Số tiền rút phải lớn hơn 0");
            return;
        }

        if (amount > currentBalance) {
            setError("Số dư khả dụng không đủ để thực hiện giao dịch này");
            return;
        }

        setIsLoading(true);
        try {
            const res = await withdrawMoneyAction(walletId, amount);
            if (res.success) {
                toast.success(`Rút thành công ${formatVND(amount)} về tài khoản ngân hàng!`);
                onSuccess?.();
                onClose();
            } else {
                setError(res.message);
                toast.error(res.message);
            }
        } catch {
            setError("Có lỗi xảy ra khi rút tiền");
            toast.error("Có lỗi xảy ra khi rút tiền");
        } finally {
            setIsLoading(false);
        }
    };

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
                    <div className="w-9 h-9 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
                        <ArrowUpRight className="w-4.5 h-4.5 stroke-[2]" />
                    </div>
                    <div>
                        <h3 className="text-base font-bold text-slate-900 tracking-tight">Rút tiền về ngân hàng</h3>
                        <p className="text-xs text-slate-500">
                            Khả dụng:{" "}
                            <span className="font-semibold text-slate-800">
                                {formatVND(currentBalance)}
                            </span>
                        </p>
                    </div>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                    {error && (
                        <div className="p-2.5 bg-rose-50 border border-rose-200 text-xs text-rose-700 rounded-lg">
                            {error}
                        </div>
                    )}

                    <div>
                        <div className="flex justify-between items-center mb-1">
                            <Label htmlFor="withdraw-amount">Số tiền muốn rút</Label>
                            <button
                                type="button"
                                onClick={() => setAmount(currentBalance)}
                                className="text-xs font-medium text-blue-600 hover:underline cursor-pointer"
                            >
                                Rút tối đa
                            </button>
                        </div>
                        <div className="relative">
                            <Input
                                id="withdraw-amount"
                                type="number"
                                min={50000}
                                max={currentBalance}
                                step={10000}
                                value={amount || ""}
                                onChange={(e) => setAmount(Number(e.target.value))}
                                className="text-base font-bold text-slate-900 pr-12"
                                placeholder="0"
                                required
                            />
                            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-slate-400">
                                VND
                            </span>
                        </div>
                    </div>

                    <div>
                        <Label>Tài khoản nhận tiền</Label>
                        <div className="flex items-center space-x-3 p-2.5 mt-1 rounded-lg border border-slate-200 bg-slate-50/70">
                            <div className="w-7 h-7 rounded-md bg-white border border-slate-200 flex items-center justify-center text-slate-600">
                                <Landmark className="w-3.5 h-3.5 stroke-[1.75]" />
                            </div>
                            <div className="flex-1 text-xs">
                                <p className="font-semibold text-slate-800">
                                    Vietcombank (Chi nhánh TP.HCM)
                                </p>
                                <p className="text-slate-400 font-mono text-[11px]">STK: {bankNumber}</p>
                            </div>
                        </div>
                    </div>

                    <div className="p-2.5 bg-slate-50 rounded-lg space-y-1 text-xs text-slate-600 border border-slate-100">
                        <div className="flex justify-between">
                            <span>Phí rút tiền:</span>
                            <span className="font-semibold text-emerald-600">Miễn phí 0 đ</span>
                        </div>
                        <div className="flex justify-between">
                            <span>Thời gian xử lý:</span>
                            <span className="font-medium text-slate-700">Tức thời (24/7)</span>
                        </div>
                    </div>

                    <Button
                        type="submit"
                        variant="primary"
                        className="w-full mt-2"
                        isLoading={isLoading}
                        disabled={currentBalance <= 0}
                    >
                        Xác nhận rút {amount > 0 ? formatVND(amount) : ""}
                    </Button>
                </form>
            </div>
        </div>
    );
}
