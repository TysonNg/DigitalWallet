"use client";

import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from "recharts";
import { formatVND } from "@/lib/utils";

interface AllocationChartProps {
    mainBalance?: number;
}

export function AllocationChart({ mainBalance = 12450000 }: AllocationChartProps) {
    const savingsBalance = 28300000;
    const travelBalance = 5720000;
    const totalAssets = mainBalance + savingsBalance + travelBalance;

    const mainPercent = ((mainBalance / totalAssets) * 100).toFixed(1);
    const savingsPercent = ((savingsBalance / totalAssets) * 100).toFixed(1);
    const travelPercent = ((travelBalance / totalAssets) * 100).toFixed(1);

    const data = [
        { name: "Ví chính", value: mainBalance, color: "#2563eb", percent: mainPercent },
        { name: "Ví tiết kiệm", value: savingsBalance, color: "#38bdf8", percent: savingsPercent },
        { name: "Ví du lịch", value: travelBalance, color: "#93c5fd", percent: travelPercent },
    ];

    return (
        <div className="bg-white rounded-2xl border border-slate-200/90 p-5 sm:p-6 shadow-xs flex flex-col justify-between">
            <h3 className="font-bold text-base text-slate-900 tracking-tight mb-4">
                Phân bổ ví
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
                            Tổng tài sản
                        </span>
                    </div>
                </div>

                {/* Breakdown Legend List */}
                <div className="flex-1 w-full space-y-2.5 pl-0 sm:pl-3">
                    <div className="flex items-center justify-between text-xs">
                        <div className="flex items-center space-x-2">
                            <span className="w-2 h-2 rounded-full bg-blue-600 inline-block" />
                            <span className="text-slate-600 font-medium">Ví chính</span>
                        </div>
                        <div className="flex items-center space-x-3">
                            <span className="text-slate-400 font-medium">{mainPercent}%</span>
                            <span className="font-semibold text-slate-900 min-w-20 text-right">
                                {formatVND(mainBalance)}
                            </span>
                        </div>
                    </div>

                    <div className="flex items-center justify-between text-xs">
                        <div className="flex items-center space-x-2">
                            <span className="w-2 h-2 rounded-full bg-sky-400 inline-block" />
                            <span className="text-slate-600 font-medium">Ví tiết kiệm</span>
                        </div>
                        <div className="flex items-center space-x-3">
                            <span className="text-slate-400 font-medium">{savingsPercent}%</span>
                            <span className="font-semibold text-slate-900 min-w-20 text-right">
                                {formatVND(savingsBalance)}
                            </span>
                        </div>
                    </div>

                    <div className="flex items-center justify-between text-xs">
                        <div className="flex items-center space-x-2">
                            <span className="w-2 h-2 rounded-full bg-blue-300 inline-block" />
                            <span className="text-slate-600 font-medium">Ví du lịch</span>
                        </div>
                        <div className="flex items-center space-x-3">
                            <span className="text-slate-400 font-medium">{travelPercent}%</span>
                            <span className="font-semibold text-slate-900 min-w-20 text-right">
                                {formatVND(travelBalance)}
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
