import { redirect } from "@/i18n/navigation";
import { getLocale } from "next-intl/server";

export default async function RegisterPage() {
    redirect({ href: "/?auth=register", locale: await getLocale() });
}

export const metadata = { robots: { index: false, follow: false } };
