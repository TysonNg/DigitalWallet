export const queryKeys = {
    wallet: {
        all: ["wallet"] as const,
        me: ["wallet", "me"] as const,
        transactions: (walletId?: string) => ["wallet", "transactions", walletId ?? "all"] as const,
        lookup: (identifier: string) => ["wallet", "lookup", identifier] as const,
    },
    beneficiaries: {
        all: ["beneficiaries"] as const,
    },
    notifications: {
        all: ["notifications"] as const,
    },
} as const;
