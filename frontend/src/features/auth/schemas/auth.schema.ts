import { z } from "zod";

export const loginSchema = z.object({
    email: z.string().email("Invalid email address"),
    password: z.string().min(6, "Password must be at least 6 characters"),
});

export type LoginInput = z.infer<typeof loginSchema>;

export const registerSchema = z
    .object({
        fullName: z.string().min(2, "Full name must be at least 2 characters").max(100, "Full name is too long"),
        email: z.string().email("Invalid email address"),
        phoneNumber: z
            .string()
            .regex(/^(0|\+84)[3|5|7|8|9][0-9]{8}$/, "Invalid phone number format"),
        dateOfBirth: z.string().refine(
            (dob) => {
                const birth = new Date(dob);
                const now = new Date();
                const age = now.getFullYear() - birth.getFullYear();
                return age >= 18;
            },
            { message: "User must be at least 18 years old to open a wallet" }
        ),
        password: z
            .string()
            .min(8, "Password must be at least 8 characters")
            .regex(/[A-Z]/, "Password must contain at least 1 uppercase letter")
            .regex(/[0-9]/, "Password must contain at least 1 number"),
        confirmPassword: z.string().min(1, "Please confirm your password"),
    })
    .refine((data) => data.password === data.confirmPassword, {
        message: "Passwords do not match",
        path: ["confirmPassword"],
    });

export type RegisterInput = z.infer<typeof registerSchema>;

export const verifyOtpSchema = z.object({
    email: z.string().email("Invalid email address"),
    otp: z
        .string()
        .length(6, "OTP must be exactly 6 digits")
        .regex(/^[0-9]+$/, "OTP must contain only numbers"),
});

export type VerifyOtpInput = z.infer<typeof verifyOtpSchema>;
