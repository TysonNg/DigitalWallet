import test from "node:test";
import assert from "node:assert/strict";
import type { PaymentGatewayType } from "../src/features/wallet/types/wallet.types";
import {
    createRefreshCoordinator,
    refreshSession,
} from "../src/server/refresh-coordinator";

interface MockSession {
    isLoggedIn: boolean;
    user?: { id: string; email: string };
    accessToken?: string;
    refreshToken?: string;
    save: () => Promise<void>;
    destroy: () => void;
}

interface PaymentOrderRequest {
    walletId?: string;
    userId: string;
    amount: number;
    gateway: PaymentGatewayType;
}

/**
 * Simulates BFF payment order creator with transparent token renewal
 */
async function createPaymentOrderFlow(
    params: { walletId?: string; amount: number; gateway: PaymentGatewayType },
    session: MockSession,
    fetchMock: (url: string, init?: RequestInit) => Promise<Response>,
    refreshTokens: ReturnType<typeof createRefreshCoordinator>
) {
    if (!session.isLoggedIn || !session.user) {
        return { success: false, message: "Your session has expired" };
    }

    if (params.amount < 1000) {
        return { success: false, message: "The minimum deposit is VND 1,000" };
    }

    let walletId = params.walletId;
    if (!walletId || walletId.trim() === "") {
        const walletRes = await fetchMock(
            `http://localhost:8080/api/v1/wallet/user/${session.user.id}`,
            {
                headers: { Authorization: `Bearer ${session.accessToken}` },
            }
        );
        const walletData = await walletRes.json();
        if (walletData.data?.id) {
            walletId = walletData.data.id;
        }
    }

    let headers: Record<string, string> = {
        "Content-Type": "application/json",
        Authorization: `Bearer ${session.accessToken}`,
    };

    let res = await fetchMock("http://localhost:8080/api/v1/payments/create-order", {
        method: "POST",
        headers,
        body: JSON.stringify({
            walletId,
            userId: session.user.id,
            amount: params.amount,
            gateway: params.gateway,
        }),
    });

    // 401 transparent token refresh
    if (res.status === 401 && session.refreshToken) {
        const newAccessToken = await refreshSession(session, refreshTokens);
        headers = {
            "Content-Type": "application/json",
            Authorization: `Bearer ${newAccessToken}`,
        };
        res = await fetchMock("http://localhost:8080/api/v1/payments/create-order", {
            method: "POST",
            headers,
            body: JSON.stringify({
                walletId,
                userId: session.user.id,
                amount: params.amount,
                gateway: params.gateway,
            }),
        });
    }

    const data = await res.json();
    return data;
}

test("PayOS payment order creates pending order and returns VietQR checkout URL", async () => {
    const session: MockSession = {
        isLoggedIn: true,
        user: { id: "user-123", email: "user@test.com" },
        accessToken: "valid-jwt",
        refreshToken: "valid-refresh",
        save: async () => {},
        destroy: () => {},
    };

    const fetchMock = async (url: string, init?: RequestInit) => {
        assert.equal(url, "http://localhost:8080/api/v1/payments/create-order");
        assert.equal(init?.method, "POST");
        const body: PaymentOrderRequest = JSON.parse(init?.body as string);
        assert.equal(body.walletId, "wallet-123");
        assert.equal(body.amount, 500000);
        assert.equal(body.gateway, "PAYOS");

        return new Response(
            JSON.stringify({
                success: true,
                message: "Tạo đơn nạp tiền thành công",
                data: {
                    id: "order-abc",
                    walletId: "wallet-123",
                    userId: "user-123",
                    amount: 500000,
                    gateway: "PAYOS",
                    gatewayOrderId: "payos-999",
                    status: "PENDING",
                    checkoutUrl: "https://pay.payos.vn/web/12345",
                },
            }),
            { status: 200, headers: { "Content-Type": "application/json" } }
        );
    };

    const coordinator = createRefreshCoordinator(async () => ({
        accessToken: "refreshed-jwt",
        refreshToken: "refreshed-rt",
    }));

    const result = await createPaymentOrderFlow(
        { walletId: "wallet-123", amount: 500000, gateway: "PAYOS" },
        session,
        fetchMock,
        coordinator
    );

    assert.equal(result.success, true);
    assert.equal(result.data.gateway, "PAYOS");
    assert.equal(result.data.status, "PENDING");
    assert.equal(result.data.checkoutUrl, "https://pay.payos.vn/web/12345");
});

test("Stripe payment order creates pending order and returns Stripe checkout session URL", async () => {
    const session: MockSession = {
        isLoggedIn: true,
        user: { id: "user-456", email: "stripe@test.com" },
        accessToken: "valid-jwt",
        refreshToken: "valid-refresh",
        save: async () => {},
        destroy: () => {},
    };

    const fetchMock = async (url: string, init?: RequestInit) => {
        const body: PaymentOrderRequest = JSON.parse(init?.body as string);
        assert.equal(body.gateway, "STRIPE");
        assert.equal(body.amount, 1000000);

        return new Response(
            JSON.stringify({
                success: true,
                data: {
                    id: "order-stripe-1",
                    walletId: "wallet-456",
                    userId: "user-456",
                    amount: 1000000,
                    gateway: "STRIPE",
                    gatewayOrderId: "cs_test_session_123",
                    status: "PENDING",
                    checkoutUrl: "https://checkout.stripe.com/c/pay/cs_test_session_123",
                },
            }),
            { status: 200, headers: { "Content-Type": "application/json" } }
        );
    };

    const coordinator = createRefreshCoordinator(async () => ({
        accessToken: "refreshed-jwt",
        refreshToken: "refreshed-rt",
    }));

    const result = await createPaymentOrderFlow(
        { walletId: "wallet-456", amount: 1000000, gateway: "STRIPE" },
        session,
        fetchMock,
        coordinator
    );

    assert.equal(result.success, true);
    assert.equal(result.data.gateway, "STRIPE");
    assert.equal(result.data.gatewayOrderId, "cs_test_session_123");
    assert.match(result.data.checkoutUrl, /checkout\.stripe\.com/);
});

