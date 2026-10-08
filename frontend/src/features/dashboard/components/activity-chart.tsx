"use client";

import { useState } from "react";
import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    Tooltip,
    ResponsiveContainer,
    CartesianGrid,
} from "recharts";
import { formatVND } from "@/lib/utils";

const DATA_30_DAYS = [
    { date: "25/03", inAmount: 1.2, outAmount: 4.0 },
    { date: "28/03", inAmount: 1.5, outAmount: 2.6 },
    { date: "31/03", inAmount: 7.5, outAmount: 4.1 },
    { date: "03/04", inAmount: 2.7, outAmount: 1.9 },
    { date: "06/04", inAmount: 3.1, outAmount: 1.8 },
    { date: "09/04", inAmount: 3.5, outAmount: 1.6 },
    { date: "12/04", inAmount: 3.8, outAmount: 4.1 },
    { date: "15/04", inAmount: 1.3, outAmount: 3.1 },
    { date: "18/04", inAmount: 4.1, outAmount: 3.0 },
    { date: "21/04", inAmount: 6.4, outAmount: 2.6 },
    { date: "24/04", inAmount: 3.1, outAmount: 2.7 },
];

const DATA_7_DAYS = [
    { date: "18/04", inAmount: 4.1, outAmount: 3.0 },
    { date: "19/04", inAmount: 2.2, outAmount: 1.8 },
    { date: "20/04", inAmount: 1.9, outAmount: 2.1 },
    { date: "21/04", inAmount: 6.4, outAmount: 2.6 },
    { date: "22/04", inAmount: 2.5, outAmount: 1.2 },
    { date: "23/04", inAmount: 2.8, outAmount: 1.5 },
    { date: "24/04", inAmount: 3.1, outAmount: 2.7 },
];

export function ActivityChart() {
    const [period, setPeriod] = useState<"30" | "7">("30");
    const data = period === "30" ? DATA_30_DAYS : DATA_7_DAYS;

    return (
        <div className="bg-white rounded-2xl border border-slate-200/90 p-5 sm:p-6 shadow-xs flex flex-col justify-between">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-6">
                <div>
                    <h3 className="font-bold text-base text-slate-900 tracking-tight">
                        Hoạt động {period === "30" ? "30 ngày" : "7 ngày"}
                    </h3>
                </div>

                <div className="flex items-center space-x-4">
                    <div className="flex items-center space-x-3 text-xs">
                        <div className="flex items-center space-x-1.5">
                            <span className="w-2 h-2 rounded-full bg-blue-600 inline-block" />
                            <span className="text-slate-600 font-medium">Tiền vào</span>
                        </div>
                        <div className="flex items-center space-x-1.5">
                            <span className="w-2 h-2 rounded-full bg-sky-300 inline-block" />
                            <span className="text-slate-600 font-medium">Tiền ra</span>
                        </div>
                    </div>

                    <select
                        value={period}
                        onChange={(e) => setPeriod(e.target.value as "30" | "7")}
                        className="text-xs font-medium text-slate-700 bg-white border border-slate-200 rounded-lg px-2.5 py-1 focus:outline-none focus:ring-1 focus:ring-blue-600 cursor-pointer"
                    >
                        <option value="30">30 ngày qua</option>
                        <option value="7">7 ngày qua</option>
                    </select>
                </div>
            </div>

            <div className="h-56 w-full -ml-2">
                <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={data} barGap={4} margin={{ top: 10, right: 10, left: -15, bottom: 0 }}>
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
                            tickFormatter={(val) => `${val}M`}
                            domain={[0, 10]}
                        />
                        <Tooltip
                            content={({ active, payload, label }) => {
                                if (active && payload && payload.length) {
                                    return (
                                        <div className="bg-slate-900 text-white text-xs rounded-lg p-2.5 shadow-md space-y-1">
                                            <p className="font-medium text-slate-400 border-b border-slate-800 pb-1 mb-1">
                                                Ngày {label}
                                            </p>
                                            <p className="flex justify-between gap-3 text-sky-300">
                                                <span>Tiền vào:</span>
                                                <span className="font-semibold">
                                                    {formatVND(Number(payload[0].value) * 1000000)}
                                                </span>
                                            </p>
                                            <p className="flex justify-between gap-3 text-slate-300">
                                                <span>Tiền ra:</span>
                                                <span className="font-semibold">
                                                    {formatVND(Number(payload[1].value) * 1000000)}
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
                            fill="#2563eb"
                            radius={[2, 2, 0, 0]}
                            barSize={6.5}
                        />
                        <Bar
                            dataKey="outAmount"
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
