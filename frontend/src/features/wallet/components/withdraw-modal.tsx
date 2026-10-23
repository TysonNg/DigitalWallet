"use client";

import { useTranslations } from "next-intl";
import { X } from "lucide-react";
import { StripeWithdrawalPanel } from "./stripe-withdrawal-panel";

interface WithdrawModalProps {
    isOpen: boolean;
    onClose: () => void;
    walletId: string;
    currentBalance: number;
    currency?: string;
    onSuccess?: () => void;
}

export function WithdrawModal({ isOpen, onClose, ...props }: WithdrawModalProps) {
    const common = useTranslations("common");

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto p-4 sm:p-6">
            <div
                className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs"
                onClick={onClose}
                aria-hidden="true"
            />

            <div
                role="dialog"
                aria-modal="true"
                aria-label="Stripe Connect sandbox withdrawal"
                className="relative z-10 my-auto max-h-[90vh] w-full max-w-xl overflow-y-auto rounded-2xl border border-slate-200 bg-white p-6 pt-12 shadow-xl"
            >
                <button
                    type="button"
                    onClick={onClose}
                    aria-label={common("close")}
                    className="absolute right-4 top-4 rounded-lg p-1.5 text-slate-500 hover:bg-slate-100"
                >
                    <X className="h-4 w-4" />
                </button>

                <StripeWithdrawalPanel key={props.walletId} {...props} />
            </div>
        </div>
    );
}
