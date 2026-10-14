import { getSession } from "@/server/session";
import { SettingsView } from "@/features/dashboard/views/settings-view";
import { redirect } from "@/i18n/navigation";
import { getLocale } from "next-intl/server";

export default async function SettingsPage() {
    const session = await getSession();

    if (!session.isLoggedIn || !session.user) {
        redirect({
            href: {
                pathname: "/",
                query: { auth: "login", returnTo: "/" + (await getLocale()) + "/dashboard/settings" },
            },
            locale: await getLocale(),
        });
    }

    return <SettingsView user={session.user!} />;
}

export const metadata = {
    title: "Cài đặt & Bảo mật | Digital Wallet",
};