test("resolves wallet automatically if walletId is omitted in payment order", async () => {
    const session: MockSession = {
        isLoggedIn: true,
        user: { id: "user-789", email: "fallback@test.com" },
        accessToken: "valid-jwt",
        refreshToken: "valid-refresh",
        save: async () => {},
        destroy: () => {},
    };

    let walletFetchCalled = false;
    const fetchMock = async (url: string, init?: RequestInit) => {
        if (url.includes("/wallet/user/")) {
            walletFetchCalled = true;
            return new Response(
                JSON.stringify({
                    success: true,
                    data: { id: "resolved-wallet-id", balance: 50000, currency: "VND" },
                }),
                { status: 200, headers: { "Content-Type": "application/json" } }
            );
        }

        const body: PaymentOrderRequest = JSON.parse(init?.body as string);
        assert.equal(body.walletId, "resolved-wallet-id");
        return new Response(
            JSON.stringify({
                success: true,
                data: {
                    id: "order-resolved",
                    walletId: "resolved-wallet-id",
                    checkoutUrl: "https://pay.payos.vn/web/resolved",
                },
            }),
            { status: 200, headers: { "Content-Type": "application/json" } }
        );
    };

    const coordinator = createRefreshCoordinator(async () => ({
        accessToken: "refreshed-jwt",
        refreshToken: "refreshed-rt",
    }));

    const result = await createPaymentOrderFlow(
        { amount: 200000, gateway: "PAYOS" },
        session,
        fetchMock,
        coordinator
    );

    assert.equal(walletFetchCalled, true);
    assert.equal(result.success, true);
    assert.equal(result.data.walletId, "resolved-wallet-id");
});

test("deposit amount below 1000 VND is rejected without calling backend", async () => {
    const session: MockSession = {
        isLoggedIn: true,
        user: { id: "user-1", email: "test@test.com" },
        save: async () => {},
        destroy: () => {},
    };

    let called = false;
    const fetchMock = async () => {
        called = true;
        return new Response();
    };

    const coordinator = createRefreshCoordinator(async () => ({
        accessToken: "t",
        refreshToken: "rt",
    }));

    const result = await createPaymentOrderFlow(
        { walletId: "w1", amount: 500, gateway: "PAYOS" },
        session,
        fetchMock,
        coordinator
    );

    assert.equal(called, false);
    assert.equal(result.success, false);
    assert.match(result.message, /minimum deposit/i);
});

test("unauthenticated session is rejected immediately", async () => {
    const session: MockSession = {
        isLoggedIn: false,
        save: async () => {},
        destroy: () => {},
    };

    const coordinator = createRefreshCoordinator(async () => ({
        accessToken: "t",
        refreshToken: "rt",
    }));

    const result = await createPaymentOrderFlow(
        { walletId: "w1", amount: 50000, gateway: "PAYOS" },
        session,
        async () => new Response(),
        coordinator
    );

    assert.equal(result.success, false);
    assert.match(result.message, /expired/i);
});

test("transparently refreshes expired token during payment order creation and retries successfully", async () => {
    let refreshed = false;
    const session: MockSession = {
        isLoggedIn: true,
        user: { id: "user-1", email: "test@test.com" },
        accessToken: "expired-token",
        refreshToken: "valid-refresh-token",
        save: async () => {},
        destroy: () => {},
    };

    let attempts = 0;
    const fetchMock = async (url: string, init?: RequestInit) => {
        attempts++;
        const auth = (init?.headers as Record<string, string>)?.["Authorization"];

        if (attempts === 1) {
            assert.equal(auth, "Bearer expired-token");
            return new Response(
                JSON.stringify({ success: false, message: "Token expired" }),
                { status: 401, headers: { "Content-Type": "application/json" } }
            );
        }

        assert.equal(auth, "Bearer freshly-rotated-jwt");
        return new Response(
            JSON.stringify({
                success: true,
                data: {
                    id: "order-after-refresh",
                    checkoutUrl: "https://pay.payos.vn/web/refreshed",
                },
            }),
            { status: 200, headers: { "Content-Type": "application/json" } }
        );
    };

    const coordinator = createRefreshCoordinator(async () => {
        refreshed = true;
        return {
            accessToken: "freshly-rotated-jwt",
            refreshToken: "new-refresh-token",
        };
    });

    const result = await createPaymentOrderFlow(
        { walletId: "w1", amount: 100000, gateway: "PAYOS" },
        session,
        fetchMock,
        coordinator
    );

    assert.equal(refreshed, true);
    assert.equal(attempts, 2);
    assert.equal(result.success, true);
    assert.equal(result.data.checkoutUrl, "https://pay.payos.vn/web/refreshed");
});

test("verifies payment feedback query params handling on dashboard return", () => {
    const successUrl = new URL("https://wallet.vn/vi/dashboard?payment=success");
    const cancelledUrl = new URL("https://wallet.vn/vi/dashboard?payment=cancelled");
    const normalUrl = new URL("https://wallet.vn/vi/dashboard");

    assert.equal(successUrl.searchParams.get("payment"), "success");
    assert.equal(cancelledUrl.searchParams.get("payment"), "cancelled");
    assert.equal(normalUrl.searchParams.get("payment"), null);
});
