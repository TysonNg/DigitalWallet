import { env } from "@/lib/env";
import { getFreshAccessToken } from "@/server/backend-client";
import { proxyMediaContent } from "@/server/media-content";

export const dynamic = "force-dynamic";

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
    const { id } = await params;

    return proxyMediaContent(id, env.SPRING_BOOT_URL, getFreshAccessToken);
}
