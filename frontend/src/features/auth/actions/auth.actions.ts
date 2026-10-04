"use server";

import { env } from "@/lib/env";
import { getSession } from "@/server/session";
import {
    LoginInput,
    loginSchema,
    RegisterInput,
    registerSchema,
    VerifyOtpInput,
    verifyOtpSchema,
} from "../schemas/auth.schema";
import { ActionResponse, AuthResponseData, User } from "../types/auth.types";

/**
 * BFF Server Action: Đăng nhập
 * Gọi Spring Boot -> Lấy Tokens -> Mã hóa lưu vào HttpOnly Cookie
 */
export async function loginAction(input: LoginInput): Promise<ActionResponse<User>> {
    const parsed = loginSchema.safeParse(input);
    if (!parsed.success) {
        return {
            success: false,
            message: "Dữ liệu không hợp lệ",
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
                message: resData.message || "Email hoặc mật khẩu không chính xác",
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
            message: "Đăng nhập thành công!",
            data: session.user,
        };
    } catch (error) {
        return {
            success: false,
            message: error instanceof Error ? error.message : "Lỗi kết nối tới máy chủ",
        };
    }
}

/**
 * BFF Server Action: Đăng ký bước 1 (Gửi mã OTP)
 */
export async function registerAction(input: RegisterInput): Promise<ActionResponse<null>> {
    const parsed = registerSchema.safeParse(input);
    if (!parsed.success) {
        return {
            success: false,
            message: "Dữ liệu không hợp lệ",
            errors: parsed.error.flatten().fieldErrors,
        };
    }

    try {
        const response = await fetch(`${env.SPRING_BOOT_URL}/auth/register`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(parsed.data),
        });

        const resData = await response.json();

        if (!response.ok || !resData.success) {
            return {
                success: false,
                message: resData.message || "Đăng ký thất bại. Vui lòng thử lại.",
            };
        }

        return {
            success: true,
            message: "Mã OTP xác thực đã được gửi tới email của bạn. Vui lòng kiểm tra hộp thư!",
        };
    } catch (error) {
        return {
            success: false,
            message: error instanceof Error ? error.message : "Lỗi kết nối tới máy chủ",
        };
    }
}

/**
 * BFF Server Action: Xác thực OTP và kích hoạt tài khoản
 */
export async function verifyOtpAction(input: VerifyOtpInput): Promise<ActionResponse<User>> {
    const parsed = verifyOtpSchema.safeParse(input);
    if (!parsed.success) {
        return {
            success: false,
            message: "Mã OTP không đúng định dạng",
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
                message: resData.message || "Mã OTP không chính xác hoặc đã hết hạn",
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
            message: "Kích hoạt tài khoản thành công!",
            data: session.user,
        };
    } catch (error) {
        return {
            success: false,
            message: error instanceof Error ? error.message : "Lỗi kết nối tới máy chủ",
        };
    }
}

/**
 * BFF Server Action: Đăng xuất (Blacklist Token + Xóa Cookie)
 */
export async function logoutAction(): Promise<ActionResponse<null>> {
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
            message: "Đăng xuất thành công!",
        };
    } catch (error) {
        return {
            success: false,
            message: error instanceof Error ? error.message : "Lỗi khi đăng xuất",
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
