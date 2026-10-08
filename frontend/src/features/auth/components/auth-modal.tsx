"use client";

import { useEffect } from "react";
import { useSearchParams, useRouter, usePathname } from "next/navigation";
import { useAuthStore } from "../store/auth.store";
import { LoginForm } from "./login-form";
import { RegisterView } from "./register-view";
import { X } from "lucide-react";

export function AuthModal() {
    const {
        isAuthModalOpen,
        authModalTab,
        registrationStep,
        closeAuthModal,
        setAuthModalTab,
        openAuthModal,
    } = useAuthStore();
    const searchParams = useSearchParams();
    const router = useRouter();
    const pathname = usePathname();

    useEffect(() => {
        const authQuery = searchParams.get("auth");
        if (authQuery === "login" || authQuery === "register") {
            openAuthModal(authQuery);
        }
    }, [searchParams, openAuthModal]);

    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.key === "Escape" && isAuthModalOpen) {
                handleClose();
            }
        };
        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [isAuthModalOpen]);

    const handleClose = () => {
        closeAuthModal();
        if (searchParams.get("auth")) {
            router.replace(pathname);
        }
    };

    if (!isAuthModalOpen) return null;

    const getTitle = () => {
        if (authModalTab === "login") return "Sign In";
        if (registrationStep === "verify") return "Verify OTP Code";
        return "Create an Account";
    };

    const getDescription = () => {
        if (authModalTab === "login") return "Enter your email and password to continue.";
        if (registrationStep === "verify")
            return "Enter the 6-digit verification code sent to your email inbox.";
        return "Fill in your details to activate your secure digital wallet.";
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto">
            {/* Minimalist Backdrop */}
            <div
                className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs transition-opacity duration-150"
                onClick={handleClose}
                aria-hidden="true"
            />

            {/* Modal Dialog Card */}
            <div className="relative w-full max-w-sm sm:max-w-md bg-white rounded-2xl shadow-xl border border-slate-200/90 p-6 sm:p-7 z-10 duration-150 my-auto">
                {/* Close Button */}
                <button
                    onClick={handleClose}
                    className="absolute top-4 right-4 p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
                    aria-label="Close"
                >
                    <X className="w-4 h-4 stroke-[1.75]" />
                </button>

                {/* Header (Clean, no floating icon box) */}
                <div className="mb-5 pr-6">
                    <h2 className="text-base sm:text-lg font-bold text-slate-900 tracking-tight">
                        {getTitle()}
                    </h2>
                    <p className="text-xs text-slate-500 mt-1 leading-normal">{getDescription()}</p>
                </div>

                {/* Tab Switcher (Minimalist segmented) */}
                {registrationStep !== "verify" && (
                    <div className="flex bg-slate-100 p-0.5 rounded-lg mb-5 border border-slate-200/60">
                        <button
                            type="button"
                            onClick={() => setAuthModalTab("login")}
                            className={`flex-1 py-1.5 text-xs font-medium rounded-md transition-all cursor-pointer ${
                                authModalTab === "login"
                                    ? "bg-white text-slate-900 shadow-xs font-semibold"
                                    : "text-slate-500 hover:text-slate-900"
                            }`}
                        >
                            Sign In
                        </button>
                        <button
                            type="button"
                            onClick={() => setAuthModalTab("register")}
                            className={`flex-1 py-1.5 text-xs font-medium rounded-md transition-all cursor-pointer ${
                                authModalTab === "register"
                                    ? "bg-white text-slate-900 shadow-xs font-semibold"
                                    : "text-slate-500 hover:text-slate-900"
                            }`}
                        >
                            Register
                        </button>
                    </div>
                )}

                {/* Form Body */}
                <div className="max-h-[70vh] overflow-y-auto px-0.5">
                    {authModalTab === "login" ? <LoginForm /> : <RegisterView showHeader={false} />}
                </div>
            </div>
        </div>
    );
}
