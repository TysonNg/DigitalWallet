"use client";

import { useTranslations } from "next-intl";

import { useAuthStore } from "../store/auth.store";
import { RegisterForm } from "./register-form";
import { VerifyOtpForm } from "./verify-otp-form";
import { AuthHeader } from "./auth-header";

interface RegisterViewProps {
    showHeader?: boolean;
}

export function RegisterView({ showHeader = false }: RegisterViewProps) {
    const t = useTranslations("auth");
    const step = useAuthStore((state) => state.registrationStep);

    return (
        <>
            {showHeader && (
                <AuthHeader
                    title={step === "register" ? t("registerTitle") : t("emailOtpTitle")}
                    description={
                        step === "register"
                            ? t("registerPageDescription")
                            : t("verifyPageDescription")
                    }
                />
            )}
            {step === "register" ? <RegisterForm /> : <VerifyOtpForm />}
        </>
    );
}
