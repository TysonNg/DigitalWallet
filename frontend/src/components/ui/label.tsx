"use client";

import { useTranslations } from "next-intl";
import * as React from "react";
import { cn } from "@/lib/utils";

export interface LabelProps extends React.LabelHTMLAttributes<HTMLLabelElement> {
    required?: boolean;
}

export const Label = React.forwardRef<HTMLLabelElement, LabelProps>(
    ({ className, children, required, ...props }, ref) => {
        const t = useTranslations("common");

        return (
            <label
                ref={ref}
                className={cn(
                    "text-xs font-medium text-slate-700 select-none block mb-1.5",
                    className
                )}
                {...props}
            >
                {children}
                {required && (
                    <span className="text-red-500 ml-0.5" aria-label={t("required")}>
                        *
                    </span>
                )}
            </label>
        );
    }
);
Label.displayName = "Label";
