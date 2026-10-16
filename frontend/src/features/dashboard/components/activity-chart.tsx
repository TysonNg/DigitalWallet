"use client";

import { useTranslations, useLocale } from "next-intl";

import { useState } from "react";
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from "recharts";

import type { TransactionDto } from "@/features/wallet/types/wallet.types";
import { activityData } from "../dashboard-data";

export function ActivityChart({
    currency = "VND",
    transactions,
    walletId,
}: {
    currency?: string;
    transactions: TransactionDto[];
    walletId: string;
}) {
    const t = useTranslations("dashboard");
    const locale = useLocale() === "vi" ? "vi-VN" : "en-US";
    const formatVND = (value: number) =>
        new Intl.NumberFormat(locale, { style: "currency", currency }).format(value);

    const [period, setPeriod] = useState<"30" | "7">("30");
    const data = activityData(transactions, walletId, Number(period)).map((item) => ({
        ...item,
        date: new Intl.DateTimeFormat(locale, {
            timeZone: "Asia/Ho_Chi_Minh",
            day: "2-digit",
            month: "2-digit",
        }).format(new Date(`${item.date}T00:00:00+07:00`)),
    }));

    return (
        <div className="bg-white rounded-2xl border border-slate-200/90 p-5 sm:p-6 shadow-xs flex flex-col justify-between">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-6">
                <div>
                    <h3 className="font-bold text-base text-slate-900 tracking-tight">
                        {t("activity")} {period === "30" ? t("30Days") : t("7Days")}
                    </h3>
                </div>

                <div className="flex items-center space-x-4">
                    <div className="flex items-center space-x-3 text-xs">
                        <div className="flex items-center space-x-1.5">
                            <span className="w-2 h-2 rounded-full bg-blue-600 inline-block" />
                            <span className="text-slate-600 font-medium"> {t("moneyIn")} </span>
                        </div>
                        <div className="flex items-center space-x-1.5">
                            <span className="w-2 h-2 rounded-full bg-sky-300 inline-block" />
                            <span className="text-slate-600 font-medium"> {t("moneyOut")} </span>
                        </div>
                    </div>

                    <select
                        aria-label={t("activity")}
                        value={period}
                        onChange={(e) => setPeriod(e.target.value as "30" | "7")}
                        className="text-xs font-medium text-slate-700 bg-white border border-slate-200 rounded-lg px-2.5 py-1 focus:outline-none focus:ring-1 focus:ring-blue-600 cursor-pointer"
                    >
                        <option value="30"> {t("last30Days")} </option>
                        <option value="7"> {t("last7Days")} </option>
                    </select>
                </div>
            </div>

            <div className="h-56 w-full -ml-2">
                <ResponsiveContainer width="100%" height="100%">
                    <BarChart
                        data={data}
                        barGap={4}
                        margin={{ top: 10, right: 10, left: -15, bottom: 0 }}
                    >
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                        <XAxis
                            dataKey="date"
                            tickLine={false}
                            axisLine={false}
                            tick={{ fontSize: 11, fill: "#94a3b8" }}
                        />
                        <YAxis
                            tickLine={false}
                            axisLine={false}
                            tick={{ fontSize: 11, fill: "#94a3b8" }}
                            tickFormatter={(val) =>
                                new Intl.NumberFormat(locale, { notation: "compact" }).format(
                                    Number(val)
                                )
                            }
                            domain={[0, "auto"]}
                        />
                        <Tooltip
                            content={({ active, payload, label }) => {
                                if (active && payload && payload.length) {
                                    return (
                                        <div className="bg-slate-900 text-white text-xs rounded-lg p-2.5 shadow-md space-y-1">
                                            <p className="font-medium text-slate-400 border-b border-slate-800 pb-1 mb-1">
                                                {t("date")} {label}
                                            </p>
                                            <p className="flex justify-between gap-3 text-sky-300">
                                                <span> {t("moneyIn")} </span>
                                                <span className="font-semibold">
                                                    {formatVND(Number(payload[0].value))}
                                                </span>
                                            </p>
                                            <p className="flex justify-between gap-3 text-slate-300">
                                                <span> {t("moneyOut")} </span>
                                                <span className="font-semibold">
                                                    {formatVND(Number(payload[1].value))}
                                                </span>
                                            </p>
                                        </div>
                                    );
                                }
                                return null;
                            }}
                        />
                        <Bar
                            dataKey="inAmount"
                            name={t("moneyIn")}
                            fill="#2563eb"
                            radius={[2, 2, 0, 0]}
                            barSize={6.5}
                        />
                        <Bar
                            dataKey="outAmount"
                            name={t("moneyOut")}
                            fill="#93c5fd"
                            radius={[2, 2, 0, 0]}
                            barSize={6.5}
                        />
                    </BarChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
}
