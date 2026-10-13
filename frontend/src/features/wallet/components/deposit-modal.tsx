"use client";

import { useTranslations, useLocale } from "next-intl";

import { useState } from "react";
import { X, ArrowDownLeft, Check, QrCode, CreditCard, Building2, ExternalLink } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
    useDepositMutation,
    useCreatePaymentOrderMutation,
    useMyWalletQuery,
} from "../hooks/use-wallet";
import { toast } from "sonner";
import { PaymentGatewayType } from "../types/wallet.types";

interface DepositModalProps {
    isOpen: boolean;
    onClose: () => void;
    walletId: string;
    currency?: string;
    onSuccess?: () => void;
}

const PRESET_AMOUNTS = [100000, 200000, 500000, 1000000, 2000000, 5000000];

type PaymentMethodType = "PAYOS" | "STRIPE" | "MOCK";

export function DepositModal({
    isOpen,
    onClose,
    walletId,
    onSuccess,
    currency = "VND",
}: DepositModalProps) {
    const t = useTranslations("wallet");
    const locale = useLocale() === "vi" ? "vi-VN" : "en-US";
    const common = useTranslations("common");
    const formatVND = (value: number) =>
        new Intl.NumberFormat(locale, { style: "currency", currency }).format(value);

    const [amount, setAmount] = useState<number>(500000);
    const [method, setMethod] = useState<PaymentMethodType>("PAYOS");
    const [error, setError] = useState<string | null>(null);

    const { data: myWallet } = useMyWalletQuery();
    const effectiveWalletId = walletId || myWallet?.id || "";

    const depositMutation = useDepositMutation();
    const createOrderMutation = useCreatePaymentOrderMutation();

    if (!isOpen) return null;

    const isLoading = depositMutation.isPending || createOrderMutation.isPending;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        if (amount < 10000) {
            setError(t("theMinimumDepositIsVND10000"));
            return;
        }

        try {
            if (method === "MOCK") {
                // Phương thức nạp mô phỏng trực tiếp
                await depositMutation.mutateAsync({ walletId: effectiveWalletId, amount });
                toast.success(t("depositSuccess", { amount: formatVND(amount) }));
                onSuccess?.();
                onClose();
            } else {
                // Nạp qua cổng thanh toán thực tế (PayOS hoặc Stripe)
                const gateway: PaymentGatewayType = method;
                const order = await createOrderMutation.mutateAsync({
                    walletId: effectiveWalletId,
                    amount,
                    gateway,
                });

                if (order.checkoutUrl) {
                    toast.info(t("redirectPayment", { gateway }));
                    window.location.href = order.checkoutUrl;
                } else {
                    throw new Error(t("thePaymentGatewayDidNotReturnACheckoutLink"));
                }
            }
        } catch (err: unknown) {
            const msg =
                err instanceof Error ? err.message : t("anErrorOccurredWhileProcessingYourDeposit");
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
                        <ArrowDownLeft className="w-4.5 h-4.5 stroke-[2]" />
                    </div>
                    <div>
                        <h3 className="text-base font-bold text-slate-900 tracking-tight">
                            {" "}
                            {t("depositIntoWallet")}{" "}
                        </h3>
                        <p className="text-xs text-slate-500">
                            {" "}
                            {t("supportsVietQRInternationalCardsSimulatedDeposits")}{" "}
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
                        <Label htmlFor="deposit-amount"> {t("depositAmount")} </Label>
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
                                {currency ?? "VND"}
                            </span>
                        </div>
                    </div>

                    <div>
                        <span className="text-[11px] font-medium text-slate-500 mb-1.5 block">
                            {t("quickAmounts")}{" "}
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
                        <Label> {t("chooseAPaymentMethod")} </Label>
                        <div className="space-y-2 mt-1.5">
                            {/* Option 1: PayOS (VietQR) */}
                            <div
                                onClick={() => setMethod("PAYOS")}
                                className={`flex items-center justify-between p-3 rounded-xl border cursor-pointer transition-all ${
                                    method === "PAYOS"
                                        ? "border-blue-600 bg-blue-50/50 text-blue-900 shadow-2xs"
                                        : "border-slate-200 hover:bg-slate-50 text-slate-700"
                                }`}
                            >
                                <div className="flex items-center space-x-3">
                                    <div className="w-8 h-8 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center">
                                        <QrCode className="w-4 h-4 stroke-[2]" />
                                    </div>
                                    <div className="text-xs">
                                        <div className="flex items-center space-x-1.5">
                                            <p className="font-semibold text-slate-900">
                                                {" "}
                                                {t("bankVietQRPayOS")}{" "}
                                            </p>
                                            <span className="px-1.5 py-0.2 bg-emerald-100 text-emerald-700 text-[10px] font-medium rounded">
                                                {t("popular")}{" "}
                                            </span>
                                        </div>
                                        <p className="text-[11px] text-slate-500">
                                            {" "}
                                            {t("scanWithAnyVietnameseBankingApp")}{" "}
                                        </p>
                                    </div>
                                </div>
                                {method === "PAYOS" && <Check className="w-4 h-4 text-blue-600" />}
                            </div>

                            {/* Option 2: Stripe (International Card) */}
                            <div
                                onClick={() => setMethod("STRIPE")}
                                className={`flex items-center justify-between p-3 rounded-xl border cursor-pointer transition-all ${
                                    method === "STRIPE"
                                        ? "border-blue-600 bg-blue-50/50 text-blue-900 shadow-2xs"
                                        : "border-slate-200 hover:bg-slate-50 text-slate-700"
                                }`}
                            >
                                <div className="flex items-center space-x-3">
                                    <div className="w-8 h-8 rounded-lg bg-indigo-100 text-indigo-700 flex items-center justify-center">
                                        <CreditCard className="w-4 h-4 stroke-[2]" />
                                    </div>
                                    <div className="text-xs">
                                        <div className="flex items-center space-x-1.5">
                                            <p className="font-semibold text-slate-900">
                                                {" "}
                                                {t("internationalCardsStripe")}{" "}
                                            </p>
                                            <span className="px-1.5 py-0.2 bg-indigo-100 text-indigo-700 text-[10px] font-medium rounded">
                                                Visa / Master
                                            </span>
                                        </div>
                                        <p className="text-[11px] text-slate-500">
                                            {" "}
                                            {t("payWithVisaMastercardOrJCB")}{" "}
                                        </p>
                                    </div>
                                </div>
                                {method === "STRIPE" && <Check className="w-4 h-4 text-blue-600" />}
                            </div>

                            {/* Option 3: Mock Direct Deposit */}
                            <div
                                onClick={() => setMethod("MOCK")}
                                className={`flex items-center justify-between p-3 rounded-xl border cursor-pointer transition-all ${
                                    method === "MOCK"
                                        ? "border-blue-600 bg-blue-50/50 text-blue-900 shadow-2xs"
                                        : "border-slate-200 hover:bg-slate-50 text-slate-700"
                                }`}
                            >
                                <div className="flex items-center space-x-3">
                                    <div className="w-8 h-8 rounded-lg bg-slate-100 text-slate-700 flex items-center justify-center">
                                        <Building2 className="w-4 h-4 stroke-[2]" />
                                    </div>
                                    <div className="text-xs">
                                        <p className="font-semibold text-slate-900">
                                            {" "}
                                            {t("quickLinkedaccountDepositSimulated")}{" "}
                                        </p>
                                        <p className="text-[11px] text-slate-500">
                                            {" "}
                                            {t(
                                                "creditYourWalletDirectlyWithoutAPaymentGateway"
                                            )}{" "}
                                        </p>
                                    </div>
                                </div>
                                {method === "MOCK" && <Check className="w-4 h-4 text-blue-600" />}
                            </div>
                        </div>
                    </div>

                    <Button
                        type="submit"
                        variant="primary"
                        className="w-full mt-2"
                        isLoading={isLoading}
                    >
                        {method === "MOCK" ? (
                            t("confirmDeposit", { amount: amount > 0 ? formatVND(amount) : "" })
                        ) : (
                            <span className="flex items-center justify-center space-x-1.5">
                                <span>
                                    {" "}
                                    {t("payAmount", {
                                        amount: amount > 0 ? formatVND(amount) : "",
                                    })}
                                </span>
                                <ExternalLink className="w-3.5 h-3.5 ml-1" />
                            </span>
                        )}
                    </Button>
                </form>
            </div>
        </div>
    );
}
