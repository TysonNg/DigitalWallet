"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { logoutAction } from "../actions/auth.actions";
import { Button } from "@/components/ui/button";
import { LogOut } from "lucide-react";
import { useAuthStore } from "../store/auth.store";

export function LogoutButton() {
    const router = useRouter();
    const [loading, setLoading] = useState(false);
    const setCurrentUser = useAuthStore((state) => state.setCurrentUser);

    const handleLogout = async () => {
        setLoading(true);
        await logoutAction();
        setCurrentUser(null);
        router.push("/login");
        router.refresh();
    };

    return (
        <Button
            variant="outline"
            size="sm"
            onClick={handleLogout}
            isLoading={loading}
            className="text-xs text-zinc-600 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-100"
        >
            <LogOut className="w-3.5 h-3.5 mr-1.5" />
            Đăng xuất
        </Button>
    );
}
