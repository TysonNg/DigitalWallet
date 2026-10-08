"use client";

import { useAuthStore } from "../store/auth.store";
import { RegisterForm } from "./register-form";
import { VerifyOtpForm } from "./verify-otp-form";
import { AuthHeader } from "./auth-header";

interface RegisterViewProps {
    showHeader?: boolean;
}

export function RegisterView({ showHeader = false }: RegisterViewProps) {
    const step = useAuthStore((state) => state.registrationStep);

    return (
        <>
            {showHeader && (
                <AuthHeader
                    title={step === "register" ? "Create Digital Wallet Account" : "Verify Email OTP Code"}
                    description={
                        step === "register"
                            ? "Fill in your details to create a secure wallet"
                            : "Enter the verification code sent to your email"
                    }
                />
            )}
            {step === "register" ? <RegisterForm /> : <VerifyOtpForm />}
        </>
    );
}
