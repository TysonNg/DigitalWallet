"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
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
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-3.5">
            {serverError && (
                <div className="rounded-lg border border-rose-200 bg-rose-50 p-2.5 text-xs text-rose-700">
                    {serverError}
                </div>
            )}

            <div>
                <Label htmlFor="email" required>
                    Email Address
                </Label>
                <Input
                    id="email"
                    type="email"
                    autoComplete="email"
                    placeholder="email@example.com"
                    error={errors.email?.message}
                    {...register("email")}
                />
            </div>

            <div>
                <div className="flex items-center justify-between mb-1">
                    <Label htmlFor="password" required className="mb-0">
                        Password
                    </Label>
                    <span className="text-[11px] text-slate-400 hover:text-slate-700 cursor-pointer">
                        Forgot password?
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

            <Button type="submit" variant="primary" className="w-full mt-1" isLoading={isSubmitting}>
                Sign In
            </Button>

            <div className="text-center pt-2">
                <p className="text-xs text-slate-500">
                    Don't have a wallet account?{" "}
                    <button
                        type="button"
                        onClick={() => useAuthStore.getState().setAuthModalTab("register")}
                        className="font-medium text-blue-600 hover:text-blue-700 hover:underline cursor-pointer"
                    >
                        Register now
                    </button>
                </p>
            </div>
        </form>
    );
}
