"use client";

import { useTranslations, useLocale } from "next-intl";
import { useState, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import { useRouter } from "@/i18n/navigation";
import {
    ArrowDownLeft,
    ArrowUpRight,
    QrCode,
    CreditCard,
    Building2,
    Landmark,
    Loader2,
    CheckCircle2,
    AlertCircle,
    ShieldCheck,
    Check,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { MoneyInput } from "@/components/ui/money-input";
import { Label } from "@/components/ui/label";
import {
    useDepositMutation,
    useWithdrawMutation,
    useCreatePaymentOrderMutation,
    useMyWalletQuery,
} from "../hooks/use-wallet";
import { PaymentGatewayType } from "../types/wallet.types";
import { toast } from "sonner";

const PRESET_AMOUNTS = [100000, 200000, 500000, 1000000, 2000000, 5000000];

type TabType = "deposit" | "withdraw";
type PaymentMethodType = "PAYOS" | "STRIPE" | "MOCK";

export function DepositWithdrawView() {
    const t = useTranslations("wallet");
    const dashT = useTranslations("dashboard");
    const common = useTranslations("common");
    const locale = useLocale() === "vi" ? "vi-VN" : "en-US";
    const router = useRouter();
    const searchParams = useSearchParams();

    const { data: myWallet, isLoading: isWalletLoading } = useMyWalletQuery();
    const currentBalance = myWallet ? myWallet.balance : 0;
    const walletId = myWallet ? myWallet.id : "";
    const currency = myWallet?.currency ?? "VND";

    const formatVND = (value: number) =>
        new Intl.NumberFormat(locale, { style: "currency", currency }).format(value);

    // Active tab
    const [activeTab, setActiveTab] = useState<TabType>("deposit");

    // Deposit form state
    const [depositAmount, setDepositAmount] = useState<number>(500000);
    const [depositMethod, setDepositMethod] = useState<PaymentMethodType>("PAYOS");
    const [depositError, setDepositError] = useState<string | null>(null);

    // Withdraw form state
    const [withdrawAmount, setWithdrawAmount] = useState<number>(200000);
    const [withdrawError, setWithdrawError] = useState<string | null>(null);
    const bankNumber = "1029384756";
    const bankName = "MB Bank (Ngân hàng Quân Đội)";

    const depositMutation = useDepositMutation();
    const withdrawMutation = useWithdrawMutation();
    const createOrderMutation = useCreatePaymentOrderMutation();

    // Sync tab from URL query param
    useEffect(() => {
        const queryTab = searchParams.get("tab");
        if (queryTab === "withdraw") {
            setActiveTab("withdraw");
        } else if (queryTab === "deposit") {
            setActiveTab("deposit");
        }
    }, [searchParams]);

    const handleTabChange = (tab: TabType) => {
        setActiveTab(tab);
        router.replace(`/dashboard/deposit-withdraw?tab=${tab}`, { scroll: false });
    };

    // Handle Deposit
    const handleDepositSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setDepositError(null);

        if (depositAmount < 10000) {
            setDepositError("Số tiền nạp tối thiểu là 10.000 đ");
            return;
        }

        try {
            if (depositMethod === "PAYOS") {
                const res = await createOrderMutation.mutateAsync({
                    walletId,
                    amount: depositAmount,
                    gateway: "PAYOS",
                });
                if (res.checkoutUrl) {
                    window.location.href = res.checkoutUrl;
                }
            } else if (depositMethod === "MOCK") {
                await depositMutation.mutateAsync({
                    walletId,
                    amount: depositAmount,
                });
                toast.success(t("depositSuccess", { amount: formatVND(depositAmount) }));
            } else {
                toast.info("Cổng Stripe đang được cấu hình.");
            }
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : t("depositFailed");
            setDepositError(msg);
            toast.error(msg);
        }
    };

    // Handle Withdraw
    const handleWithdrawSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setWithdrawError(null);

        if (withdrawAmount <= 0) {
            setWithdrawError(t("theWithdrawalAmountMustBeGreaterThan0"));
            return;
        }

        if (withdrawAmount > currentBalance) {
            setWithdrawError(t("yourAvailableBalanceIsInsufficientForThisTransaction"));
            return;
        }

        try {
            await withdrawMutation.mutateAsync({
                walletId,
                amount: withdrawAmount,
            });
            toast.success(t("withdrawalRequestSuccessful", { amount: formatVND(withdrawAmount) }));
            setWithdrawAmount(200000);
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : t("withdrawalFailed");
            setWithdrawError(msg);
            toast.error(msg);
        }
    };

    const isDepositLoading = depositMutation.isPending || createOrderMutation.isPending;
    const isWithdrawLoading = withdrawMutation.isPending;

    return (
        <main className="flex-1 p-4 sm:p-8 max-w-4xl w-full mx-auto space-y-6">
            {/* Header */}
            <div className="text-center sm:text-left">
                <h1 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
                    {dashT("depositWithdraw")}
                </h1>
                <p className="text-xs sm:text-sm text-slate-500 mt-1">
                    Nạp tiền qua mã QR PayOS tức thì hoặc rút tiền về tài khoản ngân hàng liên kết
                </p>
            </div>

            {/* Segmented Tab Controls */}
            <div className="flex justify-center">
                <div className="bg-slate-200/80 p-1 rounded-xl flex items-center space-x-1 w-full max-w-md">
                    <button
                        type="button"
                        onClick={() => handleTabChange("deposit")}
                        className={`flex-1 py-2 px-4 rounded-lg text-xs font-semibold flex items-center justify-center space-x-2 transition-all cursor-pointer ${
                            activeTab === "deposit"
                                ? "bg-white text-blue-600 shadow-xs"
                                : "text-slate-600 hover:text-slate-900"
                        }`}
                    >
                        <ArrowDownLeft className="w-4 h-4 text-emerald-600" />
                        <span>{t("deposit")}</span>
                    </button>
                    <button
                        type="button"
                        onClick={() => handleTabChange("withdraw")}
                        className={`flex-1 py-2 px-4 rounded-lg text-xs font-semibold flex items-center justify-center space-x-2 transition-all cursor-pointer ${
                            activeTab === "withdraw"
                                ? "bg-white text-blue-600 shadow-xs"
                                : "text-slate-600 hover:text-slate-900"
                        }`}
                    >
                        <ArrowUpRight className="w-4 h-4 text-blue-600" />
                        <span>{t("withdraw")}</span>
                    </button>
                </div>
            </div>

            {/* Current Balance Banner */}
            <div className="max-w-2xl mx-auto p-4.5 rounded-2xl bg-white border border-slate-200/90 shadow-xs flex items-center justify-between">
                <div className="flex items-center space-x-3">
                    <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
                        <ShieldCheck className="w-5 h-5 stroke-[1.75]" />
                    </div>
                    <div>
                        <p className="text-[11px] font-medium text-slate-500">
                            {t("currentBalance")}
                        </p>
                        <p className="text-lg sm:text-xl font-bold text-slate-900">
                            {isWalletLoading ? "..." : formatVND(currentBalance)}
                        </p>
                    </div>
                </div>
                <div className="text-right hidden sm:block">
                    <p className="text-[10px] font-mono text-slate-400">
                        ID: {walletId.slice(0, 12)}...
                    </p>
                    <span className="inline-block mt-0.5 text-[10px] bg-emerald-50 text-emerald-700 px-2 py-0.5 rounded-full font-medium border border-emerald-100">
                        Đang hoạt động
                    </span>
                </div>
            </div>

            {/* Tab 1: Deposit View */}
            {activeTab === "deposit" && (
                <div className="max-w-2xl mx-auto bg-white rounded-2xl border border-slate-200/90 p-6 sm:p-8 shadow-xs animate-in fade-in duration-150">
                    <form onSubmit={handleDepositSubmit} className="space-y-6">
                        {/* Amount */}
                        <div className="space-y-2">
                            <Label
                                htmlFor="deposit-amount"
                                className="text-xs font-semibold text-slate-700"
                            >
                                {t("amount")} ({currency})
                            </Label>
                            <MoneyInput
                                id="deposit-amount"
                                min="10000"
                                step="10000"
                                value={depositAmount}
                                onValueChange={setDepositAmount}
                                className="text-base font-semibold"
                                required
                            />
                            {/* Preset Buttons */}
                            <div className="grid grid-cols-3 sm:grid-cols-6 gap-1.5 pt-1">
                                {PRESET_AMOUNTS.map((val) => (
                                    <button
                                        key={val}
                                        type="button"
                                        onClick={() => setDepositAmount(val)}
                                        className={`py-1.5 px-2 rounded-lg text-xs font-medium border transition-colors cursor-pointer ${
                                            depositAmount === val
                                                ? "bg-blue-50 border-blue-600 text-blue-600 font-semibold"
                                                : "bg-white border-slate-200 text-slate-600 hover:bg-slate-50"
                                        }`}
                                    >
                                        {formatVND(val)}
                                    </button>
                                ))}
                            </div>
                        </div>

                        {/* Payment Method Selector */}
                        <div className="space-y-2">
                            <Label className="text-xs font-semibold text-slate-700">
                                {t("paymentMethod")}
                            </Label>
                            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                                {/* PayOS Option */}
                                <button
                                    type="button"
                                    onClick={() => setDepositMethod("PAYOS")}
                                    className={`p-3.5 rounded-xl border text-left flex flex-col justify-between transition-all cursor-pointer ${
                                        depositMethod === "PAYOS"
                                            ? "border-blue-600 bg-blue-50/50 ring-1 ring-blue-600"
                                            : "border-slate-200 hover:bg-slate-50"
                                    }`}
                                >
                                    <div className="flex items-center justify-between mb-2">
                                        <div className="w-8 h-8 rounded-lg bg-blue-600 text-white flex items-center justify-center">
                                            <QrCode className="w-4 h-4" />
                                        </div>
                                        {depositMethod === "PAYOS" && (
                                            <Check className="w-4 h-4 text-blue-600" />
                                        )}
                                    </div>
                                    <div>
                                        <p className="text-xs font-bold text-slate-900">
                                            PayOS VietQR
                                        </p>
                                        <p className="text-[11px] text-slate-500 mt-0.5">
                                            Quét mã ngân hàng 24/7
                                        </p>
                                    </div>
                                </button>

                                {/* Mock Direct Option */}
                                <button
                                    type="button"
                                    onClick={() => setDepositMethod("MOCK")}
                                    className={`p-3.5 rounded-xl border text-left flex flex-col justify-between transition-all cursor-pointer ${
                                        depositMethod === "MOCK"
                                            ? "border-blue-600 bg-blue-50/50 ring-1 ring-blue-600"
                                            : "border-slate-200 hover:bg-slate-50"
                                    }`}
                                >
                                    <div className="flex items-center justify-between mb-2">
                                        <div className="w-8 h-8 rounded-lg bg-emerald-600 text-white flex items-center justify-center">
                                            <Building2 className="w-4 h-4" />
                                        </div>
                                        {depositMethod === "MOCK" && (
                                            <Check className="w-4 h-4 text-blue-600" />
                                        )}
                                    </div>
                                    <div>
                                        <p className="text-xs font-bold text-slate-900">
                                            Nạp nhanh trực tiếp
                                        </p>
                                        <p className="text-[11px] text-slate-500 mt-0.5">
                                            Cộng tiền ví tức thì (Mock)
                                        </p>
                                    </div>
                                </button>

                                {/* Stripe Option */}
                                <button
                                    type="button"
                                    onClick={() => setDepositMethod("STRIPE")}
                                    className={`p-3.5 rounded-xl border text-left flex flex-col justify-between transition-all cursor-pointer ${
                                        depositMethod === "STRIPE"
                                            ? "border-blue-600 bg-blue-50/50 ring-1 ring-blue-600"
                                            : "border-slate-200 hover:bg-slate-50"
                                    }`}
                                >
                                    <div className="flex items-center justify-between mb-2">
                                        <div className="w-8 h-8 rounded-lg bg-indigo-600 text-white flex items-center justify-center">
                                            <CreditCard className="w-4 h-4" />
                                        </div>
                                        {depositMethod === "STRIPE" && (
                                            <Check className="w-4 h-4 text-blue-600" />
                                        )}
                                    </div>
                                    <div>
                                        <p className="text-xs font-bold text-slate-900">
                                            Thẻ Quốc Tế
                                        </p>
                                        <p className="text-[11px] text-slate-500 mt-0.5">
                                            Visa / MasterCard / JCB
                                        </p>
                                    </div>
                                </button>
                            </div>
                        </div>

                        {depositError && (
                            <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-xs text-rose-600 flex items-center space-x-2">
                                <AlertCircle className="w-4 h-4 shrink-0" />
                                <span>{depositError}</span>
                            </div>
                        )}

                        <Button
                            type="submit"
                            variant="primary"
                            disabled={isDepositLoading || !depositAmount}
                            className="w-full h-11 text-sm font-semibold rounded-xl flex items-center justify-center space-x-2 cursor-pointer shadow-xs"
                        >
                            {isDepositLoading ? (
                                <>
                                    <Loader2 className="w-4 h-4 animate-spin" />
                                    <span>{t("processing")}</span>
                                </>
                            ) : (
                                <>
                                    <ArrowDownLeft className="w-4 h-4" />
                                    <span>
                                        {depositMethod === "PAYOS"
                                            ? "Tạo mã QR & Nạp ngay"
                                            : `Nạp ${formatVND(depositAmount)} vào ví`}
                                    </span>
                                </>
                            )}
                        </Button>
                    </form>
                </div>
            )}

            {/* Tab 2: Withdraw View */}
            {activeTab === "withdraw" && (
                <div className="max-w-2xl mx-auto bg-white rounded-2xl border border-slate-200/90 p-6 sm:p-8 shadow-xs animate-in fade-in duration-150">
                    <form onSubmit={handleWithdrawSubmit} className="space-y-6">
                        {/* Linked Bank Card */}
                        <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/90 flex items-center justify-between">
                            <div className="flex items-center space-x-3">
                                <div className="w-10 h-10 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
                                    <Landmark className="w-5 h-5 stroke-[1.75]" />
                                </div>
                                <div>
                                    <p className="text-xs font-bold text-slate-900">{bankName}</p>
                                    <p className="text-xs font-mono text-slate-500 mt-0.5">
                                        STK: {bankNumber} • NGUYEN MINH ANH
                                    </p>
                                </div>
                            </div>
                            <span className="text-[10px] font-semibold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-100">
                                Đã liên kết
                            </span>
                        </div>

                        {/* Amount Input */}
                        <div className="space-y-2">
                            <Label
                                htmlFor="withdraw-amount"
                                className="text-xs font-semibold text-slate-700"
                            >
                                {t("amount")} ({currency})
                            </Label>
                            <MoneyInput
                                id="withdraw-amount"
                                min="10000"
                                step="10000"
                                max={currentBalance}
                                value={withdrawAmount}
                                onValueChange={setWithdrawAmount}
                                className="text-base font-semibold"
                                required
                            />

                            {/* Preset Buttons */}
                            <div className="grid grid-cols-3 sm:grid-cols-6 gap-1.5 pt-1">
                                {PRESET_AMOUNTS.map((val) => (
                                    <button
                                        key={val}
                                        type="button"
                                        disabled={val > currentBalance}
                                        onClick={() => setWithdrawAmount(val)}
                                        className={`py-1.5 px-2 rounded-lg text-xs font-medium border transition-colors cursor-pointer ${
                                            withdrawAmount === val
                                                ? "bg-blue-50 border-blue-600 text-blue-600 font-semibold"
                                                : val > currentBalance
                                                  ? "bg-slate-50 border-slate-200 text-slate-300 cursor-not-allowed"
                                                  : "bg-white border-slate-200 text-slate-600 hover:bg-slate-50"
                                        }`}
                                    >
                                        {formatVND(val)}
                                    </button>
                                ))}
                            </div>
                        </div>

                        {/* Transaction Note */}
                        <div className="p-3.5 rounded-xl bg-amber-50/70 border border-amber-200/80 text-xs text-amber-800 space-y-1">
                            <p className="font-semibold flex items-center gap-1.5">
                                <ShieldCheck className="w-3.5 h-3.5 text-amber-600" />
                                Lưu ý khi rút tiền:
                            </p>
                            <p className="text-[11px] leading-relaxed text-amber-700">
                                Tiền sẽ được chuyển về tài khoản ngân hàng liên kết trong vòng 1-3
                                phút. Miễn phí rút tiền cho 5 giao dịch đầu tiên trong tháng.
                            </p>
                        </div>

                        {withdrawError && (
                            <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-xs text-rose-600 flex items-center space-x-2">
                                <AlertCircle className="w-4 h-4 shrink-0" />
                                <span>{withdrawError}</span>
                            </div>
                        )}

                        <Button
                            type="submit"
                            variant="primary"
                            disabled={
                                isWithdrawLoading ||
                                !withdrawAmount ||
                                withdrawAmount > currentBalance
                            }
                            className="w-full h-11 text-sm font-semibold rounded-xl flex items-center justify-center space-x-2 cursor-pointer shadow-xs"
                        >
                            {isWithdrawLoading ? (
                                <>
                                    <Loader2 className="w-4 h-4 animate-spin" />
                                    <span>{t("processing")}</span>
                                </>
                            ) : (
                                <>
                                    <ArrowUpRight className="w-4 h-4" />
                                    <span>Rút {formatVND(withdrawAmount)} về ngân hàng</span>
                                </>
                            )}
                        </Button>
                    </form>
                </div>
            )}
        </main>
    );
}
