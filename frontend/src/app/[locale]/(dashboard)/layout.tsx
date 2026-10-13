import { getSession } from "@/server/session";
import { redirect } from "@/i18n/navigation";
import { getLocale } from "next-intl/server";

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
    }

    return <>{children}</>;
}

export const metadata = { robots: { index: false, follow: false } };
