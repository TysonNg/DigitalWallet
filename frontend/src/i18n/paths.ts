import { isLocale, type Locale } from "./routing";

export function safeReturnTo(value: string | null | undefined, locale: string): string {
    const selected = isLocale(locale) ? locale : "vi";
    const fallback = "/" + selected + "/dashboard";
    if (!value || !value.startsWith("/") || value.startsWith("//") || value.includes("\\"))
        return fallback;
    try {
        const url = new URL(value, "https://wallet.invalid");
        if (
            url.origin !== "https://wallet.invalid" ||
            !/^\/(?:vi\/|en\/)?dashboard\/?$/.test(url.pathname)
        )
            return fallback;
        return fallback + url.search + url.hash;
    } catch {
        return fallback;
    }
}
export function resolveLocale(pathname: string, cookie?: string): Locale {
    const first = pathname.split("/")[1];
    return isLocale(first) ? first : cookie && isLocale(cookie) ? cookie : "vi";
}
