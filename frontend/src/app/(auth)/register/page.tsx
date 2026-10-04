import { Card } from "@/components/ui/card";
import { RegisterView } from "@/features/auth/components/register-view";
import { getSession } from "@/server/session";
import { redirect } from "next/navigation";

export const metadata = {
    title: "Đăng ký tài khoản | Digital Wallet",
    description: "Mở ví điện tử an toàn với quy trình xác thực OTP 2 bước",
};

export default async function RegisterPage() {
    const session = await getSession();
    if (session.isLoggedIn) {
        redirect("/dashboard");
    }

    return (
        <div className="min-h-screen flex items-center justify-center p-4 bg-zinc-50 dark:bg-zinc-950">
            <div className="w-full max-w-md">
                <Card>
                    <RegisterView />
                </Card>
            </div>
        </div>
    );
}
