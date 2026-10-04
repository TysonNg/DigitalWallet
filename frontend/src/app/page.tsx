import { getSession } from "@/server/session";
import { redirect } from "next/navigation";
import Link from "next/link";
import { Wallet, ShieldCheck, Zap, Lock } from "lucide-react";
import { Button } from "@/components/ui/button";

export const metadata = {
    title: "Digital Wallet | Hệ thống Ví Điện Tử Bảo Mật",
    description: "Ví điện tử cá nhân với kiến trúc BFF và bảo mật ngân hàng số",
};

export default async function HomePage() {
    const session = await getSession();

    if (session.isLoggedIn) {
        redirect("/dashboard");
    }

    return (
        <div className="min-h-screen flex flex-col bg-zinc-50 dark:bg-zinc-950 text-zinc-900 dark:text-zinc-50">
            {/* Top Navbar */}
            <nav className="border-b border-zinc-200 dark:border-zinc-800 bg-white/80 dark:bg-zinc-950/80 backdrop-blur-xs">
                <div className="max-w-5xl mx-auto flex h-14 items-center justify-between px-4 sm:px-6">
                    <div className="flex items-center space-x-2.5">
                        <div className="flex items-center justify-center w-8 h-8 rounded-md bg-zinc-900 text-zinc-50 dark:bg-zinc-100 dark:text-zinc-900">
                            <Wallet className="w-4 h-4 stroke-[1.75]" />
                        </div>
                        <span className="font-semibold text-sm tracking-tight">Digital Wallet</span>
                    </div>

                    <div className="flex items-center space-x-2">
                        <Link href="/login">
                            <Button variant="ghost" size="sm">
                                Đăng nhập
                            </Button>
                        </Link>
                        <Link href="/register">
                            <Button variant="primary" size="sm">
                                Mở ví ngay
                            </Button>
                        </Link>
                    </div>
                </div>
            </nav>

            {/* Hero Section */}
            <main className="flex-1 flex flex-col justify-center max-w-4xl mx-auto px-4 py-16 sm:py-24 text-center">
                <div className="inline-flex items-center self-center rounded-xs border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 px-2.5 py-1 text-xs text-zinc-600 dark:text-zinc-400 mb-6">
                    <ShieldCheck className="w-3.5 h-3.5 mr-1.5 text-emerald-600 dark:text-emerald-400" />
                    Kiến trúc Backend-For-Frontend (BFF) & Stateless Session
                </div>

                <h1 className="text-3xl sm:text-5xl font-semibold tracking-tight text-zinc-900 dark:text-zinc-50 leading-tight">
                    Hệ thống Ví Điện Tử Cá Nhân <br className="hidden sm:inline" />
                    Bảo Mật & Chuẩn Mực Cao
                </h1>

                <p className="mt-4 text-sm sm:text-base text-zinc-500 dark:text-zinc-400 max-w-2xl mx-auto leading-relaxed">
                    Nền tảng ví tiền được xây dựng với Spring Boot Domain-Driven Design (DDD), xác
                    thực 2 bước qua OTP email, băm mật khẩu Argon2 và lớp giao diện Next.js BFF
                    không lưu token trên trình duyệt.
                </p>

                <div className="flex items-center justify-center space-x-3 mt-8">
                    <Link href="/register">
                        <Button size="lg" className="h-10 px-5 text-sm">
                            Đăng ký tài khoản mới
                        </Button>
                    </Link>
                    <Link href="/login">
                        <Button variant="outline" size="lg" className="h-10 px-5 text-sm">
                            Đăng nhập ví
                        </Button>
                    </Link>
                </div>

                {/* Feature Grid */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-16 text-left">
                    <div className="rounded-md border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-950 p-4">
                        <Lock className="w-5 h-5 text-zinc-700 dark:text-zinc-300 mb-2 stroke-[1.75]" />
                        <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                            Bảo mật Argon2 & OTP
                        </h3>
                        <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1 leading-relaxed">
                            Mật khẩu được băm bằng thuật toán tiêu chuẩn OWASP #1, đăng ký 2 bước
                            gửi OTP vào email.
                        </p>
                    </div>

                    <div className="rounded-md border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-950 p-4">
                        <ShieldCheck className="w-5 h-5 text-zinc-700 dark:text-zinc-300 mb-2 stroke-[1.75]" />
                        <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                            BFF Token-Handler
                        </h3>
                        <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1 leading-relaxed">
                            Trình duyệt chỉ giữ Cookie phiên mã hóa AES-256. Không bao giờ lưu JWT ở
                            client, loại trừ rủi ro XSS.
                        </p>
                    </div>

                    <div className="rounded-md border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-950 p-4">
                        <Zap className="w-5 h-5 text-zinc-700 dark:text-zinc-300 mb-2 stroke-[1.75]" />
                        <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                            Session Rotation & Redis
                        </h3>
                        <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1 leading-relaxed">
                            Xoay vòng Refresh Token (RTR), phát hiện đăng nhập thiết bị khác và
                            Blacklist token ngay khi logout.
                        </p>
                    </div>
                </div>
            </main>

            {/* Footer */}
            <footer className="border-t border-zinc-200 dark:border-zinc-800 py-6 text-center text-xs text-zinc-400">
                <p>Digital Wallet System © 2026. Xây dựng theo kiến trúc DDD & BFF.</p>
            </footer>
        </div>
    );
}
