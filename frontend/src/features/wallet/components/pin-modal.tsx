"use client";

import { useState, useRef, useEffect } from "react";
import { Lock, ShieldCheck, X, AlertCircle, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";

interface PinModalProps {
    isOpen: boolean;
    onClose: () => void;
    onConfirm: (pin: string) => Promise<void> | void;
    title?: string;
    description?: string;
    amount?: number;
    currency?: string;
    isLoading?: boolean;
    errorMessage?: string | null;
}

export function PinModal({
    isOpen,
    onClose,
    onConfirm,
    title = "Xác nhận mã PIN ví",
    description = "Nhập mã PIN 6 số để hoàn tất xác thực giao dịch",
    amount,
    currency = "VND",
    isLoading = false,
    errorMessage = null,
}: PinModalProps) {
    const [pin, setPin] = useState<string[]>(["", "", "", "", "", ""]);
    const [localError, setLocalError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [shake, setShake] = useState(false);
    const inputsRef = useRef<(HTMLInputElement | null)[]>([]);

    useEffect(() => {
        if (isOpen) {
            setPin(["", "", "", "", "", ""]);
            setLocalError(null);
            setTimeout(() => {
                inputsRef.current[0]?.focus();
            }, 100);
        }
    }, [isOpen]);

    useEffect(() => {
        if (errorMessage) {
            setLocalError(errorMessage);
            triggerShake();
        }
    }, [errorMessage]);

    const triggerShake = () => {
        setShake(true);
        setTimeout(() => setShake(false), 500);
    };

    if (!isOpen) return null;

    const handleChange = (index: number, value: string) => {
        setLocalError(null);

        // Handle pasting multiple digits
        if (value.length > 1) {
            const digits = value.replace(/\D/g, "").slice(0, 6).split("");
            if (digits.length > 0) {
                const newPin = [...pin];
                for (let i = 0; i < 6; i++) {
                    newPin[i] = digits[i] || "";
                }
                setPin(newPin);
                const nextFocus = Math.min(digits.length, 5);
                inputsRef.current[nextFocus]?.focus();
                return;
            }
        }

        const digit = value.replace(/\D/g, "").slice(-1);
        const newPin = [...pin];
        newPin[index] = digit;
        setPin(newPin);

        if (digit && index < 5) {
            inputsRef.current[index + 1]?.focus();
        }
    };

    const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Backspace") {
            if (!pin[index] && index > 0) {
                inputsRef.current[index - 1]?.focus();
            }
        } else if (e.key === "ArrowLeft" && index > 0) {
            inputsRef.current[index - 1]?.focus();
        } else if (e.key === "ArrowRight" && index < 5) {
            inputsRef.current[index + 1]?.focus();
        } else if (e.key === "Enter" && pin.every((d) => d !== "")) {
            handleSubmit();
        }
    };

    const handlePaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
        e.preventDefault();
        const pasted = e.clipboardData.getData("text").replace(/\D/g, "").slice(0, 6);
        if (pasted) {
            const digits = pasted.split("");
            const newPin = ["", "", "", "", "", ""];
            for (let i = 0; i < digits.length; i++) {
                newPin[i] = digits[i];
            }
            setPin(newPin);
            const nextIdx = Math.min(digits.length, 5);
            inputsRef.current[nextIdx]?.focus();
        }
    };

    const handleSubmit = async () => {
        const fullPin = pin.join("");
        if (fullPin.length !== 6) {
            setLocalError("Vui lòng nhập đầy đủ 6 chữ số mã PIN");
            triggerShake();
            return;
        }

        setIsSubmitting(true);
        setLocalError(null);

        try {
            await onConfirm(fullPin);
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : "Mã PIN không chính xác";
            setLocalError(msg);
            triggerShake();
            setPin(["", "", "", "", "", ""]);
            inputsRef.current[0]?.focus();
        } finally {
            setIsSubmitting(false);
        }
    };

    const isComplete = pin.every((d) => d !== "");
    const busy = isLoading || isSubmitting;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200">
            <div
                className={`bg-white rounded-3xl max-w-sm w-full p-6 sm:p-7 shadow-2xl border border-slate-100 relative transition-transform ${
                    shake ? "animate-shake" : ""
                }`}
                style={{
                    animation: shake ? "shake 0.45s cubic-bezier(0.36, 0.07, 0.19, 0.97) both" : undefined,
                }}
            >
                {/* Close Button */}
                <button
                    onClick={onClose}
                    disabled={busy}
                    className="absolute right-5 top-5 p-1.5 rounded-full text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
                >
                    <X className="w-5 h-5" />
                </button>

                {/* Header Icon */}
                <div className="flex flex-col items-center text-center space-y-2 mb-6">
                    <div className="w-14 h-14 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center shadow-xs">
                        <Lock className="w-7 h-7 stroke-[1.75]" />
                    </div>
                    <h3 className="text-lg font-bold text-slate-900 tracking-tight">{title}</h3>
                    <p className="text-xs text-slate-500 max-w-[280px] leading-relaxed">
                        {description}
                    </p>
                    {amount !== undefined && (
                        <div className="mt-2 py-1 px-3 bg-slate-50 border border-slate-200/80 rounded-xl">
                            <span className="text-xs text-slate-500 mr-1.5">Số tiền giao dịch:</span>
                            <span className="text-sm font-bold text-slate-900 font-mono">
                                {new Intl.NumberFormat("vi-VN", { style: "currency", currency }).format(amount)}
                            </span>
                        </div>
                    )}
                </div>

                {/* 6 PIN Cells */}
                <div className="flex justify-center items-center gap-2.5 mb-5" onPaste={handlePaste}>
                    {pin.map((digit, index) => (
                        <div key={index} className="relative">
                            <input
                                ref={(el) => {
                                    inputsRef.current[index] = el;
                                }}
                                type="password"
                                inputMode="numeric"
                                pattern="[0-9]*"
                                maxLength={1}
                                value={digit}
                                disabled={busy}
                                onChange={(e) => handleChange(index, e.target.value)}
                                onKeyDown={(e) => handleKeyDown(index, e)}
                                className={`w-11 h-13 text-center text-xl font-bold font-mono rounded-xl border transition-all outline-hidden cursor-pointer ${
                                    digit
                                        ? "border-blue-600 bg-blue-50/40 text-blue-900 shadow-2xs"
                                        : "border-slate-200 bg-slate-50/70 text-slate-800 hover:border-slate-300 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-100"
                                }`}
                            />
                        </div>
                    ))}
                </div>

                {/* Error Banner */}
                {localError && (
                    <div className="mb-4 p-3 rounded-xl bg-rose-50 border border-rose-200/80 text-xs text-rose-600 flex items-start space-x-2">
                        <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                        <span className="leading-snug">{localError}</span>
                    </div>
                )}

                {/* Action Buttons */}
                <div className="space-y-2 pt-1">
                    <Button
                        type="button"
                        variant="primary"
                        disabled={!isComplete || busy}
                        onClick={handleSubmit}
                        className="w-full h-11 text-sm font-semibold rounded-xl cursor-pointer shadow-sm flex items-center justify-center gap-2"
                    >
                        {busy ? (
                            <>
                                <Loader2 className="w-4 h-4 animate-spin" />
                                Đang xác thực...
                            </>
                        ) : (
                            <>
                                <ShieldCheck className="w-4 h-4" />
                                Xác nhận
                            </>
                        )}
                    </Button>

                    <Button
                        type="button"
                        variant="outline"
                        disabled={busy}
                        onClick={onClose}
                        className="w-full h-10 text-xs text-slate-600 rounded-xl cursor-pointer"
                    >
                        Hủy bỏ
                    </Button>
                </div>
            </div>

            <style jsx global>{`
                @keyframes shake {
                    10%, 90% { transform: translate3d(-1px, 0, 0); }
                    20%, 80% { transform: translate3d(2px, 0, 0); }
                    30%, 50%, 70% { transform: translate3d(-4px, 0, 0); }
                    40%, 60% { transform: translate3d(4px, 0, 0); }
                }
            `}</style>
        </div>
    );
}
