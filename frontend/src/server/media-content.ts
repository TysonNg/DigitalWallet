import { isReceiveUuid } from "../features/wallet/receive-qr";

/** Isolated binary BFF policy; only the route supplies the configured backend and session. */
export async function proxyMediaContent(
    id: string,
    backendUrl: string,
    getToken: () => Promise<string | null>,
    fetchContent: typeof fetch = fetch
): Promise<Response> {
    const headers = { "Cache-Control": "no-store", "X-Content-Type-Options": "nosniff" };
    const failure = (status: number) => new Response(null, { status, headers });

    if (!isReceiveUuid(id)) return failure(400);

    try {
        const token = await getToken();
        if (!token) return failure(401);

        const upstream = await fetchContent(`${backendUrl}/media/${id}/content`, {
            headers: { Authorization: `Bearer ${token}`, Accept: "image/png" },
            cache: "no-store",
            redirect: "error",
            signal: AbortSignal.timeout(15_000),
        });

        if (!upstream.ok) {
            await upstream.body?.cancel();
            return failure([401, 403, 404].includes(upstream.status) ? upstream.status : 502);
        }

        if (
            upstream.headers.get("Content-Type")?.split(";")[0].trim().toLowerCase() !== "image/png"
        ) {
            await upstream.body?.cancel();
            return failure(502);
        }

        return new Response(await upstream.arrayBuffer(), {
            headers: {
                ...headers,
                "Content-Type": "image/png",
                "Content-Disposition": `inline; filename="receive-qr-${id}.png"`,
            },
        });
    } catch (error) {
        return failure(
            error && typeof error === "object" && "statusCode" in error && error.statusCode === 401
                ? 401
                : 502
        );
    }
}
