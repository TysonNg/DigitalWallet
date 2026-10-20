export interface AdminStats {
    totalUsers: number;
    activeUsers: number;
    suspendedUsers: number;
    totalWallets: number;
    totalWalletBalance: number;
    totalTransactions: number;
    totalTransactionVolume: number;
}

export interface AdminUser {
    id: string;
    fullName: string;
    email: string;
    phoneNumber?: string;
    dob?: string;
    status: "ACTIVE" | "SUSPENDED" | "CLOSED";
    roles: string[];
    authorities: string[];
    walletId?: string;
    walletStatus?: "ACTIVE" | "FROZEN" | "CLOSED";
    walletBalance: number;
    walletCurrency: string;
    createdAt: string;
}

export interface AssignRolesInput {
    roles: string[];
}

export interface TrialBalance {
    totalDebit: number;
    totalCredit: number;
    balanced: boolean;
    difference: number;
    totalEntries: number;
}

export interface WalletReconciliation {
    walletId: string;
    walletBalance: number;
    reconciled: boolean;
}

