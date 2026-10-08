"use client";

import { useState } from "react";
import { X, Send } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { formatVND } from "@/lib/utils";
import { transferMoneyAction } from "../actions/wallet.actions";
import { toast } from "sonner";

interface TransferModalProps {
    isOpen: boolean;
    onClose: () => void;
    senderWalletId: string;
    currentBalance: number;
    onSuccess?: () => void;
}

export function TransferModal({
    isOpen,
    onClose,
    senderWalletId,
    currentBalance,
    onSuccess,
}: TransferModalProps) {
    const [receiverWalletId, setReceiverWalletId] = useState<string>("");
    const [amount, setAmount] = useState<number>(200000);
    const [description, setDescription] = useState<string>("Chuyển tiền ví điện tử");
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);

        if (!receiverWalletId.trim()) {
            setError("Vui lòng nhập mã ví người nhận");
            return;
        }

        if (receiverWalletId.trim() === senderWalletId) {
            setError("Không thể tự chuyển tiền cho chính ví của bạn");
            return;
        }

        if (amount <= 0) {
            setError("Số tiền chuyển phải lớn hơn 0");
            return;
        }

        if (amount > currentBalance) {
            setError("Số dư ví không đủ để thực hiện chuyển khoản này");
            return;
        }

        setIsLoading(true);
        try {
            const res = await transferMoneyAction(
                senderWalletId,
                receiverWalletId.trim(),
                amount,
                description
            );
            if (res.success) {
                toast.success(`Chuyển ${formatVND(amount)} thành công!`);
                onSuccess?.();
                onClose();
            } else {
                setError(res.message);
                toast.error(res.message);
            }
        } catch {
            setError("Có lỗi xảy ra khi chuyển tiền");
            toast.error("Có lỗi xảy ra khi chuyển tiền");
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
                        <Send className="w-4 h-4 stroke-[2]" />
                    </div>
                    <div>
                        <h3 className="text-base font-bold text-slate-900 tracking-tight">Chuyển tiền qua Ví</h3>
                        <p className="text-xs text-slate-500">
                            Khả dụng:{" "}
                            <span className="font-semibold text-slate-800">
                                {formatVND(currentBalance)}
                            </span>
                        </p>
                    </div>
                </div>

                <form onSubmit={handleSubmit} className="space-y-3.5">
                    {error && (
                        <div className="p-2.5 bg-rose-50 border border-rose-200 text-xs text-rose-700 rounded-lg">
                            {error}
                        </div>
                    )}

                    <div>
                        <Label htmlFor="receiver-wallet-id" required>
                            Mã ví người nhận (UUID)
                        </Label>
                        <Input
                            id="receiver-wallet-id"
                            placeholder="b8a7c2e1-4567-..."
                            value={receiverWalletId}
                            onChange={(e) => setReceiverWalletId(e.target.value)}
                            className="font-mono text-xs"
                            required
                        />
                    </div>

                    <div>
                        <Label htmlFor="transfer-amount" required>
                            Số tiền chuyển
                        </Label>
                        <div className="relative mt-1">
                            <Input
                                id="transfer-amount"
                                type="number"
                                min={10000}
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
                        <Label htmlFor="transfer-desc">Nội dung chuyển tiền</Label>
                        <Input
                            id="transfer-desc"
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            placeholder="Nhập ghi chú giao dịch"
                            className="text-xs"
                        />
                    </div>

                    <div className="p-2.5 bg-slate-50 rounded-lg space-y-1 text-xs text-slate-600 border border-slate-100">
                        <div className="flex justify-between">
                            <span>Phí chuyển tiền:</span>
                            <span className="font-semibold text-emerald-600">Miễn phí 0 đ</span>
                        </div>
                    </div>

                    <Button
                        type="submit"
                        variant="primary"
                        className="w-full mt-2"
                        isLoading={isLoading}
                        disabled={currentBalance <= 0}
                    >
                        Chuyển ngay {amount > 0 ? formatVND(amount) : ""}
                    </Button>
                </form>
            </div>
        </div>
    );
}
