"use client";

import { useTranslations, useLocale } from "next-intl";
import { useState, useEffect, useRef } from "react";
import {
    X,
    Send,
    BookUser,
    CheckCircle2,
    Loader2,
    BookmarkPlus,
    AlertCircle,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { MoneyInput } from "@/components/ui/money-input";
import { Label } from "@/components/ui/label";
import {
    useTransferMutation,
    useBeneficiariesQuery,
    useAddBeneficiaryMutation,
    ActionError,
} from "../hooks/use-wallet";
import { lookupRecipientAction } from "../actions/wallet.actions";
import { BeneficiaryDto, RecipientLookupDto } from "../types/wallet.types";
import { toast } from "sonner";
import { initializeTransferForm } from "../transfer-form";
import { PinModal } from "./pin-modal";

interface InitialRecipient {
    identifier: string;
    walletId: string;
    fullName: string;
    nickname?: string;
}

interface TransferModalProps {
    isOpen: boolean;
    onClose: () => void;
    senderWalletId: string;
    currentBalance: number;
    currency?: string;
    onSuccess?: () => void;
    initialRecipient?: InitialRecipient | null;
    initialAmount?: number;
    initialDescription?: string;
}

interface PendingTransfer {
    senderWalletId: string;
    receiverWalletId: string;
    amount: number;
    description: string;
}

export function TransferModal({
    isOpen,
    onClose,
    senderWalletId,
    currentBalance,
    currency = "VND",
    onSuccess,
    initialRecipient,
    initialAmount,
    initialDescription,
}: TransferModalProps) {
    const t = useTranslations("wallet");
    const apiErr = useTranslations("apiErrors");
    const locale = useLocale() === "vi" ? "vi-VN" : "en-US";
    const common = useTranslations("common");
    const formatVND = (value: number) =>
        new Intl.NumberFormat(locale, { style: "currency", currency }).format(value);

    // Form inputs
    const [identifier, setIdentifier] = useState<string>("");
    const [receiverWalletId, setReceiverWalletId] = useState<string>("");
    const defaultDescription = t("defaultTransferDescription");
    const [amount, setAmount] = useState<number>(() =>
        initializeTransferForm(defaultDescription, initialAmount, initialDescription).amount
    );
    const [description, setDescription] = useState<string>(() =>
        initializeTransferForm(defaultDescription, initialAmount, initialDescription).description
    );
    const [error, setError] = useState<string | null>(null);

    const [pendingTransfer, setPendingTransfer] = useState<PendingTransfer | null>(null);

    const pendingTransferRef = useRef<PendingTransfer | null>(null);

    const transferInFlightRef = useRef(false);

    // Recipient lookup states
    const [isLookingUp, setIsLookingUp] = useState<boolean>(false);
    const [verifiedRecipient, setVerifiedRecipient] = useState<RecipientLookupDto | null>(null);
    const [lookupError, setLookupError] = useState<string | null>(null);

    // Beneficiary save options
    const [saveBeneficiary, setSaveBeneficiary] = useState<boolean>(false);
    const [beneficiaryNickname, setBeneficiaryNickname] = useState<string>("");

    // Quick address book selector
    const [isAddressBookOpen, setIsAddressBookOpen] = useState<boolean>(false);

    const { data: beneficiaries = [] } = useBeneficiariesQuery();
    const transferMutation = useTransferMutation();
    const addBeneficiaryMutation = useAddBeneficiaryMutation();

    // Debounce timer ref
    const lookupTimeoutRef = useRef<NodeJS.Timeout | null>(null);

    const [previousInputs, setPreviousInputs] = useState<unknown[] | null>(null);
    const inputs = [isOpen, initialRecipient, currency, initialAmount, initialDescription, defaultDescription];

    useEffect(() => {
        if (!isOpen) pendingTransferRef.current = null;
    }, [isOpen]);

    // Adjust state before children render when a different form is requested.
    if (!previousInputs || inputs.some((value, index) => value !== previousInputs[index])) {
        setPreviousInputs(inputs);
        if (!isOpen) {
            setPendingTransfer(null);
            setIdentifier("");
            setReceiverWalletId("");
            setVerifiedRecipient(null);
            setLookupError(null);
            setError(null);
            setSaveBeneficiary(false);
            setBeneficiaryNickname("");
            setIsAddressBookOpen(false);
        } else if (initialRecipient) {
            setIdentifier(initialRecipient.identifier);
            setReceiverWalletId(initialRecipient.walletId);
            setVerifiedRecipient({
                userId: "",
                walletId: initialRecipient.walletId,
                fullName: initialRecipient.fullName,
                currency,
            });
            setLookupError(null);
        }

        const form = initializeTransferForm(defaultDescription, initialAmount, initialDescription);
        setAmount(form.amount);
        setDescription(form.description);
    }

    // Handle debounced lookup when user types in the identifier input
    const handleIdentifierChange = (value: string) => {
        setIdentifier(value);
        setError(null);
        setLookupError(null);

        if (lookupTimeoutRef.current) {
            clearTimeout(lookupTimeoutRef.current);
        }

        const trimmed = value.trim();
        if (!trimmed || trimmed.length < 3) {
            setVerifiedRecipient(null);
            setReceiverWalletId("");
            setIsLookingUp(false);
            return;
        }

        setIsLookingUp(true);
        lookupTimeoutRef.current = setTimeout(async () => {
            try {
                const res = await lookupRecipientAction(trimmed);
                if (res.success && res.data) {
                    setVerifiedRecipient(res.data);
                    setReceiverWalletId(res.data.walletId);
                    setLookupError(null);
                } else {
                    setVerifiedRecipient(null);
                    setReceiverWalletId("");
                    const msg =
                        res.errorCode && apiErr.has(res.errorCode)
                            ? apiErr(res.errorCode)
                            : res.message || t("recipientNotFound");
                    setLookupError(msg);
                }
            } catch {
                setVerifiedRecipient(null);
                setReceiverWalletId("");
                setLookupError(t("recipientNotFound"));
            } finally {
                setIsLookingUp(false);
            }
        }, 400);
    };

    const handleSelectBeneficiary = (b: BeneficiaryDto) => {
        const val = b.phoneNumber || b.email || b.beneficiaryWalletId;
        setIdentifier(val);
        setReceiverWalletId(b.beneficiaryWalletId);
        setVerifiedRecipient({
            userId: b.beneficiaryUserId,
            walletId: b.beneficiaryWalletId,
            fullName: b.fullName || b.nickname || "",
            phoneNumber: b.phoneNumber,
            email: b.email,
            currency,
        });
        setLookupError(null);
        setError(null);
        setIsAddressBookOpen(false);
    };

    // Check if the current recipient is already saved in beneficiaries
    const isAlreadyBeneficiary =
        verifiedRecipient &&
        beneficiaries.some(
            (b) =>
                b.beneficiaryWalletId === receiverWalletId ||
                (verifiedRecipient.userId && b.beneficiaryUserId === verifiedRecipient.userId)
        );

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (pendingTransferRef.current || transferInFlightRef.current) return;

        setError(null);

        const targetWalletId = receiverWalletId.trim();

        if (!targetWalletId) {
            setError(t("pleaseEnterTheRecipientWalletID"));
            return;
        }

        if (targetWalletId === senderWalletId) {
            setError(t("youCannotTransferToYourOwnWallet"));
            return;
        }

        if (!Number.isSafeInteger(amount) || amount <= 0) {
            setError(t("theTransferAmountMustBeGreaterThan0"));
            return;
        }

        if (amount > currentBalance) {
            setError(t("yourWalletBalanceIsInsufficientForThisTransfer"));
            return;
        }

        const transfer = {
            senderWalletId,
            receiverWalletId: targetWalletId,
            amount,
            description: initialDescription !== undefined
                ? description
                : description.trim() || defaultDescription,
        };
        pendingTransferRef.current = transfer;
        setPendingTransfer(transfer);
    };

    const handleCancelPin = () => {
        if (transferInFlightRef.current) return;

        pendingTransferRef.current = null;
        setPendingTransfer(null);
    };

    const handleClose = () => {
        if (transferInFlightRef.current) return;

        handleCancelPin();
        onClose();
    };

    const handleConfirmPin = async (pin: string) => {
        if (!pendingTransfer || pendingTransferRef.current !== pendingTransfer || transferInFlightRef.current) return;

        if (!/^\d{6}$/.test(pin)) {
            throw new Error(apiErr("INVALID_WALLET_PIN"));
        }

        transferInFlightRef.current = true;
        setError(null);

        try {
            await transferMutation.mutateAsync({ ...pendingTransfer, pin });

            // Automatically save to beneficiaries if checked and recipient info is available
            if (saveBeneficiary && verifiedRecipient && verifiedRecipient.userId) {
                try {
                    await addBeneficiaryMutation.mutateAsync({
                        beneficiaryUserId: verifiedRecipient.userId,
                        beneficiaryWalletId: pendingTransfer.receiverWalletId,
                        nickname: beneficiaryNickname.trim() || undefined,
                    });
                } catch {
                    // Ignored if already added or background error
                }
            }

            pendingTransferRef.current = null;
            setPendingTransfer(null);
            toast.success(t("transferSuccess", { amount: formatVND(pendingTransfer.amount) }));
            onSuccess?.();
            onClose();
        } catch (err: unknown) {
            let msg = t("anErrorOccurredWhileTransferringMoney");
            if (err instanceof ActionError && err.errorCode && apiErr.has(err.errorCode)) {
                msg = apiErr(err.errorCode);
            } else if (err instanceof Error) {
                msg = err.message;
            }
            setError(msg);
            toast.error(msg);
            throw new Error(msg);
        } finally {
            transferInFlightRef.current = false;
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto">
            <div
                className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs transition-opacity duration-150"
                aria-label={common("close")}
                onClick={handleClose}
            />

            <div className="relative w-full max-w-sm sm:max-w-md bg-white rounded-2xl shadow-xl border border-slate-200/90 p-6 sm:p-7 z-10 duration-150 my-auto">
                <button
                    onClick={handleClose}
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
                            {t("transferBetweenWallets")}
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
                        <div className="p-2.5 bg-rose-50 border border-rose-200 text-xs text-rose-700 rounded-lg flex items-center gap-2">
                            <AlertCircle className="w-4 h-4 shrink-0" />
                            <span>{error}</span>
                        </div>
                    )}

                    {/* Recipient Input with Address Book Icon */}
                    <div>
                        <Label htmlFor="receiver-identifier" required>
                            {t("recipientIdentifier")}
                        </Label>
                        <div className="relative mt-1">
                            <Input
                                id="receiver-identifier"
                                placeholder={t("searchRecipientPlaceholder")}
                                value={identifier}
                                onChange={(e) => handleIdentifierChange(e.target.value)}
                                className="text-xs pr-18 font-medium"
                                required
                            />
                            <div className="absolute right-1.5 top-1/2 -translate-y-1/2 flex items-center gap-1">
                                {isLookingUp && (
                                    <Loader2 className="w-4 h-4 animate-spin text-blue-600 mr-1" />
                                )}
                                <button
                                    type="button"
                                    onClick={() => setIsAddressBookOpen(!isAddressBookOpen)}
                                    className={`p-1.5 rounded-md transition-colors cursor-pointer ${
                                        isAddressBookOpen
                                            ? "bg-blue-100 text-blue-700"
                                            : "text-slate-400 hover:text-blue-600 hover:bg-slate-100"
                                    }`}
                                    title={t("openAddressBook")}
                                >
                                    <BookUser className="w-4 h-4" />
                                </button>
                            </div>
                        </div>

                        {/* Quick Address Book Dropdown */}
                        {isAddressBookOpen && (
                            <div className="mt-2 p-2 rounded-xl bg-slate-50 border border-slate-200 shadow-sm max-h-48 overflow-y-auto space-y-1">
                                <div className="text-[11px] font-semibold text-slate-500 px-2 py-1">
                                    {t("selectFromBeneficiaries")}
                                </div>
                                {beneficiaries.length === 0 ? (
                                    <div className="text-center py-3 text-xs text-slate-400">
                                        {t("noBeneficiariesYet")}
                                    </div>
                                ) : (
                                    beneficiaries.map((b) => (
                                        <button
                                            key={b.id}
                                            type="button"
                                            onClick={() => handleSelectBeneficiary(b)}
                                            className="w-full text-left p-2 rounded-lg hover:bg-white hover:shadow-xs transition-all flex items-center justify-between text-xs cursor-pointer group"
                                        >
                                            <div className="flex items-center gap-2">
                                                <div className="w-6 h-6 rounded-full bg-blue-100 text-blue-600 font-bold text-[10px] flex items-center justify-center uppercase shrink-0">
                                                    {(b.fullName || b.nickname || "?").charAt(0)}
                                                </div>
                                                <div>
                                                    <div className="font-semibold text-slate-800 group-hover:text-blue-600">
                                                        {b.nickname || b.fullName}
                                                    </div>
                                                    <div className="text-[10px] text-slate-400">
                                                        {b.phoneNumber || b.email}
                                                    </div>
                                                </div>
                                            </div>
                                            <span className="text-[10px] text-blue-600 font-medium opacity-0 group-hover:opacity-100 transition-opacity">
                                                {t("choose")}
                                            </span>
                                        </button>
                                    ))
                                )}
                            </div>
                        )}

                        {/* Recipient Lookup Error */}
                        {lookupError && !isLookingUp && (
                            <div className="mt-1.5 text-xs text-rose-600 flex items-center gap-1">
                                <AlertCircle className="w-3.5 h-3.5" />
                                <span>{lookupError}</span>
                            </div>
                        )}

                        {/* Verified Recipient Card */}
                        {verifiedRecipient && !isLookingUp && (
                            <div className="mt-2 p-2.5 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-between transition-all">
                                <div className="flex items-center gap-2.5">
                                    <div className="w-8 h-8 rounded-full bg-emerald-600 text-white font-bold text-xs flex items-center justify-center uppercase shadow-xs">
                                        {(verifiedRecipient.fullName || "?").charAt(0)}
                                    </div>
                                    <div>
                                        <div className="flex items-center gap-1.5">
                                            <span className="text-xs font-bold text-slate-900 tracking-wide uppercase">
                                                {verifiedRecipient.fullName}
                                            </span>
                                            <span className="inline-flex items-center gap-0.5 text-[9px] text-emerald-700 font-semibold bg-emerald-100/90 px-1.5 py-0.5 rounded-full">
                                                <CheckCircle2 className="w-2.5 h-2.5 text-emerald-600" />
                                                {t("verifiedRecipient")}
                                            </span>
                                        </div>
                                        <div className="text-[10px] text-slate-500 font-mono">
                                            {verifiedRecipient.phoneNumber ||
                                                verifiedRecipient.email ||
                                                `Ví: ${verifiedRecipient.walletId.slice(0, 8)}...`}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}

                        {/* Checkbox Save to Beneficiaries */}
                        {verifiedRecipient && !isAlreadyBeneficiary && (
                            <div className="mt-2.5 pt-2 border-t border-slate-100 space-y-2">
                                <label className="flex items-center gap-2 text-xs text-slate-700 cursor-pointer select-none">
                                    <input
                                        type="checkbox"
                                        checked={saveBeneficiary}
                                        onChange={(e) => setSaveBeneficiary(e.target.checked)}
                                        className="rounded border-slate-300 text-blue-600 focus:ring-blue-500 w-3.5 h-3.5 cursor-pointer"
                                    />
                                    <span className="font-medium flex items-center gap-1">
                                        <BookmarkPlus className="w-3.5 h-3.5 text-blue-600" />
                                        {t("saveToBeneficiaries")}
                                    </span>
                                </label>

                                {saveBeneficiary && (
                                    <div className="pl-5">
                                        <Input
                                            placeholder={t("nicknamePlaceholder")}
                                            value={beneficiaryNickname}
                                            onChange={(e) => setBeneficiaryNickname(e.target.value)}
                                            className="text-xs h-8"
                                        />
                                    </div>
                                )}
                            </div>
                        )}
                    </div>

                    {/* Amount Input */}
                    <div>
                        <Label htmlFor="transfer-amount" required>
                            {t("transferAmount")}
                        </Label>
                        <div className="relative mt-1">
                            <MoneyInput
                                id="transfer-amount"
                                min={10000}
                                max={currentBalance}
                                step={10000}
                                value={amount}
                                onValueChange={setAmount}
                                className="text-base font-bold text-slate-900 pr-12"
                                placeholder="0"
                                required
                            />
                            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-slate-400">
                                {currency ?? "VND"}
                            </span>
                        </div>
                    </div>

                    {/* Description Input */}
                    <div>
                        <Label htmlFor="transfer-desc">{t("transferDescription")}</Label>
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
                            <span>{t("transferFee")}</span>
                            <span className="font-semibold text-emerald-600">{t("free")}</span>
                        </div>
                    </div>

                    <Button
                        type="submit"
                        variant="primary"
                        className="w-full mt-2"
                        isLoading={transferMutation.isPending}
                        disabled={currentBalance <= 0 || !receiverWalletId || isLookingUp}
                    >
                        {t("transferNowAmount", { amount: amount > 0 ? formatVND(amount) : "" })}
                    </Button>
                </form>
            </div>

            {pendingTransfer && (
                <PinModal
                    isOpen={true}
                    onClose={handleCancelPin}
                    onConfirm={handleConfirmPin}
                    amount={pendingTransfer.amount}
                    currency={currency}
                    isLoading={transferMutation.isPending}
                />
            )}
        </div>
    );
}
