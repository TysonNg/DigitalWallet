import type { Metadata } from "next";
import { Inter } from "next/font/google";
import { Toaster } from "sonner";
import "./globals.css";

const inter = Inter({
    variable: "--font-inter",
    subsets: ["latin", "vietnamese"],
    display: "swap",
});

export const metadata: Metadata = {
    title: "Digital Wallet | Hệ Thống Ví Điện Tử",
    description: "Nền tảng ví điện tử cá nhân an toàn, quản lý số dư và giao dịch tài chính",
};

export default function RootLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    return (
        <html
            lang="vi"
            className={`${inter.variable} font-sans h-full antialiased`}
        >
            <body className="min-h-full flex flex-col bg-[#f8fafc] text-slate-900 font-sans">
                {children}
                <Toaster position="top-right" richColors closeButton theme="light" />
            </body>
        </html>
    );
}
