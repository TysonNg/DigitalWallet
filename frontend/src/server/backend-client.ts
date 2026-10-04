import { env } from "@/lib/env";
import { getSession } from "@/server/session";

export interface ApiResponse<T> {
    success: boolean;
    statusCode: number;
    message: string;
    data: T;
    timestamp: string;
}

export class BackendError extends Error {
    statusCode: number;
    constructor(message: string, statusCode: number = 500) {
        super(message);
        this.name = "BackendError";
        this.statusCode = statusCode;
    }
}

/**
 * Client for making HTTP requests directly from Next.js server (BFF) to Spring Boot Backend.
 * Handles automatic JWT attachment, response parsing, and transparent token refresh.
 */
export async function backendFetch<T>(
    endpoint: string,
    options: RequestInit = {},
    requireAuth: boolean = true
): Promise<ApiResponse<T>> {
    const url = `${env.SPRING_BOOT_URL}${endpoint.startsWith("/") ? endpoint : `/${endpoint}`}`;
    const headers = new Headers(options.headers || {});
    headers.set("Content-Type", "application/json");

    let session = requireAuth ? await getSession() : null;

    if (requireAuth && session?.accessToken) {
        headers.set("Authorization", `Bearer ${session.accessToken}`);
    }

    let response = await fetch(url, {
        ...options,
        headers,
    });

    // Handle Token Expiration (HTTP 401) with transparent refresh
    if (response.status === 401 && requireAuth && session?.refreshToken) {
        const refreshed = await refreshAccessToken();
        if (refreshed) {
            session = await getSession();
            if (session.accessToken) {
                headers.set("Authorization", `Bearer ${session.accessToken}`);
                response = await fetch(url, {
                    ...options,
                    headers,
                });
            }
        }
    }

    const data: ApiResponse<T> = await response.json().catch(() => ({
        success: false,
        statusCode: response.status,
        message: response.statusText || "Server communication failed",
        data: null as unknown as T,
        timestamp: new Date().toISOString(),
    }));

    if (!response.ok || !data.success) {
        throw new BackendError(data.message || "An error occurred", response.status);
    }

    return data;
}

/**
 * Transparently refresh access token using the stored refresh token cookie.
 */
async function refreshAccessToken(): Promise<boolean> {
    try {
        const session = await getSession();
        if (!session.refreshToken) return false;

        const response = await fetch(`${env.SPRING_BOOT_URL}/auth/refresh-token`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Cookie: `refreshToken=${session.refreshToken}`,
            },
        });

        if (!response.ok) {
            session.destroy();
            return false;
        }

        const data: ApiResponse<{ accessToken: string }> = await response.json();
        const setCookieHeader = response.headers.get("set-cookie");
        let newRefreshToken = session.refreshToken;

        if (setCookieHeader) {
            const match = setCookieHeader.match(/refreshToken=([^;]+)/);
            if (match) {
                newRefreshToken = match[1];
            }
        }

        session.accessToken = data.data.accessToken;
        session.refreshToken = newRefreshToken;
        await session.save();
        return true;
    } catch {
        return false;
    }
}
