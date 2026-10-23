"use client";

import { useEffect, useId, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { useMyWalletQuery } from "../hooks/use-wallet";
import {
    useStripeWithdrawalMutation,
    useStripeWithdrawalsQuery,
} from "../hooks/use-stripe-withdrawal";
import { createWithdrawalIntent, validateWithdrawal } from "../stripe-withdrawal";
import type { TestBank, WithdrawalDto, WithdrawalStatus } from "../types/stripe-withdrawal.types";

interface StripeWithdrawalPanelProps {
    walletId: string;
    currentBalance: number;
    currency?: string;
    onSuccess?: () => void;
}

const statusLabels: Record<WithdrawalStatus, string> = {
    PENDING: "Pending",
    PROCESSING: "Processing",
    SUCCEEDED: "Succeeded (test money only)",
    FAILED: "Failed",
    RETURNED: "Returned ? funds restored",
    REVIEW_REQUIRED: "Review required — do not resubmit; contact support",
};

const vnd = (amount: number) =>
    new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(amount);

export function StripeWithdrawalPanel({
    walletId,
    currentBalance,
    currency = "VND",
    onSuccess,
}: StripeWithdrawalPanelProps) {
    const id = useId();

    const wallet = useMyWalletQuery();

    const history = useStripeWithdrawalsQuery(walletId);

    const mutation = useStripeWithdrawalMutation();

    const [amount, setAmount] = useState("25000");

    const [testBank, setTestBank] = useState<TestBank>("SUCCESS");

    const [pin, setPin] = useState("");

    const [error, setError] = useState<string | null>(null);

    const [submitted, setSubmitted] = useState<WithdrawalDto | null>(null);

    const intent = useRef(createWithdrawalIntent());

    const submitting = useRef(false);

    const notified = useRef<string | null>(null);

    const walletReady = wallet.data?.id === walletId && !wallet.isError;
    const latest = history.data?.find((item) => item.id === submitted?.id) ?? submitted;
    const hasPin = wallet.data?.hasPin === true;
    const balance = walletReady ? wallet.data!.balance : currentBalance;

    useEffect(() => {
        if (latest?.status === "SUCCEEDED" && notified.current !== latest.id) {
            notified.current = latest.id;
            onSuccess?.();
        }
    }, [latest, onSuccess]);

    const changeIntent = () => {
        setSubmitted(null);
        setError(null);
    };

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        if (submitting.current || submitted) return;
        const input = { walletId, amount: Number(amount), testBank, ...(hasPin ? { pin } : {}) };
        // A timed-out request may already have reserved the balance. Let the backend
        // resolve a retry by its original UUID instead of rejecting it locally.
        const validation = validateWithdrawal(input, {
            balance: intent.current.hasPending(input) ? undefined : balance,
            hasPin,
        });
        if (!walletReady || currency !== "VND" || validation) {
            setError(
                validation ??
                    "An available VND wallet is required. Refresh your wallet and try again."
            );
            return;
        }
        submitting.current = true;
        setError(null);

        try {
            const result = await mutation.mutateAsync({
                ...input,
                idempotencyKey: intent.current.keyFor(input),
            });
            setSubmitted(result);
            setPin("");
            intent.current.confirm(input);
        } catch (cause) {
            setError(
                cause instanceof Error
                    ? cause.message
                    : "Unable to confirm withdrawal. Retry the same request."
            );
        } finally {
            submitting.current = false;
        }
    };

    return (
        <section className="space-y-5" aria-label="Stripe Connect sandbox withdrawals">
            <div className="space-y-2">
                <h2 className="text-lg font-semibold text-slate-900">
                    Stripe Connect sandbox withdrawal
                </h2>
                <p className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
                    Test mode only. No real money is transferred. Only the test bank accounts below
                    can be used.
                </p>
                <p className="text-sm text-slate-600">Available: {vnd(balance)}</p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
                <fieldset disabled={mutation.isPending} className="space-y-4 disabled:opacity-60">
                    <div className="space-y-1">
                        <label htmlFor={`${id}-amount`} className="block text-sm font-medium">
                            Amount (VND)
                        </label>
                        <input
                            id={`${id}-amount`}
                            type="number"
                            inputMode="numeric"
                            min={25000}
                            max={100000000}
                            step={250}
                            required
                            value={amount}
                            onChange={(event) => {
                                setAmount(event.target.value);
                                changeIntent();
                            }}
                            className="w-full rounded-lg border border-slate-300 p-2.5"
                        />
                        <p className="text-xs text-slate-500">
                            25,000–100,000,000 VND, in increments of 250 VND.
                        </p>
                    </div>

                    <div className="space-y-1">
                        <label htmlFor={`${id}-bank`} className="block text-sm font-medium">
                            Test bank account
                        </label>
                        <select
                            id={`${id}-bank`}
                            value={testBank}
                            onChange={(event) => {
                                setTestBank(event.target.value as TestBank);
                                changeIntent();
                            }}
                            className="w-full rounded-lg border border-slate-300 p-2.5"
                        >
                            <option value="SUCCESS">Success — 000123456789</option>
                            <option value="FAILURE">Failure — 000111111116</option>
                        </select>
                        <p className="text-xs text-slate-500">
                            Routing: 110000000 · USD · Stripe test accounts
                        </p>
                    </div>

                    {hasPin && (
                        <div className="space-y-1">
                            <label htmlFor={`${id}-pin`} className="block text-sm font-medium">
                                Wallet PIN
                            </label>
                            <input
                                id={`${id}-pin`}
                                type="password"
                                inputMode="numeric"
                                autoComplete="off"
                                pattern="[0-9]{6}"
                                maxLength={6}
                                required
                                value={pin}
                                onChange={(event) => {
                                    setPin(event.target.value);
                                    setError(null);
                                }}
                                className="w-full rounded-lg border border-slate-300 p-2.5"
                            />
                        </div>
                    )}
                </fieldset>

                <p className="rounded-lg bg-slate-50 p-3 text-sm text-slate-600">
                    Demo conversion: 25,000 VND = 1 USD. Fixed simulation rate, not a market
                    exchange rate.
                    {Number.isFinite(Number(amount)) &&
                        Number(amount) > 0 &&
                        ` Estimated test payout: ${(Number(amount) / 25000).toFixed(2)} USD.`}
                </p>

                {error && (
                    <p role="alert" className="text-sm text-rose-700">
                        {error}
                    </p>
                )}
                {wallet.isError && (
                    <p role="alert" className="text-sm text-rose-700">
                        Unable to verify wallet/PIN settings.{" "}
                        <button
                            type="button"
                            className="underline"
                            onClick={() => void wallet.refetch()}
                        >
                            Refresh wallet
                        </button>
                    </p>
                )}

                <Button
                    type="submit"
                    className="w-full"
                    isLoading={mutation.isPending}
                    disabled={
                        !walletReady ||
                        currency !== "VND" ||
                        mutation.isPending ||
                        Boolean(submitted)
                    }
                >
                    {submitted
                        ? "Request recorded — see status below"
                        : error
                          ? "Retry same test withdrawal"
                          : "Submit test withdrawal"}
                </Button>
            </form>

            {latest && (
                <div
                    role="status"
                    className={`rounded-lg border p-3 text-sm ${latest.status === "FAILED" || latest.status === "RETURNED" || latest.status === "REVIEW_REQUIRED" ? "border-rose-200 bg-rose-50" : "border-slate-200 bg-slate-50"}`}
                >
                    <p className="font-medium">{statusLabels[latest.status]}</p>
                    <p className="break-all text-xs">Request: {latest.id}</p>
                    {latest.error && <p>{latest.error}</p>}
                    <button
                        type="button"
                        className="mt-2 underline"
                        onClick={() => {
                            changeIntent();
                            setPin("");
                        }}
                    >
                        Start a new test withdrawal
                    </button>
                </div>
            )}

            <div className="space-y-3 border-t border-slate-200 pt-4">
                <div className="flex items-center justify-between gap-2">
                    <h3 className="font-semibold text-slate-900">Latest 50 sandbox withdrawals</h3>
                    <button
                        type="button"
                        disabled={history.isFetching || !walletId}
                        onClick={() => void history.refetch()}
                        className="text-sm text-blue-700 underline disabled:opacity-50"
                    >
                        Refresh
                    </button>
                </div>
                <p className="text-xs text-slate-500">
                    Status refreshes every 3 seconds while this panel is open.
                </p>
                {history.isPending && <p className="text-sm">Loading withdrawal history…</p>}
                {history.isError && (
                    <p role="alert" className="text-sm text-rose-700">
                        {history.error.message} Status could not be refreshed.
                    </p>
                )}
                {history.data?.length === 0 && (
                    <p className="text-sm text-slate-500">No sandbox withdrawals yet.</p>
                )}
                <ul className="max-h-80 space-y-3 overflow-y-auto" aria-live="polite">
                    {history.data?.map((item) => (
                        <li
                            key={item.id}
                            className="space-y-1 rounded-lg border border-slate-200 p-3 text-sm"
                        >
                            <p className="font-medium">
                                {vnd(item.amount)} → {item.payoutAmount.toFixed(2)}{" "}
                                {item.payoutCurrency}
                            </p>
                            <p>{statusLabels[item.status]}</p>
                            <p className="text-xs text-slate-500">
                                {new Date(item.createdAt).toLocaleString()} ·{" "}
                                {item.testBank === "SUCCESS" ? "000123456789" : "000111111116"}
                            </p>
                            <p className="break-all text-xs text-slate-500">{item.id}</p>
                            {item.payoutId && (
                                <p className="break-all text-xs">Payout: {item.payoutId}</p>
                            )}
                            {item.error && <p className="text-rose-700">{item.error}</p>}
                        </li>
                    ))}
                </ul>
            </div>
        </section>
    );
}
