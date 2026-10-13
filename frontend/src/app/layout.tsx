import { getLocale } from "next-intl/server";
import { Inter } from "next/font/google";
import { Toaster } from "sonner";
import { QueryProvider } from "@/providers/query-provider";
import "./globals.css";

const inter = Inter({
    variable: "--font-inter",
    subsets: ["latin", "vietnamese"],
    display: "swap",
});

export default async function RootLayout({ children }: { children: React.ReactNode }) {
    const locale = await getLocale();

    return (
        <html lang={locale} className={`${inter.variable} font-sans h-full antialiased`}>
            <body className="min-h-full flex flex-col bg-[#f8fafc] text-slate-900 font-sans">
                <QueryProvider>{children}</QueryProvider>
                <Toaster position="top-right" richColors closeButton theme="light" />
            </body>
        </html>
    );
}
