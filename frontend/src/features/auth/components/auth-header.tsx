import { Wallet } from "lucide-react";
import Link from "next/link";

interface AuthHeaderProps {
    title: string;
    description: string;
}

export function AuthHeader({ title, description }: AuthHeaderProps) {
    return (
        <div className="flex flex-col items-center text-center space-y-3 mb-6">
            <Link
                href="/"
                className="flex items-center justify-center w-10 h-10 rounded-md bg-zinc-900 text-zinc-50 dark:bg-zinc-100 dark:text-zinc-900 border border-zinc-800 dark:border-zinc-200"
            >
                <Wallet className="w-5 h-5 stroke-[1.75]" />
            </Link>
            <div>
                <h1 className="text-xl font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
                    {title}
                </h1>
                <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">{description}</p>
            </div>
        </div>
    );
}
