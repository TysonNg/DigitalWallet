import { getRequestConfig } from "next-intl/server";
import { cookies } from "next/headers";
import { isLocale } from "./routing";
import vi from "../../messages/vi.json";
import en from "../../messages/en.json";
export default getRequestConfig(async ({ requestLocale }) => {
    const requested = await requestLocale;
    const cookie = (await cookies()).get("NEXT_LOCALE")?.value;
    const locale =
        requested && isLocale(requested) ? requested : cookie && isLocale(cookie) ? cookie : "vi";
    return { locale, messages: locale === "vi" ? vi : en, timeZone: "Asia/Ho_Chi_Minh" };
});
