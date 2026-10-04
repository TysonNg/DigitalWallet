"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { loginSchema, LoginInput } from "../schemas/auth.schema";
import { loginAction } from "../actions/auth.actions";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuthStore } from "../store/auth.store";

export function LoginForm() {
    const router = useRouter();
    const [serverError, setServerError] = useState<string | null>(null);
    const setCurrentUser = useAuthStore((state) => state.setCurrentUser);

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<LoginInput>({
        resolver: zodResolver(loginSchema),
        defaultValues: {
            email: "",
            password: "",
        },
    });

    const onSubmit = async (values: LoginInput) => {
        setServerError(null);
        const result = await loginAction(values);

        if (!result.success) {
            setServerError(result.message);
            return;
        }

        if (result.data) {
            setCurrentUser(result.data);
        }

        router.push("/dashboard");
        router.refresh();
    };

    return (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {serverError && (
                <div className="rounded-md border border-red-200 bg-red-50 p-3 text-xs text-red-700 dark:border-red-900/50 dark:bg-red-950/30 dark:text-red-400">
                    {serverError}
                </div>
            )}

            <div>
                <Label htmlFor="email" required>
                    Địa chỉ Email
                </Label>
                <Input
                    id="email"
                    type="email"
                    autoComplete="email"
                    placeholder="tyson@example.com"
                    error={errors.email?.message}
                    {...register("email")}
                />
            </div>

            <div>
                <div className="flex items-center justify-between mb-1.5">
                    <Label htmlFor="password" required className="mb-0">
                        Mật khẩu
                    </Label>
                    <span className="text-xs text-zinc-500 hover:text-zinc-800 dark:hover:text-zinc-200 cursor-pointer">
                        Quên mật khẩu?
                    </span>
                </div>
                <Input
                    id="password"
                    type="password"
                    autoComplete="current-password"
                    placeholder="••••••••"
                    error={errors.password?.message}
                    {...register("password")}
                />
            </div>

            <Button type="submit" className="w-full" isLoading={isSubmitting}>
                Đăng nhập
            </Button>

            <div className="text-center pt-2">
                <p className="text-xs text-zinc-500 dark:text-zinc-400">
                    Chưa có tài khoản ví?{" "}
                    <Link
                        href="/register"
                        className="font-medium text-zinc-900 dark:text-zinc-100 hover:underline"
                    >
                        Đăng ký ngay
                    </Link>
                </p>
            </div>
        </form>
    );
}
