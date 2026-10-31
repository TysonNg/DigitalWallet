"use client";

import { useTranslations, useLocale } from "next-intl";
import { useState, useEffect, useRef } from "react";
import { useSearchParams } from "next/navigation";
import {
    Send,
    BookUser,
    CheckCircle2,
    Loader2,
    BookmarkPlus,
    AlertCircle,
    ArrowRight,
    Search,
    ShieldCheck,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { MoneyInput } from "@/components/ui/money-input";
import { Label } from "@/components/ui/label";
import { Link } from "@/i18n/navigation";
import {
    useTransferMutation,
    useBeneficiariesQuery,
    useAddBeneficiaryMutation,
    useMyWalletQuery,
    ActionError,
} from "../hooks/use-wallet";
import { lookupRecipientAction } from "../actions/wallet.actions";
import { BeneficiaryDto, RecipientLookupDto } from "../types/wallet.types";
import { PinModal } from "../components/pin-modal";
import { toast } from "sonner";

const PRESET_AMOUNTS = [100000, 200000, 500000, 1000000, 2000000, 5000000];

export function TransferView() {
    const t = useTranslations("wallet");
    const dashT = useTranslations("dashboard");
    const apiErr = useTranslations("apiErrors");
    const locale = useLocale() === "vi" ? "vi-VN" : "en-US";
    const searchParams = useSearchParams();

    const { data: myWallet, isLoading: isWalletLoading } = useMyWalletQuery();
    const currentBalance = myWallet ? myWallet.balance : 0;
    const senderWalletId = myWallet ? myWallet.id : "";
    const currency = myWallet?.currency ?? "VND";

    const formatVND = (value: number) =>
        new Intl.NumberFormat(locale, { style: "currency", currency }).format(value);

    const queryRecipient = searchParams.get("recipient") || searchParams.get("walletId") || searchParams.get("phone") || "";
    const queryAmount = searchParams.get("amount");
    const parsedAmount = Number(queryAmount);
    const initialAmount = Number.isFinite(parsedAmount) && parsedAmount > 0 ? parsedAmount : 200000;

    // Form states
    const [identifier, setIdentifier] = useState<string>(queryRecipient);
    const [receiverWalletId, setReceiverWalletId] = useState<string>("");
    const [amount, setAmount] = useState<number>(initialAmount);
    const [description, setDescription] = useState<string>(t("defaultTransferDescription"));
    const [error, setError] = useState<string | null>(null);

    // Lookup states
    const [isLookingUp, setIsLookingUp] = useState<boolean>(false);
    const [verifiedRecipient, setVerifiedRecipient] = useState<RecipientLookupDto | null>(null);
    const [lookupError, setLookupError] = useState<string | null>(null);

    // Save beneficiary
    const [saveBeneficiary, setSaveBeneficiary] = useState<boolean>(false);
    const [beneficiaryNickname, setBeneficiaryNickname] = useState<string>("");

    // Transfer success receipt state
    const [lastTransferSuccess, setLastTransferSuccess] = useState<{
        amount: number;
        recipientName: string;
        receiverWalletId: string;
    } | null>(null);

    const [isPinModalOpen, setIsPinModalOpen] = useState<boolean>(false);

    // Filter beneficiaries on side widget
    const [beneficiaryFilter, setBeneficiaryFilter] = useState<string>("");

    const { data: beneficiaries = [], isLoading: isBeneficiariesLoading } = useBeneficiariesQuery();
    const transferMutation = useTransferMutation();
    const addBeneficiaryMutation = useAddBeneficiaryMutation();

    const lookupTimeoutRef = useRef<NodeJS.Timeout | null>(null);

    const [previousQuery, setPreviousQuery] = useState({ recipient: queryRecipient, amount: queryAmount });

    if (previousQuery.recipient !== queryRecipient || previousQuery.amount !== queryAmount) {
        setPreviousQuery({ recipient: queryRecipient, amount: queryAmount });
        if (previousQuery.amount !== queryAmount) setAmount(initialAmount);
        if (previousQuery.recipient !== queryRecipient) {
            setIdentifier(queryRecipient);
            setReceiverWalletId("");
            setVerifiedRecipient(null);
            setLookupError(null);
        }
    }

    useEffect(() => {
        if (!queryRecipient || identifier !== queryRecipient) return;
        let cancelled = false;
        lookupRecipientAction(queryRecipient).then((res) => {
            if (cancelled) return;
            if (res.success && res.data) {
                setVerifiedRecipient(res.data);
                setReceiverWalletId(res.data.walletId);
                setLookupError(null);
            } else {
                setLookupError(res.errorCode && apiErr.has(res.errorCode) ? apiErr(res.errorCode) : res.message || t("recipientNotFound"));
            }
        }).catch(() => {
            if (!cancelled) setLookupError(t("recipientNotFound"));
        });
        return () => { cancelled = true; };
    }, [queryRecipient, identifier, apiErr, t]);

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
    };

    const isAlreadyBeneficiary =
        verifiedRecipient &&
        beneficiaries.some(
            (b) =>
                b.beneficiaryWalletId === receiverWalletId ||
                (verifiedRecipient.userId && b.beneficiaryUserId === verifiedRecipient.userId)
        );

    const executeTransfer = async (pin?: string) => {
        const targetWalletId = receiverWalletId.trim();

        try {
            await transferMutation.mutateAsync({
                senderWalletId,
                receiverWalletId: targetWalletId,
                amount,
                description: description.trim() || t("defaultTransferDescription"),
                pin,
            });

            if (saveBeneficiary && verifiedRecipient && verifiedRecipient.userId) {
                try {
                    await addBeneficiaryMutation.mutateAsync({
                        beneficiaryUserId: verifiedRecipient.userId,
                        beneficiaryWalletId: targetWalletId,
                        nickname: beneficiaryNickname.trim() || undefined,
                    });
                } catch {
                    // Ignored if already added
                }
            }

            setLastTransferSuccess({
                amount,
                recipientName: verifiedRecipient?.fullName || targetWalletId,
                receiverWalletId: targetWalletId,
            });

            setIsPinModalOpen(false);
            toast.success(t("transferSuccess", { amount: formatVND(amount) }));
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
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
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

        if (amount <= 0) {
            setError(t("theTransferAmountMustBeGreaterThan0"));
            return;
        }

        if (amount > currentBalance) {
            setError(t("yourWalletBalanceIsInsufficientForThisTransfer"));
            return;
        }

        if (myWallet?.hasPin) {
            setIsPinModalOpen(true);
            return;
        }

        await executeTransfer();
    };

    const handleResetForm = () => {
        setLastTransferSuccess(null);
        setIdentifier("");
        setReceiverWalletId("");
        setVerifiedRecipient(null);
        setLookupError(null);
        setError(null);
        setSaveBeneficiary(false);
        setBeneficiaryNickname("");
    };

    const filteredBeneficiaries = beneficiaries.filter((b) => {
        if (!beneficiaryFilter) return true;
        const query = beneficiaryFilter.toLowerCase();
        return (
            (b.fullName && b.fullName.toLowerCase().includes(query)) ||
            (b.nickname && b.nickname.toLowerCase().includes(query)) ||
            (b.phoneNumber && b.phoneNumber.includes(query)) ||
            (b.email && b.email.toLowerCase().includes(query))
        );
    });

    return (
        <main className="flex-1 p-4 sm:p-8 max-w-7xl w-full mx-auto space-y-6">
            {/* Header */}
            <div>
                <h1 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
                    {dashT("transfer")}
                </h1>
                <p className="text-xs sm:text-sm text-slate-500 mt-1">{t("transferSubtitle")}</p>
            </div>

            {/* 2-Column Layout */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
                {/* Main Column: Transfer Form (2 Cols Span) */}
                <div className="lg:col-span-2">
                    {lastTransferSuccess ? (
                        /* Receipt State */
                        <div className="bg-white rounded-2xl border border-slate-200/90 p-8 shadow-xs text-center space-y-5 animate-in fade-in duration-200">
                            <div className="w-16 h-16 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center mx-auto shadow-xs">
                                <CheckCircle2 className="w-10 h-10 stroke-[2]" />
                            </div>
                            <div>
                                <h3 className="text-xl font-bold text-slate-900">
                                    {t("transferSuccess", {
                                        amount: formatVND(lastTransferSuccess.amount),
                                    })}
                                </h3>
                                <p className="text-xs text-slate-500 mt-1">
                                    Giao dịch chuyển tiền đã được thực hiện thành công.
                                </p>
                            </div>

                            <div className="max-w-md mx-auto p-4 rounded-xl bg-slate-50 border border-slate-100 text-xs space-y-2 text-left">
                                <div className="flex justify-between py-1 border-b border-slate-200/60">
                                    <span className="text-slate-500">Người nhận:</span>
                                    <span className="font-semibold text-slate-800">
                                        {lastTransferSuccess.recipientName}
                                    </span>
                                </div>
                                <div className="flex justify-between py-1 border-b border-slate-200/60">
                                    <span className="text-slate-500">Mã ví nhận:</span>
                                    <span className="font-mono text-slate-800">
                                        {lastTransferSuccess.receiverWalletId}
                                    </span>
                                </div>
                                <div className="flex justify-between py-1">
                                    <span className="text-slate-500">Số tiền chuyển:</span>
                                    <span className="font-bold text-emerald-600 text-sm">
                                        {formatVND(lastTransferSuccess.amount)}
                                    </span>
                                </div>
                            </div>

                            <div className="flex flex-col sm:flex-row gap-3 justify-center pt-2">
                                <Button
                                    onClick={handleResetForm}
                                    variant="primary"
                                    className="cursor-pointer"
                                >
                                    Thực hiện giao dịch mới
                                </Button>
                                <Link
                                    href="/dashboard/history"
                                    className="inline-flex items-center justify-center border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 rounded-lg px-4 py-2 text-xs font-medium cursor-pointer"
                                >
                                    Xem lịch sử giao dịch
                                </Link>
                            </div>
                        </div>
                    ) : (
                        /* Transfer Form */
                        <form
                            onSubmit={handleSubmit}
                            className="bg-white rounded-2xl border border-slate-200/90 p-6 sm:p-8 shadow-xs space-y-6"
                        >
                            {/* Available Balance Banner */}
                            <div className="flex items-center justify-between p-4 rounded-xl bg-blue-50/70 border border-blue-100/70">
                                <div className="flex items-center space-x-3">
                                    <div className="w-8 h-8 rounded-lg bg-blue-600 text-white flex items-center justify-center">
                                        <ShieldCheck className="w-4 h-4 stroke-[2]" />
                                    </div>
                                    <div>
                                        <p className="text-[11px] font-medium text-blue-700">
                                            {t("currentBalance")}
                                        </p>
                                        <p className="text-base sm:text-lg font-bold text-blue-950">
                                            {isWalletLoading ? "..." : formatVND(currentBalance)}
                                        </p>
                                    </div>
                                </div>
                            </div>

                            {/* Section 1: Recipient Lookup */}
                            <div className="space-y-2">
                                <Label
                                    htmlFor="transfer-recipient-input"
                                    className="text-xs font-semibold text-slate-700"
                                >
                                    {t("recipientPhoneOrEmail")}
                                </Label>
                                <div className="relative">
                                    <Input
                                        id="transfer-recipient-input"
                                        placeholder={t("recipientPlaceholder")}
                                        value={identifier}
                                        onChange={(e) => handleIdentifierChange(e.target.value)}
                                        className="text-xs pr-10"
                                        required
                                    />
                                    {isLookingUp && (
                                        <Loader2 className="w-4 h-4 text-blue-600 animate-spin absolute right-3 top-1/2 -translate-y-1/2" />
                                    )}
                                </div>

                                {lookupError && (
                                    <div className="flex items-center space-x-1.5 text-xs text-rose-600 mt-1">
                                        <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                                        <span>{lookupError}</span>
                                    </div>
                                )}

                                {verifiedRecipient && (
                                    <div className="p-3.5 rounded-xl bg-emerald-50/70 border border-emerald-200/80 flex items-center justify-between animate-in fade-in duration-150">
                                        <div className="flex items-center space-x-3">
                                            <div className="w-9 h-9 rounded-full bg-emerald-600 text-white flex items-center justify-center font-bold text-xs shadow-xs">
                                                {verifiedRecipient.fullName
                                                    ? verifiedRecipient.fullName
                                                          .slice(0, 2)
                                                          .toUpperCase()
                                                    : "US"}
                                            </div>
                                            <div>
                                                <div className="flex items-center space-x-1.5">
                                                    <p className="text-xs font-bold text-slate-900">
                                                        {verifiedRecipient.fullName}
                                                    </p>
                                                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                                                </div>
                                                <p className="text-[11px] text-slate-500 font-mono">
                                                    Ví: {verifiedRecipient.walletId}
                                                </p>
                                            </div>
                                        </div>
                                    </div>
                                )}
                            </div>

                            {/* Section 2: Amount & Presets */}
                            <div className="space-y-2">
                                <Label
                                    htmlFor="transfer-amount-input"
                                    className="text-xs font-semibold text-slate-700"
                                >
                                    {t("amount")} ({currency})
                                </Label>
                                <MoneyInput
                                    id="transfer-amount-input"
                                    min="1000"
                                    step="1000"
                                    value={amount}
                                    onValueChange={setAmount}
                                    className="text-sm font-semibold"
                                    required
                                />

                                <div className="grid grid-cols-3 sm:grid-cols-6 gap-1.5 pt-1">
                                    {PRESET_AMOUNTS.map((val) => (
                                        <button
                                            key={val}
                                            type="button"
                                            onClick={() => setAmount(val)}
                                            className={`py-1.5 px-2 rounded-lg text-xs font-medium border transition-colors cursor-pointer ${
                                                amount === val
                                                    ? "bg-blue-50 border-blue-600 text-blue-600 font-semibold"
                                                    : "bg-white border-slate-200 text-slate-600 hover:bg-slate-50"
                                            }`}
                                        >
                                            {formatVND(val)}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {/* Section 3: Description */}
                            <div className="space-y-2">
                                <Label
                                    htmlFor="transfer-description-input"
                                    className="text-xs font-semibold text-slate-700"
                                >
                                    {t("description")}
                                </Label>
                                <Input
                                    id="transfer-description-input"
                                    value={description}
                                    onChange={(e) => setDescription(e.target.value)}
                                    placeholder={t("defaultTransferDescription")}
                                    className="text-xs"
                                    maxLength={255}
                                />
                            </div>

                            {/* Beneficiary Save Checkbox */}
                            {verifiedRecipient && !isAlreadyBeneficiary && (
                                <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 space-y-2">
                                    <label className="flex items-center space-x-2 text-xs text-slate-700 font-medium cursor-pointer">
                                        <input
                                            type="checkbox"
                                            checked={saveBeneficiary}
                                            onChange={(e) => setSaveBeneficiary(e.target.checked)}
                                            className="rounded text-blue-600 focus:ring-blue-500 w-4 h-4 cursor-pointer"
                                        />
                                        <BookmarkPlus className="w-3.5 h-3.5 text-blue-600" />
                                        <span>{t("saveToBeneficiaries")}</span>
                                    </label>
                                    {saveBeneficiary && (
                                        <Input
                                            placeholder={t("nicknameOptional")}
                                            value={beneficiaryNickname}
                                            onChange={(e) => setBeneficiaryNickname(e.target.value)}
                                            className="text-xs bg-white"
                                        />
                                    )}
                                </div>
                            )}

                            {error && (
                                <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-xs text-rose-600 flex items-center space-x-2">
                                    <AlertCircle className="w-4 h-4 shrink-0" />
                                    <span>{error}</span>
                                </div>
                            )}

                            <Button
                                type="submit"
                                variant="primary"
                                disabled={
                                    transferMutation.isPending || isLookingUp || !receiverWalletId
                                }
                                className="w-full h-11 text-sm font-semibold rounded-xl flex items-center justify-center space-x-2 cursor-pointer shadow-xs"
                            >
                                {transferMutation.isPending ? (
                                    <>
                                        <Loader2 className="w-4 h-4 animate-spin" />
                                        <span>{t("processing")}</span>
                                    </>
                                ) : (
                                    <>
                                        <Send className="w-4 h-4" />
                                        <span>
                                            {t("transferNow", { amount: formatVND(amount) })}
                                        </span>
                                    </>
                                )}
                            </Button>
                        </form>
                    )}
                </div>

                {/* Right Column: Quick Beneficiaries Widget */}
                <div className="bg-white rounded-2xl border border-slate-200/90 p-5 shadow-xs space-y-4">
                    <div className="flex items-center justify-between pb-3 border-b border-slate-100">
                        <div className="flex items-center space-x-2">
                            <BookUser className="w-4 h-4 text-blue-600" />
                            <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                                {t("selectFromBeneficiaries")}
                            </h2>
                        </div>
                        <span className="text-[11px] font-semibold text-slate-400 bg-slate-50 px-2 py-0.5 rounded-full border border-slate-100">
                            {beneficiaries.length}
                        </span>
                    </div>

                    {/* Search inside beneficiaries */}
                    <div className="relative">
                        <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-1/2 -translate-y-1/2" />
                        <Input
                            placeholder={t("searchBeneficiaries")}
                            value={beneficiaryFilter}
                            onChange={(e) => setBeneficiaryFilter(e.target.value)}
                            className="text-xs pl-8 h-8"
                        />
                    </div>

                    {/* List */}
                    <div className="space-y-1.5 max-h-[420px] overflow-y-auto pr-1">
                        {isBeneficiariesLoading ? (
                            <div className="py-8 text-center text-xs text-slate-400 flex items-center justify-center space-x-2">
                                <Loader2 className="w-3.5 h-3.5 animate-spin text-blue-600" />
                                <span>{t("beneficiariesLoading")}</span>
                            </div>
                        ) : filteredBeneficiaries.length === 0 ? (
                            <div className="py-8 text-center text-xs text-slate-400 space-y-1">
                                <p className="font-medium text-slate-500">
                                    {t("noBeneficiariesYet")}
                                </p>
                                <p className="text-[11px]">{t("beneficiariesEmptyNotice")}</p>
                            </div>
                        ) : (
                            filteredBeneficiaries.map((b) => (
                                <button
                                    key={b.id}
                                    type="button"
                                    onClick={() => handleSelectBeneficiary(b)}
                                    className="w-full text-left p-2.5 rounded-xl hover:bg-blue-50/70 border border-transparent hover:border-blue-100 transition-colors flex items-center justify-between group cursor-pointer"
                                >
                                    <div className="flex items-center space-x-2.5 min-w-0">
                                        <div className="w-8 h-8 rounded-full bg-slate-100 text-slate-700 group-hover:bg-blue-600 group-hover:text-white flex items-center justify-center font-bold text-[11px] transition-colors shrink-0">
                                            {b.fullName
                                                ? b.fullName.slice(0, 2).toUpperCase()
                                                : "US"}
                                        </div>
                                        <div className="min-w-0">
                                            <p className="text-xs font-semibold text-slate-800 truncate group-hover:text-blue-600 transition-colors">
                                                {b.nickname || b.fullName || "Người nhận"}
                                            </p>
                                            <p className="text-[11px] text-slate-400 truncate">
                                                {b.phoneNumber ||
                                                    b.email ||
                                                    b.beneficiaryWalletId.slice(0, 10)}
                                            </p>
                                        </div>
                                    </div>
                                    <span className="text-[10px] text-blue-600 font-semibold opacity-0 group-hover:opacity-100 transition-opacity flex items-center shrink-0">
                                        Chọn <ArrowRight className="w-3 h-3 ml-0.5" />
                                    </span>
                                </button>
                            ))
                        )}
                    </div>

                    <div className="pt-2 border-t border-slate-100">
                        <Link
                            href="/dashboard/beneficiaries"
                            className="text-xs text-blue-600 hover:text-blue-700 font-medium flex items-center justify-center py-1 hover:underline"
                        >
                            Quản lý toàn bộ danh bạ →
                        </Link>
                    </div>
                </div>
            </div>

            <PinModal
                isOpen={isPinModalOpen}
                onClose={() => setIsPinModalOpen(false)}
                onConfirm={executeTransfer}
                amount={amount}
                currency={currency}
                isLoading={transferMutation.isPending}
                title="Xác thực mã PIN chuyển tiền"
                description={`Nhập mã PIN 6 số để xác nhận chuyển tiền đến ${
                    verifiedRecipient?.fullName || receiverWalletId
                }`}
            />
        </main>
    );
}
