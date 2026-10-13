"use client";
import { useLocale } from "next-intl";
import { useRouter, usePathname } from "@/i18n/navigation";
import { useSearchParams } from "next/navigation";
import { safeReturnTo } from "@/i18n/paths";
export function LanguageSwitcher() {
    const locale = useLocale();
    const router = useRouter();
    const pathname = usePathname();
    const searchParams = useSearchParams();
    return (
        <select
            aria-label={locale === "vi" ? "Ngôn ngữ" : "Language"}
            value={locale}
            className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs max-w-32"
            onChange={(event) => {
                const next = event.target.value;
                const query = new URLSearchParams(searchParams.toString());
                if (query.has("returnTo"))
                    query.set("returnTo", safeReturnTo(query.get("returnTo"), next));
                router.replace(
                    pathname + (query.size ? "?" + query.toString() : "") + window.location.hash,
                    { locale: next, scroll: false }
                );
            }}
        >
            <option value="vi">Tiếng Việt</option>
            <option value="en">English</option>
        </select>
    );
}
