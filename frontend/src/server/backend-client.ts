import { getLocale } from "next-intl/server";
import { env } from "@/lib/env";
import { getSession } from "@/server/session";
import {
    createRefreshCoordinator,
    RefreshRejectedError,
    refreshSession,
} from "./refresh-coordinator";

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
    const locale = await getLocale();
    const fallback =
        locale === "vi" ? "Không thể kết nối với máy chủ" : "Unable to communicate with the server";
    const url = `${env.SPRING_BOOT_URL}${endpoint.startsWith("/") ? endpoint : `/${endpoint}`}`;
    const headers = new Headers(options.headers || {});
    headers.set("Content-Type", "application/json");

    const session = requireAuth ? await getSession() : null;

    if (requireAuth && session?.accessToken) {
        headers.set("Authorization", `Bearer ${session.accessToken}`);
    }

    let response = await fetch(url, {
        ...options,
        headers,
    });

    // Handle Token Expiration (HTTP 401) with transparent refresh
    if (response.status === 401 && requireAuth && session?.refreshToken) {
        const accessToken = await refreshAccessToken(session);
        headers.set("Authorization", `Bearer ${accessToken}`);
        response = await fetch(url, { ...options, headers });
    }

    const data: ApiResponse<T> = await response.json().catch(() => ({
        success: false,
        statusCode: response.status,
        message: fallback,
        data: null as unknown as T,
        timestamp: new Date().toISOString(),
    }));

    if (!response.ok || !data.success) {
        throw new BackendError(data.message || fallback, response.status);
    }

    return data;
}

const refreshTokens = createRefreshCoordinator(async (refreshToken) => {
    const response = await fetch(`${env.SPRING_BOOT_URL}/auth/refresh-token`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Cookie: `refreshToken=${refreshToken}`,
        },
        cache: "no-store",
        signal: AbortSignal.timeout(10_000),
    });

    if (response.status === 401 || response.status === 403) throw new RefreshRejectedError();
    if (!response.ok)
        throw new BackendError("Unable to refresh your session. Please retry.", response.status);

    const data: ApiResponse<{ accessToken: string }> = await response.json();
    const cookies = response.headers.getSetCookie();
    const rotated = cookies
        .map((cookie) => cookie.match(/^refreshToken=([^;]+)/)?.[1])
        .find(Boolean);

    if (!data.success || !data.data?.accessToken || !rotated) {
        throw new BackendError("Invalid response from the authentication server.", 502);
    }

    return { accessToken: data.data.accessToken, refreshToken: rotated };
});

// Call only from a client-invoked Server Action or Route Handler, never during SSR.
async function refreshAccessToken(
    session: Awaited<ReturnType<typeof getSession>>
): Promise<string> {
    try {
        return await refreshSession(session, refreshTokens);
    } catch (error) {
        if (error instanceof RefreshRejectedError) throw new BackendError(error.message, 401);
        throw error;
    }
}

export async function getFreshAccessToken(): Promise<string | null> {
    const session = await getSession();
    if (!session.isLoggedIn || !session.accessToken) return null;

    try {
        const payload = JSON.parse(
            Buffer.from(session.accessToken.split(".")[1], "base64url").toString()
        );
        if (typeof payload.exp === "number" && payload.exp * 1000 > Date.now() + 30_000) {
            return session.accessToken;
        }
    } catch {
        // Let the authentication server validate the refresh token.
    }

    return refreshAccessToken(session);
}
