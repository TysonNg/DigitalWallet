import { defineRouting } from "next-intl/routing";

export const locales = ["vi", "en"] as const;
export type Locale = (typeof locales)[number];
export const routing = defineRouting({
    locales,
    defaultLocale: "vi",
    localePrefix: "always",
    localeDetection: false,
    localeCookie: { name: "NEXT_LOCALE", maxAge: 60 * 60 * 24 * 365, path: "/", sameSite: "lax" },
});
export function isLocale(value: string): value is Locale {
    return locales.includes(value as Locale);
}
