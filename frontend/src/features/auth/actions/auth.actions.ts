"use server";

import { getTranslations } from "next-intl/server";
import { env } from "@/lib/env";
import { getSession } from "@/server/session";
import {
    LoginInput,
    createLoginSchema,
    RegisterInput,
    createRegisterSchema,
    VerifyOtpInput,
    createVerifyOtpSchema,
} from "../schemas/auth.schema";
import { ActionResponse, AuthResponseData, User } from "../types/auth.types";

/**
 * BFF Server Action: Đăng nhập
 * Gọi Spring Boot -> Lấy Tokens -> Mã hóa lưu vào HttpOnly Cookie
 */
export async function loginAction(input: LoginInput): Promise<ActionResponse<User>> {
    const [t, validation] = await Promise.all([
        getTranslations("authErrors"),
        getTranslations("authValidation"),
    ]);
    const parsed = createLoginSchema(validation).safeParse(input);
    if (!parsed.success) {
        return {
            success: false,
            message: t("invalidInput"),
            errors: parsed.error.flatten().fieldErrors,
        };
    }

    try {
        const response = await fetch(`${env.SPRING_BOOT_URL}/auth/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(parsed.data),
        });

        const resData = await response.json();

        if (!response.ok || !resData.success) {
            return {
                success: false,
                message: resData.message || t("invalidCredentials"),
            };
        }

        const authData: AuthResponseData = resData.data;

        // Lấy refreshToken từ Set-Cookie do Spring Boot trả về
        const setCookie = response.headers.get("set-cookie");
        let refreshToken: string | undefined = undefined;
        if (setCookie) {
            const match = setCookie.match(/refreshToken=([^;]+)/);
            if (match) {
                refreshToken = match[1];
            }
        }

        // BFF MÃ HÓA toàn bộ Token vào session cookie
        const session = await getSession();
        session.accessToken = authData.accessToken;
        session.refreshToken = refreshToken;
        session.user = {
            id: authData.user.id,
            email: authData.user.email,
            fullName: authData.user.fullName,
            phoneNumber: authData.user.phoneNumber,
            status: authData.user.status,
        };
        session.isLoggedIn = true;
        await session.save();

        return {
            success: true,
            message: t("loginSuccess"),
            data: session.user,
        };
    } catch (error) {
        const isFetchError = error instanceof Error && error.message.includes("fetch failed");
        return {
            success: false,
            message: isFetchError ? t("backendConnection") : t("serverConnection"),
        };
    }
}

/**
 * BFF Server Action: Đăng ký bước 1 (Gửi mã OTP)
 */
export async function registerAction(input: RegisterInput): Promise<ActionResponse<null>> {
    const [t, validation] = await Promise.all([
        getTranslations("authErrors"),
        getTranslations("authValidation"),
    ]);
    const parsed = createRegisterSchema(validation).safeParse(input);
    if (!parsed.success) {
        return {
            success: false,
            message: t("invalidInput"),
            errors: parsed.error.flatten().fieldErrors,
        };
    }

    try {
        const payload = {
            fullName: parsed.data.fullName,
            email: parsed.data.email,
            phoneNumber: parsed.data.phoneNumber,
            dob: parsed.data.dateOfBirth,
            password: parsed.data.password,
            confirmPassword: parsed.data.confirmPassword,
        };

        const response = await fetch(`${env.SPRING_BOOT_URL}/auth/register`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });

        const resData = await response.json();

        if (!response.ok || !resData.success) {
            return {
                success: false,
                message: resData.message || t("registerFailed"),
            };
        }

        return {
            success: true,
            message: t("otpSent"),
        };
    } catch (error) {
        const isFetchError = error instanceof Error && error.message.includes("fetch failed");
        return {
            success: false,
            message: isFetchError ? t("backendConnection") : t("serverConnection"),
        };
    }
}

/**
 * BFF Server Action: Xác thực OTP và kích hoạt tài khoản
 */
export async function verifyOtpAction(input: VerifyOtpInput): Promise<ActionResponse<User>> {
    const [t, validation] = await Promise.all([
        getTranslations("authErrors"),
        getTranslations("authValidation"),
    ]);
    const parsed = createVerifyOtpSchema(validation).safeParse(input);
    if (!parsed.success) {
        return {
            success: false,
            message: t("invalidOtpFormat"),
            errors: parsed.error.flatten().fieldErrors,
        };
    }

    try {
        const response = await fetch(`${env.SPRING_BOOT_URL}/auth/register/verify`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(parsed.data),
        });

        const resData = await response.json();

        if (!response.ok || !resData.success) {
            return {
                success: false,
                message: resData.message || t("invalidOtp"),
            };
        }

        const authData: AuthResponseData = resData.data;

        const setCookie = response.headers.get("set-cookie");
        let refreshToken: string | undefined = undefined;
        if (setCookie) {
            const match = setCookie.match(/refreshToken=([^;]+)/);
            if (match) {
                refreshToken = match[1];
            }
        }

        // Tự động đăng nhập người dùng sau khi verify OTP thành công
        const session = await getSession();
        session.accessToken = authData.accessToken;
        session.refreshToken = refreshToken;
        session.user = {
            id: authData.user.id,
            email: authData.user.email,
            fullName: authData.user.fullName,
            phoneNumber: authData.user.phoneNumber,
            status: authData.user.status,
        };
        session.isLoggedIn = true;
        await session.save();

        return {
            success: true,
            message: t("activationSuccess"),
            data: session.user,
        };
    } catch (error) {
        const isFetchError = error instanceof Error && error.message.includes("fetch failed");
        return {
            success: false,
            message: isFetchError ? t("backendConnection") : t("serverConnection"),
        };
    }
}

/**
 * BFF Server Action: Đăng xuất (Blacklist Token + Xóa Cookie)
 */
export async function logoutAction(): Promise<ActionResponse<null>> {
    const t = await getTranslations("authErrors");
    try {
        const session = await getSession();

        if (session.accessToken) {
            // Gọi sang Spring Boot để đưa Access Token vào Redis Blacklist
            await fetch(`${env.SPRING_BOOT_URL}/auth/logout`, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${session.accessToken}`,
                },
            }).catch(() => {
                // Vẫn tiếp tục hủy session cục bộ kể cả khi backend lỗi
            });
        }

        session.destroy();

        return {
            success: true,
            message: t("logoutSuccess"),
        };
    } catch {
        return {
            success: false,
            message: t("logoutFailed"),
        };
    }
}

/**
 * Lấy thông tin phiên đăng nhập hiện tại từ BFF Session
 */
export async function getCurrentUserAction(): Promise<User | null> {
    const session = await getSession();
    if (!session.isLoggedIn || !session.user) {
        return null;
    }
    return session.user;
}

/**
 * Lấy Access Token từ BFF Session phục vụ kết nối WebSocket
 */
export async function getAccessTokenAction(): Promise<string | null> {
    const session = await getSession();
    if (!session.isLoggedIn || !session.accessToken) {
        return null;
    }
    return session.accessToken;
}
