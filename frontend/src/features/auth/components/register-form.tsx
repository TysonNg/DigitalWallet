"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { registerSchema, RegisterInput } from "../schemas/auth.schema";
import { registerAction } from "../actions/auth.actions";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuthStore } from "../store/auth.store";

export function RegisterForm() {
    const [serverError, setServerError] = useState<string | null>(null);
    const setPendingEmail = useAuthStore((state) => state.setPendingEmail);
    const setRegistrationStep = useAuthStore((state) => state.setRegistrationStep);

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<RegisterInput>({
        resolver: zodResolver(registerSchema),
        defaultValues: {
            fullName: "",
            email: "",
            phoneNumber: "",
            password: "",
            dateOfBirth: "2000-01-01",
        },
    });

    const onSubmit = async (values: RegisterInput) => {
        setServerError(null);
        const result = await registerAction(values);

        if (!result.success) {
            setServerError(result.message);
            return;
        }

        // Chuyển sang bước 2: Xác thực mã OTP qua Email
        setPendingEmail(values.email);
        setRegistrationStep("verify");
    };

    return (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-3.5">
            {serverError && (
                <div className="rounded-md border border-red-200 bg-red-50 p-3 text-xs text-red-700 dark:border-red-900/50 dark:bg-red-950/30 dark:text-red-400">
                    {serverError}
                </div>
            )}

            <div>
                <Label htmlFor="fullName" required>
                    Họ và tên
                </Label>
                <Input
                    id="fullName"
                    placeholder="Nguyễn Văn A"
                    error={errors.fullName?.message}
                    {...register("fullName")}
                />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <div>
                    <Label htmlFor="email" required>
                        Email
                    </Label>
                    <Input
                        id="email"
                        type="email"
                        placeholder="a@gmail.com"
                        error={errors.email?.message}
                        {...register("email")}
                    />
                </div>

                <div>
                    <Label htmlFor="phoneNumber" required>
                        Số điện thoại
                    </Label>
                    <Input
                        id="phoneNumber"
                        placeholder="0912345678"
                        error={errors.phoneNumber?.message}
                        {...register("phoneNumber")}
                    />
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <div>
                    <Label htmlFor="dateOfBirth" required>
                        Ngày sinh (≥18 tuổi)
                    </Label>
                    <Input
                        id="dateOfBirth"
                        type="date"
                        error={errors.dateOfBirth?.message}
                        {...register("dateOfBirth")}
                    />
                </div>

                <div>
                    <Label htmlFor="password" required>
                        Mật khẩu (≥8 ký tự)
                    </Label>
                    <Input
                        id="password"
                        type="password"
                        placeholder="••••••••"
                        error={errors.password?.message}
                        {...register("password")}
                    />
                </div>
            </div>

            <Button type="submit" className="w-full mt-2" isLoading={isSubmitting}>
                Tiếp tục xác thực OTP
            </Button>

            <div className="text-center pt-2">
                <p className="text-xs text-zinc-500 dark:text-zinc-400">
                    Đã có tài khoản?{" "}
                    <Link
                        href="/login"
                        className="font-medium text-zinc-900 dark:text-zinc-100 hover:underline"
                    >
                        Đăng nhập
                    </Link>
                </p>
            </div>
        </form>
    );
}
