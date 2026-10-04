import { Card } from "@/components/ui/card";
import { AuthHeader } from "@/features/auth/components/auth-header";
import { LoginForm } from "@/features/auth/components/login-form";
import { getSession } from "@/server/session";
import { redirect } from "next/navigation";

export const metadata = {
    title: "Đăng nhập | Digital Wallet",
    description: "Đăng nhập vào tài khoản ví điện tử bảo mật của bạn",
};

export default async function LoginPage() {
    const session = await getSession();
    if (session.isLoggedIn) {
        redirect("/dashboard");
    }

    return (
        <div className="min-h-screen flex items-center justify-center p-4 bg-zinc-50 dark:bg-zinc-950">
            <div className="w-full max-w-sm">
                <AuthHeader
                    title="Đăng nhập Ví điện tử"
                    description="Nhập thông tin xác thực để truy cập số dư và giao dịch"
                />
                <Card>
                    <LoginForm />
                </Card>
            </div>
        </div>
    );
}
