import test from "node:test";
import assert from "node:assert/strict";
import {
    transactionDirection,
    activityData,
    monthlySpending,
} from "../src/features/dashboard/dashboard-data";
import type { TransactionDto } from "../src/features/wallet/types/wallet.types";
const tx = (values: Partial<TransactionDto>): TransactionDto => ({
    id: "t",
    walletId: "mine",
    type: "DEPOSIT",
    amount: 100,
    status: "SUCCESS",
    createdAt: "2026-10-15T12:00:00+07:00",
    ...values,
});
test("shared transfer is income for receiver, expense for sender", () => {
    const transfer = tx({
        type: "TRANSFER_OUT",
        senderWalletId: "other",
        receiverWalletId: "mine",
    });
    assert.equal(transactionDirection(transfer, "mine"), 1);
    assert.equal(transactionDirection(transfer, "other"), -1);
    assert.equal(transactionDirection(transfer, "unrelated"), 0);
});
test("activity includes successful transactions on Vietnam calendar days only", () => {
    const data = activityData(
        [
            tx({ createdAt: "2026-10-14T18:00:00Z" }),
            tx({ type: "WITHDRAW", amount: 30 }),
            tx({ status: "PENDING", amount: 999 }),
            tx({ createdAt: "2026-10-16T12:00:00+07:00", amount: 999 }),
        ],
        "mine",
        7,
        new Date("2026-10-15T10:00:00Z")
    );
    assert.equal(data.length, 7);
    assert.deepEqual(data[6], { date: "2026-10-15", inAmount: 100, outAmount: 30 });
    assert.equal(data[5].inAmount, 0);
});
test("monthly spending handles year boundary and ignores failed and incoming transfers", () => {
    assert.deepEqual(
        monthlySpending(
            [
                tx({ type: "WITHDRAW", createdAt: "2026-01-01T00:00:00+07:00", amount: 70 }),
                tx({
                    type: "TRANSFER_OUT",
                    senderWalletId: "other",
                    receiverWalletId: "mine",
                    createdAt: "2026-01-02T00:00:00+07:00",
                    amount: 500,
                }),
                tx({ type: "WITHDRAW", createdAt: "2025-12-31T00:00:00+07:00", amount: 100 }),
                tx({
                    type: "WITHDRAW",
                    status: "FAILED",
                    createdAt: "2026-01-02T00:00:00+07:00",
                    amount: 500,
                }),
            ],
            "mine",
            new Date("2026-01-15T00:00:00Z")
        ),
        { current: 70, previous: 100, changePercent: -30 }
    );
    assert.deepEqual(monthlySpending([], "mine"), { current: 0, previous: 0, changePercent: null });
});
