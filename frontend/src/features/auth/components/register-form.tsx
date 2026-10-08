"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
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
                    Full Name
                </Label>
                <Input
                    id="fullName"
                    placeholder="John Doe"
                    error={errors.fullName?.message}
                    {...register("fullName")}
                />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                <div>
                    <Label htmlFor="email" required>
                        Email
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
                        Phone Number
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
                    Date of Birth
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
                        Password
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
                        Confirm Password
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

            <Button type="submit" variant="primary" className="w-full mt-2" isLoading={isSubmitting}>
                Continue to OTP Verification
            </Button>

            <div className="text-center pt-1.5">
                <p className="text-xs text-slate-500">
                    Already have an account?{" "}
                    <button
                        type="button"
                        onClick={() => useAuthStore.getState().setAuthModalTab("login")}
                        className="font-medium text-blue-600 hover:text-blue-700 hover:underline cursor-pointer"
                    >
                        Sign In
                    </button>
                </p>
            </div>
        </form>
    );
}
