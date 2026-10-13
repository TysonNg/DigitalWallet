import type { Metadata } from "next";
import { siteOrigin } from "@/i18n/site";
import { getSession } from "@/server/session";
import { redirect } from "@/i18n/navigation";
import { getLocale } from "next-intl/server";
import { LandingView } from "@/features/landing/landing-view";

export default async function HomePage() {
    const session = await getSession();

    if (session.isLoggedIn) {
        redirect({ href: "/dashboard", locale: await getLocale() });
    }

    return <LandingView />;
}

export async function generateMetadata({
    params,
    searchParams,
}: {
    params: Promise<{ locale: string }>;
    searchParams: Promise<Record<string, string | string[] | undefined>>;
}): Promise<Metadata> {
    const { locale } = await params;
    const query = await searchParams;
    const origin = siteOrigin();
    return {
        title:
            locale === "vi"
                ? "Digital Wallet | Ví điện tử an toàn"
                : "Digital Wallet | Secure digital wallet",
        description:
            locale === "vi"
                ? "Quản lý số dư và giao dịch với ví điện tử cá nhân."
                : "Manage your balance and transactions with your personal digital wallet.",
        alternates: {
            canonical: origin + "/" + locale,
            languages: { vi: origin + "/vi", en: origin + "/en" },
        },
        robots: query.auth ? { index: false, follow: true } : { index: true, follow: true },
    };
}
