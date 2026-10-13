"use client";

import { useTranslations } from "next-intl";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { createRegisterSchema, RegisterInput } from "../schemas/auth.schema";
import { registerAction } from "../actions/auth.actions";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuthStore } from "../store/auth.store";

export function RegisterForm() {
    const t = useTranslations("auth");
    const validation = useTranslations("authValidation");
    const [serverError, setServerError] = useState<string | null>(null);
    const setPendingEmail = useAuthStore((state) => state.setPendingEmail);
    const setRegistrationStep = useAuthStore((state) => state.setRegistrationStep);

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<RegisterInput>({
        resolver: zodResolver(createRegisterSchema(validation)),
        defaultValues: {
            fullName: "",
            email: "",
            phoneNumber: "",
            dateOfBirth: "2000-01-01",
            password: "",
            confirmPassword: "",
        },
    });

    const onSubmit = async (values: RegisterInput) => {
        setServerError(null);
        const result = await registerAction(values);

        if (!result.success) {
            setServerError(result.message);
            return;
        }

        setPendingEmail(values.email);
        setRegistrationStep("verify");
    };

    return (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
            {serverError && (
                <div className="rounded-lg border border-rose-200 bg-rose-50 p-2.5 text-xs text-rose-700">
                    {serverError}
                </div>
            )}

            <div>
                <Label htmlFor="fullName" required>
                    {t("fullName")}
                </Label>
                <Input
                    id="fullName"
                    placeholder={t("namePlaceholder")}
                    error={errors.fullName?.message}
                    {...register("fullName")}
                />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                <div>
                    <Label htmlFor="email" required>
                        {t("email")}
                    </Label>
                    <Input
                        id="email"
                        type="email"
                        placeholder="john@example.com"
                        error={errors.email?.message}
                        {...register("email")}
                    />
                </div>

                <div>
                    <Label htmlFor="phoneNumber" required>
                        {t("phoneNumber")}
                    </Label>
                    <Input
                        id="phoneNumber"
                        placeholder="0912345678"
                        error={errors.phoneNumber?.message}
                        {...register("phoneNumber")}
                    />
                </div>
            </div>

            <div>
                <Label htmlFor="dateOfBirth" required>
                    {t("dateOfBirth")}
                </Label>
                <Input
                    id="dateOfBirth"
                    type="date"
                    error={errors.dateOfBirth?.message}
                    {...register("dateOfBirth")}
                />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                <div>
                    <Label htmlFor="password" required>
                        {t("password")}
                    </Label>
                    <Input
                        id="password"
                        type="password"
                        placeholder="••••••••"
                        error={errors.password?.message}
                        {...register("password")}
                    />
                </div>

                <div>
                    <Label htmlFor="confirmPassword" required>
                        {t("confirmPassword")}
                    </Label>
                    <Input
                        id="confirmPassword"
                        type="password"
                        placeholder="••••••••"
                        error={errors.confirmPassword?.message}
                        {...register("confirmPassword")}
                    />
                </div>
            </div>

            <Button
                type="submit"
                variant="primary"
                className="w-full mt-2"
                isLoading={isSubmitting}
            >
                {t("continueOtp")}
            </Button>

            <div className="text-center pt-1.5">
                <p className="text-xs text-slate-500">
                    {t("hasAccount")}{" "}
                    <button
                        type="button"
                        onClick={() => useAuthStore.getState().setAuthModalTab("login")}
                        className="font-medium text-blue-600 hover:text-blue-700 hover:underline cursor-pointer"
                    >
                        {t("signIn")}
                    </button>
                </p>
            </div>
        </form>
    );
}
