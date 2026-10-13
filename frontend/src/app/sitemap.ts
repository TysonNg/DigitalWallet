import type { MetadataRoute } from "next";
import { siteOrigin } from "@/i18n/site";
export default function sitemap(): MetadataRoute.Sitemap {
    const origin = siteOrigin();
    const languages = { vi: origin + "/vi", en: origin + "/en" };
    return ["vi", "en"].map((locale) => ({
        url: origin + "/" + locale,
        alternates: { languages },
    }));
}
