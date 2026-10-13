import { z } from "zod";
import defaultMessages from "../../../../messages/en/authValidation.json";

type ValidationKey = keyof typeof defaultMessages.authValidation;
export type AuthValidationTranslator = (key: ValidationKey) => string;

const defaultTranslator: AuthValidationTranslator = (key) => defaultMessages.authValidation[key];

export const createLoginSchema = (t: AuthValidationTranslator) =>
    z.object({
        email: z.string({ error: t("invalidType") }).email(t("email")),
        password: z.string({ error: t("invalidType") }).min(6, t("loginPassword")),
    });

export const loginSchema = createLoginSchema(defaultTranslator);

export type LoginInput = z.infer<typeof loginSchema>;

export const createRegisterSchema = (t: AuthValidationTranslator) =>
    z
        .object({
            fullName: z
                .string({ error: t("invalidType") })
                .min(2, t("fullNameMin"))
                .max(100, t("fullNameMax")),
            email: z.string({ error: t("invalidType") }).email(t("email")),
            phoneNumber: z
                .string({ error: t("invalidType") })
                .regex(/^(0|\+84)[3|5|7|8|9][0-9]{8}$/, t("phone")),
            dateOfBirth: z.string({ error: t("invalidType") }).refine(
                (dob) => {
                    const birth = new Date(dob);
                    const now = new Date();
                    const age = now.getFullYear() - birth.getFullYear();
                    return age >= 18;
                },
                { message: t("age") }
            ),
            password: z
                .string({ error: t("invalidType") })
                .min(8, t("passwordMin"))
                .regex(/[A-Z]/, t("passwordUppercase"))
                .regex(/[0-9]/, t("passwordNumber")),
            confirmPassword: z.string({ error: t("invalidType") }).min(1, t("confirmPassword")),
        })
        .refine((data) => data.password === data.confirmPassword, {
            message: t("passwordMismatch"),
            path: ["confirmPassword"],
        });

export const registerSchema = createRegisterSchema(defaultTranslator);

export type RegisterInput = z.infer<typeof registerSchema>;

export const createVerifyOtpSchema = (t: AuthValidationTranslator) =>
    z.object({
        email: z.string({ error: t("invalidType") }).email(t("email")),
        otp: z
            .string({ error: t("invalidType") })
            .length(6, t("otpLength"))
            .regex(/^[0-9]+$/, t("otpNumber")),
    });

export const verifyOtpSchema = createVerifyOtpSchema(defaultTranslator);

export type VerifyOtpInput = z.infer<typeof verifyOtpSchema>;
