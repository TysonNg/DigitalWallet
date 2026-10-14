import { getSession } from "@/server/session";
import { redirect } from "@/i18n/navigation";
import { getLocale } from "next-intl/server";
import { DashboardShell } from "@/features/dashboard/components/dashboard-shell";

export default async function DashboardLayout({ children }: { children: React.ReactNode }) {
    const session = await getSession();

    if (!session.isLoggedIn || !session.user) {
        redirect({
            href: {
                pathname: "/",
                query: { auth: "login", returnTo: "/" + (await getLocale()) + "/dashboard" },
            },
            locale: await getLocale(),
        });
        return null;
    }

    return <DashboardShell user={session.user as any}>{children}</DashboardShell>;
}

export const metadata = { robots: { index: false, follow: false } };
