import { getSession } from "@/server/session";
import { redirect } from "@/i18n/navigation";
import { getLocale } from "next-intl/server";
import { dehydrate, HydrationBoundary } from "@tanstack/react-query";
import { getQueryClient } from "@/lib/get-query-client";
import { queryKeys } from "@/lib/query-keys";
import { DashboardView } from "@/features/dashboard/dashboard-view";
import {
    getMyWalletAction,
    getWalletTransactionsAction,
} from "@/features/wallet/actions/wallet.actions";
import { getNotificationsAction } from "@/features/dashboard/actions/notification.actions";
import { WalletDto } from "@/features/wallet/types/wallet.types";

export default async function DashboardPage() {
    const session = await getSession();

    if (!session.isLoggedIn || !session.user) {
        redirect({
            href: {
                pathname: "/",
                query: { auth: "login", returnTo: "/" + (await getLocale()) + "/dashboard" },
            },
            locale: await getLocale(),
        });
    }

    const user = session.user!;
    const queryClient = getQueryClient();

    // 1. Prefetch Wallet
    await queryClient.prefetchQuery({
        queryKey: queryKeys.wallet.me,
        queryFn: async () => {
            const res = await getMyWalletAction();
            return res.data ?? null;
        },
    });

    const wallet = queryClient.getQueryData<WalletDto | null>(queryKeys.wallet.me);

    // 2. Prefetch Transactions
    if (wallet?.id) {
        await queryClient.prefetchQuery({
            queryKey: queryKeys.wallet.transactions(wallet.id),
            queryFn: async () => {
                const res = await getWalletTransactionsAction(wallet.id);
                return res.data ?? [];
            },
        });
    }

    // 3. Prefetch Notifications
    await queryClient.prefetchQuery({
        queryKey: queryKeys.notifications.all,
        queryFn: async () => {
            const res = await getNotificationsAction();
            return res.data ?? [];
        },
    });

    return (
        <HydrationBoundary state={dehydrate(queryClient)}>
            <DashboardView user={user} initialWallet={wallet} />
        </HydrationBoundary>
    );
}
