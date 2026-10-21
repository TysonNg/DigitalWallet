import { NextRequest } from "next/server";
import { env } from "@/lib/env";
import { getFreshAccessToken } from "@/server/backend-client";
import { getSession } from "@/server/session";

export const dynamic = "force-dynamic";

export async function POST(req: NextRequest) {
    try {
        const session = await getSession();
        if (!session.isLoggedIn || !session.user) {
            return new Response(JSON.stringify({ error: "Unauthorized" }), {
                status: 401,
                headers: { "Content-Type": "application/json" },
            });
        }

        const token = await getFreshAccessToken();
        if (!token) {
            return new Response(JSON.stringify({ error: "Session expired" }), {
                status: 401,
                headers: { "Content-Type": "application/json" },
            });
        }

        const body = await req.json();

        const backendRes = await fetch(`${env.SPRING_BOOT_URL}/ai/chat/stream`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
                Accept: "text/event-stream",
            },
            body: JSON.stringify(body),
            signal: req.signal,
        });

        if (!backendRes.ok) {
            const errText = await backendRes.text().catch(() => "");
            return new Response(errText || "Backend streaming failed", {
                status: backendRes.status,
            });
        }

        return new Response(backendRes.body, {
            headers: {
                "Content-Type": "text/event-stream; charset=utf-8",
                "Cache-Control": "no-cache, no-transform",
                Connection: "keep-alive",
            },
        });
    } catch (err: unknown) {
        const message = err instanceof Error ? err.message : "Internal streaming error";
        return new Response(JSON.stringify({ error: message }), {
            status: 500,
            headers: { "Content-Type": "application/json" },
        });
    }
}
