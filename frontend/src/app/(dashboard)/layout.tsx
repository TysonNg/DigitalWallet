import { getSession } from "@/server/session";
import { redirect } from "next/navigation";
import Link from "next/link";
import { Wallet, ShieldCheck } from "lucide-react";
import { LogoutButton } from "@/features/auth/components/logout-button";

export default async function DashboardLayout({ children }: { children: React.ReactNode }) {
    const session = await getSession();

    if (!session.isLoggedIn || !session.user) {
        redirect("/login");
    }

    const user = session.user;

    return (
        <div className="min-h-screen bg-zinc-50 dark:bg-zinc-950 text-zinc-900 dark:text-zinc-50">
            {/* Minimalist Top Navigation Bar */}
            <header className="sticky top-0 z-40 w-full border-b border-zinc-200 dark:border-zinc-800 bg-white/95 dark:bg-zinc-950/95 backdrop-blur-xs">
                <div className="max-w-5xl mx-auto flex h-14 items-center justify-between px-4 sm:px-6">
                    <div className="flex items-center space-x-3">
                        <Link href="/dashboard" className="flex items-center space-x-2">
                            <div className="flex items-center justify-center w-8 h-8 rounded-md bg-zinc-900 text-zinc-50 dark:bg-zinc-100 dark:text-zinc-900">
                                <Wallet className="w-4 h-4 stroke-[1.75]" />
                            </div>
                            <span className="font-semibold text-sm tracking-tight">
                                Digital Wallet
                            </span>
                        </Link>
                        <div className="hidden sm:flex items-center space-x-1 pl-4 border-l border-zinc-200 dark:border-zinc-800 text-xs text-zinc-500">
                            <ShieldCheck className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400 mr-1" />
                            BFF Stateless Session
                        </div>
                    </div>

                    <div className="flex items-center space-x-3">
                        <div className="hidden md:flex flex-col items-end text-xs">
                            <span className="font-medium text-zinc-900 dark:text-zinc-100">
                                {user.fullName}
                            </span>
                            <span className="text-zinc-500 dark:text-zinc-400">{user.email}</span>
                        </div>
                        <LogoutButton />
                    </div>
                </div>
            </header>

            {/* Main Content Area */}
            <main className="max-w-5xl mx-auto p-4 sm:p-6 lg:p-8">{children}</main>
        </div>
    );
}
