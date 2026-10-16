"use client";

import { useLayoutEffect, useRef } from "react";
import { useLocale, useTranslations } from "next-intl";
import { Input, type InputProps } from "./input";
import { formatMoneyInput, parseMoneyInput } from "@/lib/money-input";

interface MoneyInputProps extends Omit<InputProps, "type" | "value" | "onChange" | "inputMode"> {
    value: number;
    onValueChange: (value: number) => void;
}

export function MoneyInput({ value, onValueChange, min, max, step, ...props }: MoneyInputProps) {
    const locale = useLocale();
    const t = useTranslations("wallet");
    const inputRef = useRef<HTMLInputElement>(null);
    const selection = useRef<number | null>(null);

    useLayoutEffect(() => {
        const input = inputRef.current;
        if (!input) return;

        let message = "";
        if (value && min !== undefined && value < Number(min)) {
            message = t("amountMinimum", { amount: formatMoneyInput(Number(min), locale) });
        } else if (value && max !== undefined && value > Number(max)) {
            message = t("amountMaximum", { amount: formatMoneyInput(Number(max), locale) });
        } else if (
            value &&
            step !== undefined &&
            step !== "any" &&
            (value - Number(min ?? 0)) % Number(step) !== 0
        ) {
            message = t("amountStep", { amount: formatMoneyInput(Number(step), locale) });
        }
        input.setCustomValidity(message);

        if (selection.current !== null) {
            input.setSelectionRange(selection.current, selection.current);
            selection.current = null;
        }
    });

    return (
        <Input
            {...props}
            ref={inputRef}
            type="text"
            inputMode="numeric"
            value={formatMoneyInput(value, locale)}
            onChange={(event) => {
                const input = event.target;
                const amount = parseMoneyInput(input.value.replace(/[.,\s]/g, ""));
                if (amount === null) {
                    input.value = formatMoneyInput(value, locale);
                    return;
                }

                const digitsBeforeCaret = input.value
                    .slice(0, input.selectionStart ?? input.value.length)
                    .replace(/\D/g, "").length;
                const formatted = formatMoneyInput(amount, locale);
                let position = 0;
                let digits = 0;
                while (position < formatted.length && digits < digitsBeforeCaret) {
                    if (/\d/.test(formatted[position])) digits++;
                    position++;
                }
                input.value = formatted;
                input.setSelectionRange(position, position);
                selection.current = position;
                onValueChange(amount);
            }}
            onPaste={(event) => {
                props.onPaste?.(event);
                if (event.defaultPrevented) return;

                // Validate pasted groups before normal editing removes separators.
                if (parseMoneyInput(event.clipboardData.getData("text")) === null) {
                    event.preventDefault();
                }
            }}
            onKeyDown={(event) => {
                props.onKeyDown?.(event);
                if (event.defaultPrevented || event.key !== "Backspace") return;

                const input = event.currentTarget;
                const caret = input.selectionStart ?? 0;
                if (caret !== input.selectionEnd || caret < 2 || /\d/.test(input.value[caret - 1]))
                    return;

                event.preventDefault();
                const digits = input.value.replace(/\D/g, "");
                const index = input.value.slice(0, caret).replace(/\D/g, "").length;
                const amount = parseMoneyInput(digits.slice(0, index - 1) + digits.slice(index));
                if (amount !== null) {
                    const formatted = formatMoneyInput(amount, locale);
                    let position = 0;
                    let count = 0;
                    while (position < formatted.length && count < index - 1) {
                        if (/\d/.test(formatted[position])) count++;
                        position++;
                    }
                    selection.current = position;
                    onValueChange(amount);
                }
            }}
        />
    );
}
