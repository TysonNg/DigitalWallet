import { redirect } from "@/i18n/navigation";
import { getLocale } from "next-intl/server";

export default async function LoginPage() {
    redirect({
        href: {
            pathname: "/",
            query: { auth: "login", returnTo: "/" + (await getLocale()) + "/dashboard" },
        },
        locale: await getLocale(),
    });
}

export const metadata = { robots: { index: false, follow: false } };
