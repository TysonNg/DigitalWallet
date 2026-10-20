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
        user: (userId: string) => ["notifications", userId] as const,
    },
    admin: {
        stats: ["admin", "stats"] as const,
        users: ["admin", "users"] as const,
        trialBalance: ["admin", "ledger", "trial-balance"] as const,
    },
} as const;

