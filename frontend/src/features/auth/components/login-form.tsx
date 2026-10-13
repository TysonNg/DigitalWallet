"use client";

import { useLocale, useTranslations } from "next-intl";
import { useSearchParams } from "next/navigation";
import { safeReturnTo } from "@/i18n/paths";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "@/i18n/navigation";
import { toast } from "sonner";
import { createLoginSchema, LoginInput } from "../schemas/auth.schema";
import { loginAction } from "../actions/auth.actions";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuthStore } from "../store/auth.store";

export function LoginForm() {
    const t = useTranslations("auth");
    const validation = useTranslations("authValidation");
    const authErrors = useTranslations("authErrors");
    const router = useRouter();
    const locale = useLocale();
    const searchParams = useSearchParams();
    const [serverError, setServerError] = useState<string | null>(null);
    const setCurrentUser = useAuthStore((state) => state.setCurrentUser);
    const closeAuthModal = useAuthStore((state) => state.closeAuthModal);

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<LoginInput>({
        resolver: zodResolver(createLoginSchema(validation)),
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
            toast.error(result.message || authErrors("invalidCredentials"));
            return;
        }

        if (result.data) {
            setCurrentUser(result.data);
        }

        toast.success(t("welcome", { name: result.data?.fullName || t("welcomeBack") }));
        // safeReturnTo returns a localized URL; next-intl's router adds the prefix itself.
        const destination = safeReturnTo(searchParams.get("returnTo"), locale);
        closeAuthModal();
        router.push(destination.replace(/^\/(?:vi|en)(?=\/)/, ""), { locale });
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
                    {t("emailAddress")}
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
                        {t("password")}
                    </Label>
                    <span className="text-[11px] text-slate-400 hover:text-slate-700 cursor-pointer">
                        {t("forgotPassword")}
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

            <Button
                type="submit"
                variant="primary"
                className="w-full mt-1"
                isLoading={isSubmitting}
            >
                {t("signIn")}
            </Button>

            <div className="text-center pt-2">
                <p className="text-xs text-slate-500">
                    {t("noAccount")}{" "}
                    <button
                        type="button"
                        onClick={() => useAuthStore.getState().setAuthModalTab("register")}
                        className="font-medium text-blue-600 hover:text-blue-700 hover:underline cursor-pointer"
                    >
                        {t("registerNow")}
                    </button>
                </p>
            </div>
        </form>
    );
}
