import { getIronSession, SessionOptions } from "iron-session";
import { cookies } from "next/headers";
import { env } from "@/lib/env";

export type UserStatus = "ACTIVE" | "INACTIVE" | "LOCKED";

export interface SessionUser {
    id: string;
    email: string;
    fullName: string;
    phoneNumber?: string;
    status: UserStatus;
}

export interface SessionData {
    accessToken?: string;
    refreshToken?: string;
    user?: SessionUser;
    isLoggedIn: boolean;
}

export const sessionOptions: SessionOptions = {
    password: env.SESSION_SECRET,
    cookieName: "wallet_session",
    cookieOptions: {
        secure: env.NODE_ENV === "production",
        httpOnly: true,
        sameSite: "strict",
        path: "/",
        maxAge: 7 * 24 * 60 * 60, // 7 days in seconds
    },
};

export async function getSession() {
    const cookieStore = await cookies();
    return getIronSession<SessionData>(cookieStore, sessionOptions);
}

export async function destroySession() {
    const session = await getSession();
    session.destroy();
}
