"use client";

import { useState } from "react";
import { X, ArrowDownLeft, Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { formatVND } from "@/lib/utils";
import { depositMoneyAction } from "../actions/wallet.actions";
import { toast } from "sonner";

interface DepositModalProps {
    isOpen: boolean;
    onClose: () => void;
    walletId: string;
    onSuccess?: () => void;
}

const PRESET_AMOUNTS = [100000, 200000, 500000, 1000000, 2000000, 5000000];

export function DepositModal({ isOpen, onClose, walletId, onSuccess }: DepositModalProps) {
    const [amount, setAmount] = useState<number>(500000);
    const [source, setSource] = useState<string>("vcb");
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        if (amount <= 0) {
            setError("Số tiền nạp tối thiểu là 10.000 đ");
            return;
        }

        setIsLoading(true);
        try {
            const res = await depositMoneyAction(walletId, amount);
            if (res.success) {
                toast.success(`Nạp thành công ${formatVND(amount)} vào Ví chính!`);
                onSuccess?.();
                onClose();
            } else {
                setError(res.message);
                toast.error(res.message);
            }
        } catch {
            setError("Có lỗi xảy ra khi nạp tiền");
            toast.error("Có lỗi xảy ra khi nạp tiền");
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
                    <div className="w-9 h-9 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
                        <ArrowDownLeft className="w-4.5 h-4.5 stroke-[2]" />
                    </div>
                    <div>
                        <h3 className="text-base font-bold text-slate-900 tracking-tight">Nạp tiền vào ví</h3>
                        <p className="text-xs text-slate-500">Miễn phí giao dịch nạp tiền 100%</p>
                    </div>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                    {error && (
                        <div className="p-2.5 bg-rose-50 border border-rose-200 text-xs text-rose-700 rounded-lg">
                            {error}
                        </div>
                    )}

                    <div>
                        <Label htmlFor="deposit-amount">Số tiền muốn nạp</Label>
                        <div className="relative mt-1">
                            <Input
                                id="deposit-amount"
                                type="number"
                                min={10000}
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
                        <span className="text-[11px] font-medium text-slate-500 mb-1.5 block">
                            Gợi ý số tiền nhanh:
                        </span>
                        <div className="grid grid-cols-3 gap-2">
                            {PRESET_AMOUNTS.map((val) => (
                                <button
                                    type="button"
                                    key={val}
                                    onClick={() => setAmount(val)}
                                    className={`py-1.5 text-xs font-medium rounded-lg border transition-all cursor-pointer ${
                                        amount === val
                                            ? "border-blue-600 bg-blue-50 text-blue-600 font-semibold shadow-2xs"
                                            : "border-slate-200 text-slate-600 hover:bg-slate-50"
                                    }`}
                                >
                                    {formatVND(val)}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div>
                        <Label>Nguồn thanh toán</Label>
                        <div className="grid grid-cols-2 gap-2 mt-1">
                            <div
                                onClick={() => setSource("vcb")}
                                className={`flex items-center justify-between p-2.5 rounded-lg border cursor-pointer transition-all ${
                                    source === "vcb"
                                        ? "border-blue-600 bg-blue-50/50 text-blue-900"
                                        : "border-slate-200 hover:bg-slate-50 text-slate-700"
                                }`}
                            >
                                <div className="text-xs">
                                    <p className="font-semibold text-slate-800">Vietcombank</p>
                                    <p className="text-[10px] text-slate-400">•••• 8923</p>
                                </div>
                                {source === "vcb" && <Check className="w-3.5 h-3.5 text-blue-600" />}
                            </div>

                            <div
                                onClick={() => setSource("mbb")}
                                className={`flex items-center justify-between p-2.5 rounded-lg border cursor-pointer transition-all ${
                                    source === "mbb"
                                        ? "border-blue-600 bg-blue-50/50 text-blue-900"
                                        : "border-slate-200 hover:bg-slate-50 text-slate-700"
                                }`}
                            >
                                <div className="text-xs">
                                    <p className="font-semibold text-slate-800">MB Bank</p>
                                    <p className="text-[10px] text-slate-400">•••• 6678</p>
                                </div>
                                {source === "mbb" && <Check className="w-3.5 h-3.5 text-blue-600" />}
                            </div>
                        </div>
                    </div>

                    <Button
                        type="submit"
                        variant="primary"
                        className="w-full mt-2"
                        isLoading={isLoading}
                    >
                        Xác nhận nạp {amount > 0 ? formatVND(amount) : ""}
                    </Button>
                </form>
            </div>
        </div>
    );
}
