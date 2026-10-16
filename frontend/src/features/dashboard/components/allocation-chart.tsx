"use client";

import { useTranslations, useLocale } from "next-intl";

import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from "recharts";

interface AllocationChartProps {
    mainBalance: number;
    currency?: string;
}

export function AllocationChart({ mainBalance, currency = "VND" }: AllocationChartProps) {
    const t = useTranslations("dashboard");
    const locale = useLocale() === "vi" ? "vi-VN" : "en-US";
    const formatVND = (value: number) =>
        new Intl.NumberFormat(locale, { style: "currency", currency }).format(value);

    const totalAssets = mainBalance;

    const mainPercent = new Intl.NumberFormat(locale, {
        maximumFractionDigits: 1,
        minimumFractionDigits: 1,
    }).format(totalAssets > 0 ? 100 : 0);
    const data = [
        { name: t("mainWallet"), value: mainBalance, color: "#2563eb", percent: mainPercent },
    ];

    return (
        <div className="bg-white rounded-2xl border border-slate-200/90 p-5 sm:p-6 shadow-xs flex flex-col justify-between">
            <h3 className="font-bold text-base text-slate-900 tracking-tight mb-4">
                {t("walletAllocation")}{" "}
            </h3>

            <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
                {/* Donut Chart */}
                <div className="relative w-44 h-44 shrink-0 flex items-center justify-center">
                    <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                            <Pie
                                data={data}
                                cx="50%"
                                cy="50%"
                                innerRadius={52}
                                outerRadius={70}
                                paddingAngle={3}
                                dataKey="value"
                                stroke="none"
                            >
                                {data.map((entry, index) => (
                                    <Cell key={`cell-${index}`} fill={entry.color} />
                                ))}
                            </Pie>
                            <Tooltip
                                content={({ active, payload }) => {
                                    if (active && payload && payload.length) {
                                        const item = payload[0].payload;
                                        return (
                                            <div className="bg-slate-900 text-white text-xs rounded-lg p-2 shadow-md">
                                                <p className="font-semibold">{item.name}</p>
                                                <p className="text-slate-300">
                                                    {formatVND(item.value)} ({item.percent}%)
                                                </p>
                                            </div>
                                        );
                                    }
                                    return null;
                                }}
                            />
                        </PieChart>
                    </ResponsiveContainer>

                    {/* Centered Asset Total */}
                    <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none text-center">
                        <span className="text-xs font-bold text-slate-900 leading-tight">
                            {formatVND(totalAssets)}
                        </span>
                        <span className="text-[10px] text-slate-400 font-medium">
                            {t("totalAssets")}{" "}
                        </span>
                    </div>
                </div>

                {/* Breakdown Legend List */}
                <div className="flex-1 w-full space-y-2.5 pl-0 sm:pl-3">
                    <div className="flex items-center justify-between text-xs">
                        <div className="flex items-center space-x-2">
                            <span className="w-2 h-2 rounded-full bg-blue-600 inline-block" />
                            <span className="text-slate-600 font-medium"> {t("mainWallet")} </span>
                        </div>
                        <div className="flex items-center space-x-3">
                            <span className="text-slate-400 font-medium">{mainPercent}%</span>
                            <span className="font-semibold text-slate-900 min-w-20 text-right">
                                {formatVND(mainBalance)}
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
