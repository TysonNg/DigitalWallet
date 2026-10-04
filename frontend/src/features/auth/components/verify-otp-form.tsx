"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { verifyOtpSchema, VerifyOtpInput } from "../schemas/auth.schema";
import { verifyOtpAction } from "../actions/auth.actions";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuthStore } from "../store/auth.store";
import { ArrowLeft, MailCheck } from "lucide-react";

export function VerifyOtpForm() {
    const router = useRouter();
    const [serverError, setServerError] = useState<string | null>(null);
    const [countdown, setCountdown] = useState<number>(60);
    const { pendingEmail, setRegistrationStep, resetRegistration, setCurrentUser } = useAuthStore();

    const {
        register,
        handleSubmit,
        setValue,
        formState: { errors, isSubmitting },
    } = useForm<VerifyOtpInput>({
        resolver: zodResolver(verifyOtpSchema),
        defaultValues: {
            email: pendingEmail || "",
            otp: "",
        },
    });

    useEffect(() => {
        if (pendingEmail) {
            setValue("email", pendingEmail);
        }
    }, [pendingEmail, setValue]);

    // Countdown timer for resending OTP
    useEffect(() => {
        if (countdown <= 0) return;
        const timer = setInterval(() => {
            setCountdown((prev) => prev - 1);
        }, 1000);
        return () => clearInterval(timer);
    }, [countdown]);

    const onSubmit = async (values: VerifyOtpInput) => {
        setServerError(null);
        const result = await verifyOtpAction(values);

        if (!result.success) {
            setServerError(result.message);
            return;
        }

        if (result.data) {
            setCurrentUser(result.data);
        }

        resetRegistration();
        router.push("/dashboard");
        router.refresh();
    };

    return (
        <div className="space-y-4">
            <div className="flex items-center space-x-3 rounded-md border border-zinc-200 bg-zinc-50 p-3.5 dark:border-zinc-800 dark:bg-zinc-900/50">
                <MailCheck className="w-5 h-5 text-zinc-700 dark:text-zinc-300 shrink-0" />
                <div className="text-xs">
                    <p className="text-zinc-500 dark:text-zinc-400">
                        Mã OTP 6 chữ số đã được gửi tới:
                    </p>
                    <p className="font-semibold text-zinc-900 dark:text-zinc-100 mt-0.5">
                        {pendingEmail || "email của bạn"}
                    </p>
                </div>
            </div>

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                {serverError && (
                    <div className="rounded-md border border-red-200 bg-red-50 p-3 text-xs text-red-700 dark:border-red-900/50 dark:bg-red-950/30 dark:text-red-400">
                        {serverError}
                    </div>
                )}

                <div>
                    <Label htmlFor="otp" required>
                        Mã xác thực OTP (6 chữ số)
                    </Label>
                    <Input
                        id="otp"
                        maxLength={6}
                        placeholder="123456"
                        className="text-center font-mono text-lg tracking-widest"
                        error={errors.otp?.message}
                        {...register("otp")}
                    />
                </div>

                <Button type="submit" className="w-full" isLoading={isSubmitting}>
                    Kích hoạt tài khoản
                </Button>
            </form>

            <div className="flex items-center justify-between pt-2 text-xs">
                <button
                    type="button"
                    onClick={() => setRegistrationStep("register")}
                    className="inline-flex items-center text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-100 cursor-pointer"
                >
                    <ArrowLeft className="w-3.5 h-3.5 mr-1" /> Quay lại
                </button>

                <div>
                    {countdown > 0 ? (
                        <span className="text-zinc-400 dark:text-zinc-500">
                            Gửi lại mã sau {countdown}s
                        </span>
                    ) : (
                        <button
                            type="button"
                            onClick={() => setCountdown(60)}
                            className="font-medium text-zinc-900 dark:text-zinc-100 hover:underline cursor-pointer"
                        >
                            Gửi lại mã OTP
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}
