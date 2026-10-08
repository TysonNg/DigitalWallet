import { getSession } from "@/server/session";
import { redirect } from "next/navigation";
import { DashboardView } from "@/features/dashboard/dashboard-view";
import {
    getMyWalletAction,
    getWalletTransactionsAction,
} from "@/features/wallet/actions/wallet.actions";
import { WalletDto, TransactionDto } from "@/features/wallet/types/wallet.types";

export const metadata = {
    title: "Tổng quan ví | Digital Wallet",
    description: "Quản lý số dư, hoạt động chi tiêu và giao dịch ví điện tử",
};

export default async function DashboardPage() {
    const session = await getSession();

    if (!session.isLoggedIn || !session.user) {
        redirect("/?auth=login");
    }

    const user = session.user;

    // Fetch real wallet and transactions from backend BFF
    let wallet: WalletDto | null = null;
    let transactions: TransactionDto[] = [];

    const walletResult = await getMyWalletAction();
    if (walletResult.success && walletResult.data) {
        wallet = walletResult.data;
        const txResult = await getWalletTransactionsAction(wallet.id);
        if (txResult.success && txResult.data) {
            transactions = txResult.data;
        }
    }

    return (
        <DashboardView
            user={user}
            initialWallet={wallet}
            initialTransactions={transactions}
        />
    );
}
