export interface TokenPair {
    accessToken: string;
    refreshToken: string;
}

export class RefreshRejectedError extends Error {
    constructor() {
        super("Your session has expired. Please log in again.");
    }
}

// Share a rotation with concurrent requests and briefly with requests carrying the old cookie.
// This coordinator is process-local; multiple BFF replicas need a shared session store.
export function createRefreshCoordinator(
    exchange: (token: string) => Promise<TokenPair>,
    now: () => number = Date.now
) {
    const entries = new Map<string, { promise: Promise<TokenPair>; expires: number }>();

    return (token: string): Promise<TokenPair> => {
        for (const [key, entry] of entries) {
            if (entry.expires <= now()) entries.delete(key);
        }

        const existing = entries.get(token);
        if (existing) return existing.promise;

        const promise = Promise.resolve().then(() => exchange(token));
        const entry = { promise, expires: Infinity };
        entries.set(token, entry);

        promise.then(
            () => {
                entry.expires = now() + 30_000;
            },
            () => {
                if (entries.get(token) === entry) entries.delete(token);
            }
        );

        return promise;
    };
}

interface RefreshSession {
    accessToken?: string;
    refreshToken?: string;
    user?: {
        id: string;
        email: string;
        fullName: string;
        phoneNumber?: string;
        status: any;
        roles?: string[];
        authorities?: string[];
    };
    save(): Promise<void>;
    destroy(): void;
}

export async function refreshSession(
    session: RefreshSession,
    exchange: (token: string) => Promise<TokenPair>
): Promise<string> {
    try {
        if (!session.refreshToken) throw new RefreshRejectedError();
        const tokens = await exchange(session.refreshToken);
        session.accessToken = tokens.accessToken;
        session.refreshToken = tokens.refreshToken;

        try {
            const payloadBase64 = tokens.accessToken.split(".")[1];
            if (payloadBase64 && session.user) {
                const payload = JSON.parse(Buffer.from(payloadBase64, "base64").toString("utf-8"));
                if (Array.isArray(payload.roles)) session.user.roles = payload.roles;
                if (Array.isArray(payload.authorities)) session.user.authorities = payload.authorities;
            }
        } catch {
            // fallback
        }

        await session.save();
        return tokens.accessToken;

    } catch (error) {
        if (error instanceof RefreshRejectedError) session.destroy();
        throw error;
    }
}
