import { getSession } from "@/server/session";
import { redirect } from "@/i18n/navigation";
import { getLocale } from "next-intl/server";
import { DashboardView } from "@/features/dashboard/dashboard-view";

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
    return <DashboardView user={user} />;
}
