import * as React from "react";
import { cn } from "@/lib/utils";

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
    error?: string;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
    ({ className, type, error, ...props }, ref) => {
        return (
            <div className="w-full">
                <input
                    type={type}
                    className={cn(
                        "flex h-9.5 w-full rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-900 transition-colors placeholder:text-slate-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-blue-600 focus-visible:border-blue-600 disabled:cursor-not-allowed disabled:opacity-50",
                        error && "border-rose-500 focus-visible:ring-rose-500",
                        className
                    )}
                    ref={ref}
                    {...props}
                />
                {error && <p className="mt-1 text-[11px] text-rose-600">{error}</p>}
            </div>
        );
    }
);
Input.displayName = "Input";
