"use client";

import { useEffect, useState } from "react";
import { useTranslations, useLocale } from "next-intl";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/ui/button";
import { MoneyInput } from "@/components/ui/money-input";
import { Label } from "@/components/ui/label";
import { CheckCircle2 } from "lucide-react";
import { resolveReceiveRecipientAction } from "../actions/receive-qr.actions";
import type { ReceiveRecipient } from "../receive-qr";

const PRESET_AMOUNTS = [50000, 100000, 200000, 500000, 1000000];

export function ReceiveRecipientView({ token, loginHref }: { token: string; loginHref: string }) {
    const t = useTranslations("receiveQr");
    const locale = useLocale();
    const [attempt, setAttempt] = useState(0);
    const [loading, setLoading] = useState(true);
    const [recipient, setRecipient] = useState<ReceiveRecipient | null>(null);
    const [error, setError] = useState("");
    const [needsLogin, setNeedsLogin] = useState(false);
    const [amount, setAmount] = useState<number>(100000);

    useEffect(() => {
        let active = true;

        async function resolve() {
            try {
                const result = await resolveReceiveRecipientAction(token);
                if (!active) return;
                if (result.success && result.data) {
                    setRecipient(result.data);
                } else {
                    setNeedsLogin(result.errorCode === "SESSION_EXPIRED");
                    setError(result.message || t("failed"));
                }
            } catch {
                if (active) setError(t("failed"));
            } finally {
                if (active) setLoading(false);
            }
        }

        void resolve();

        return () => {
            active = false;
        };
    }, [token, attempt, t]);

    function retry() {
        setLoading(true);
        setRecipient(null);
        setError("");
        setNeedsLogin(false);
        setAttempt((value) => value + 1);
    }

    return (
        <main className="min-h-screen bg-slate-50 px-4 py-16 text-slate-900">
            <section className="mx-auto max-w-md space-y-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
                <div>
                    <h1 className="text-xl font-semibold text-slate-900">{t("recipientTitle")}</h1>
                    <p className="mt-1 text-sm text-slate-500">{t("review")}</p>
                </div>

                {loading && (
                    <div className="flex items-center justify-center py-8">
                        <p role="status" className="text-sm text-slate-500">
                            {t("resolving")}
                        </p>
                    </div>
                )}

                {error && (
                    <p role="alert" className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">
                        {error}
                    </p>
                )}

                {!loading && needsLogin && (
                    <Link
                        href={loginHref}
                        className="inline-block w-full text-center rounded-xl bg-blue-600 px-4 py-2.5 font-medium text-white shadow-sm hover:bg-blue-700 transition"
                    >
                        {t("login")}
                    </Link>
                )}

                {!loading && error && !needsLogin && (
                    <Button variant="outline" onClick={retry} className="w-full">
                        {t("retry")}
                    </Button>
                )}

                {recipient && (
                    <div className="space-y-5">
                        {/* Recipient Card - Bank style without raw wallet ID */}
                        <div className="flex items-center gap-3.5 rounded-xl border border-slate-100 bg-slate-50/80 p-4">
                            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-blue-600 text-lg font-bold text-white shadow-sm">
                                {recipient.fullName ? recipient.fullName.charAt(0).toUpperCase() : "U"}
                            </div>
                            <div className="min-w-0 flex-1">
                                <p className="text-xs font-medium uppercase tracking-wider text-slate-400">
                                    {t("name")}
                                </p>
                                <p className="truncate text-base font-semibold text-slate-900">
                                    {recipient.fullName}
                                </p>
                                <span className="inline-flex items-center gap-1 text-xs font-medium text-emerald-600">
                                    <CheckCircle2 className="h-3.5 w-3.5" />
                                    {t("currency")}: {recipient.currency}
                                </span>
                            </div>
                        </div>

                        {/* Amount Input */}
                        <div className="space-y-2">
                            <Label htmlFor="receive-transfer-amount" className="text-sm font-medium text-slate-700">
                                {t("amount")}
                            </Label>
                            <div className="relative">
                                <MoneyInput
                                    id="receive-transfer-amount"
                                    value={amount}
                                    onValueChange={setAmount}
                                    min={1000}
                                    className="h-12 pr-14 text-lg font-semibold"
                                />
                                <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-sm font-medium text-slate-400">
                                    {recipient.currency}
                                </span>
                            </div>

                            {/* Preset Amount Chips */}
                            <div className="flex flex-wrap gap-2 pt-1">
                                {PRESET_AMOUNTS.map((preset) => (
                                    <button
                                        key={preset}
                                        type="button"
                                        onClick={() => setAmount(preset)}
                                        className={`rounded-lg px-2.5 py-1 text-xs font-medium transition ${
                                            amount === preset
                                                ? "bg-blue-600 text-white"
                                                : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                                        }`}
                                    >
                                        {new Intl.NumberFormat(locale === "vi" ? "vi-VN" : "en-US").format(preset)}
                                    </button>
                                ))}
                            </div>
                        </div>

                        {/* Submit Button */}
                        <Link
                            href={`/dashboard/transfer?recipient=${encodeURIComponent(recipient.walletId)}${amount > 0 ? `&amount=${amount}` : ""}`}
                            className="inline-flex w-full items-center justify-center rounded-xl bg-blue-600 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700"
                        >
                            {t("continue")}
                        </Link>
                    </div>
                )}
            </section>
        </main>
    );
}
