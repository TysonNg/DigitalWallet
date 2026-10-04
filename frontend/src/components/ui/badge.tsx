import * as React from "react";
import { cn } from "@/lib/utils";

export interface BadgeProps extends React.HTMLAttributes<HTMLDivElement> {
    variant?: "default" | "success" | "warning" | "danger" | "outline";
}

export function Badge({ className, variant = "default", ...props }: BadgeProps) {
    const variants = {
        default: "bg-zinc-100 text-zinc-900 dark:bg-zinc-800 dark:text-zinc-100",
        success:
            "bg-emerald-50 text-emerald-700 border border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-300 dark:border-emerald-800",
        warning:
            "bg-amber-50 text-amber-700 border border-amber-200 dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-800",
        danger: "bg-red-50 text-red-700 border border-red-200 dark:bg-red-950/40 dark:text-red-300 dark:border-red-800",
        outline: "border border-zinc-200 text-zinc-800 dark:border-zinc-800 dark:text-zinc-200",
    };

    return (
        <div
            className={cn(
                "inline-flex items-center rounded-xs px-2 py-0.5 text-xs font-medium tracking-wide uppercase",
                variants[variant],
                className
            )}
            {...props}
        />
    );
}
