export function siteOrigin() {
    const value = process.env.SITE_URL || "http://localhost:3000";
    return new URL(value).origin;
}
