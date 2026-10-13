import { NextRequest, NextResponse } from "next/server";
import { isLocale } from "./i18n/routing";
import { resolveLocale } from "./i18n/paths";

export function proxy(request: NextRequest) {
    const pathname = request.nextUrl.pathname;
    if (/^\/(api|_next|_vercel)(\/|$)/.test(pathname) || /\.[^/]+$/.test(pathname))
        return NextResponse.next();
    const first = pathname.split("/")[1];
    if (/^[a-z]{2}(?:-[A-Za-z]{2})?$/.test(first) && !isLocale(first)) {
        return new NextResponse("Not Found", { status: 404 });
    }
    const locale = resolveLocale(pathname, request.cookies.get("NEXT_LOCALE")?.value);
    let response: NextResponse;
    if (!isLocale(first)) {
        const url = request.nextUrl.clone();
        url.pathname = "/" + locale + (pathname === "/" ? "" : pathname);
        response = NextResponse.redirect(url);
    } else {
        const headers = new Headers(request.headers);
        headers.set("X-NEXT-INTL-LOCALE", locale);
        response = NextResponse.next({ request: { headers } });
    }
    response.cookies.set("NEXT_LOCALE", locale, {
        maxAge: 31536000,
        path: "/",
        sameSite: "lax",
        secure: request.nextUrl.protocol === "https:",
    });
    return response;
}
export const config = { matcher: ["/((?!api|_next|_vercel|.*\\..*).*)"] };
