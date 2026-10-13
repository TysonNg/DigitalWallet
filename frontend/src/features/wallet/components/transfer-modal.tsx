"use client";

import { useTranslations, useLocale } from "next-intl";

import { useState } from "react";
import { X, Send } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useTransferMutation } from "../hooks/use-wallet";
import { toast } from "sonner";

interface TransferModalProps {
    isOpen: boolean;
    onClose: () => void;
    senderWalletId: string;
    currentBalance: number;
    currency?: string;
    onSuccess?: () => void;
}

export function TransferModal({
    isOpen,
    onClose,
    senderWalletId,
    currentBalance,
    currency = "VND",
    onSuccess,
}: TransferModalProps) {
    const t = useTranslations("wallet");
    const locale = useLocale() === "vi" ? "vi-VN" : "en-US";
    const common = useTranslations("common");
    const formatVND = (value: number) =>
        new Intl.NumberFormat(locale, { style: "currency", currency }).format(value);

    const [receiverWalletId, setReceiverWalletId] = useState<string>("");
    const [amount, setAmount] = useState<number>(200000);
    const [description, setDescription] = useState<string>(t("defaultTransferDescription"));
    const [error, setError] = useState<string | null>(null);

    const transferMutation = useTransferMutation();

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);

        if (!receiverWalletId.trim()) {
            setError(t("pleaseEnterTheRecipientWalletID"));
            return;
        }

        if (receiverWalletId.trim() === senderWalletId) {
            setError(t("youCannotTransferToYourOwnWallet"));
            return;
        }

        if (amount <= 0) {
            setError(t("theTransferAmountMustBeGreaterThan0"));
            return;
        }

        if (amount > currentBalance) {
            setError(t("yourWalletBalanceIsInsufficientForThisTransfer"));
            return;
        }

        try {
            await transferMutation.mutateAsync({
                senderWalletId,
                receiverWalletId: receiverWalletId.trim(),
                amount,
                description: description.trim() || t("defaultTransferDescription"),
            });
            toast.success(t("transferSuccess", { amount: formatVND(amount) }));
            onSuccess?.();
            onClose();
        } catch (err: unknown) {
            const msg =
                err instanceof Error ? err.message : t("anErrorOccurredWhileTransferringMoney");
            setError(msg);
            toast.error(msg);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto">
            <div
                className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs transition-opacity duration-150"
                aria-label={common("close")}
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
                        <h3 className="text-base font-bold text-slate-900 tracking-tight">
                            {" "}
                            {t("transferBetweenWallets")}{" "}
                        </h3>
                        <p className="text-xs text-slate-500">
                            {t("available")}{" "}
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
                            {t("recipientWalletIDUUID")}{" "}
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
                            {t("transferAmount")}{" "}
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
                                {currency ?? "VND"}
                            </span>
                        </div>
                    </div>

                    <div>
                        <Label htmlFor="transfer-desc"> {t("transferDescription")} </Label>
                        <Input
                            id="transfer-desc"
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            placeholder={t("enterATransactionNote")}
                            className="text-xs"
                        />
                    </div>

                    <div className="p-2.5 bg-slate-50 rounded-lg space-y-1 text-xs text-slate-600 border border-slate-100">
                        <div className="flex justify-between">
                            <span> {t("transferFee")} </span>
                            <span className="font-semibold text-emerald-600"> {t("free")} </span>
                        </div>
                    </div>

                    <Button
                        type="submit"
                        variant="primary"
                        className="w-full mt-2"
                        isLoading={transferMutation.isPending}
                        disabled={currentBalance <= 0}
                    >
                        {t("transferNowAmount", { amount: amount > 0 ? formatVND(amount) : "" })}
                    </Button>
                </form>
            </div>
        </div>
    );
}
