import type { TransactionDto } from "../wallet/types/wallet.types";

const zone = "Asia/Ho_Chi_Minh";

// Backend LocalDateTime values have no offset; use the wallet's Vietnam calendar.
export function transactionDate(value: string): Date {
    return new Date(/(?:Z|[+-]\d{2}:\d{2})$/i.test(value) ? value : `${value}+07:00`);
}

function dayKey(date: Date): string {
    return new Intl.DateTimeFormat("en-CA", {
        timeZone: zone,
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
    }).format(date);
}

export function transactionDirection(tx: TransactionDto, walletId: string): number {
    if (tx.senderWalletId === walletId && tx.receiverWalletId === walletId) return 0;
    if (tx.receiverWalletId === walletId) return 1;
    if (tx.senderWalletId === walletId) return -1;
    if (tx.senderWalletId || tx.receiverWalletId) return 0;
    if (tx.type === "DEPOSIT" || tx.type === "TRANSFER_IN") return 1;
    return -1;
}

export function activityData(
    transactions: TransactionDto[],
    walletId: string,
    days: number,
    now = new Date()
) {
    const today = dayKey(now);
    const end = new Date(`${today}T00:00:00+07:00`).getTime();
    const rows = Array.from({ length: days }, (_, index) => ({
        date: dayKey(new Date(end - (days - 1 - index) * 86400000)),
        inAmount: 0,
        outAmount: 0,
    }));
    for (const tx of transactions) {
        const date = transactionDate(tx.createdAt);
        if (tx.status !== "SUCCESS" || !Number.isFinite(date.getTime()) || date > now) continue;
        const row = rows.find((item) => item.date === dayKey(date));
        const direction = transactionDirection(tx, walletId);
        if (row && direction) row[direction > 0 ? "inAmount" : "outAmount"] += Math.abs(tx.amount);
    }
    return rows;
}

export function monthlySpending(
    transactions: TransactionDto[],
    walletId: string,
    now = new Date()
) {
    const month = dayKey(now).slice(0, 7);
    const previousMonth = dayKey(
        new Date(new Date(`${month}-01T00:00:00+07:00`).getTime() - 86400000)
    ).slice(0, 7);
    let current = 0;
    let previous = 0;
    for (const tx of transactions) {
        const date = transactionDate(tx.createdAt);
        if (
            tx.status !== "SUCCESS" ||
            transactionDirection(tx, walletId) >= 0 ||
            !Number.isFinite(date.getTime()) ||
            date > now
        )
            continue;
        const key = dayKey(date).slice(0, 7);
        if (key === month) current += Math.abs(tx.amount);
        if (key === previousMonth) previous += Math.abs(tx.amount);
    }
    return {
        current,
        previous,
        changePercent: previous ? ((current - previous) / previous) * 100 : null,
    };
}
