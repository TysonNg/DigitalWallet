import { getIronSession, SessionOptions } from "iron-session";
import { cookies } from "next/headers";
import { env } from "@/lib/env";

export type UserStatus = "ACTIVE" | "INACTIVE" | "LOCKED" | "SUSPENDED";


export interface SessionUser {
    id: string;
    email: string;
    fullName: string;
    phoneNumber?: string;
    status: UserStatus;
    roles?: string[];
    authorities?: string[];
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
        secure: process.env.COOKIE_SECURE === "true",
        httpOnly: true,
        sameSite: "lax",
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
