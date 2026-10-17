import { getSession } from "@/server/session";
import { redirect } from "@/i18n/navigation";
import { getLocale } from "next-intl/server";
import { getAdminStatsAction, getAdminUsersAction } from "@/features/admin/actions/admin.actions";
import { AdminConsoleView } from "@/features/admin/views/admin-console-view";
import { Metadata } from "next";

export const metadata: Metadata = {
    title: "Admin Backoffice Console | DigitalWallet",
    description: "Cổng vận hành và quản trị hệ thống ví điện tử DigitalWallet.",
};

export default async function AdminDashboardPage() {
    const locale = await getLocale();
    const session = await getSession();

    // 1. Kiểm tra đăng nhập
    if (!session.isLoggedIn || !session.user) {
        redirect({
            href: {
                pathname: "/",
                query: { auth: "login", returnTo: `/${locale}/admin` },
            },
            locale,
        });
        return null;
    }

    const user = session.user;


    // 2. Role Guard: Bắt buộc phải có ROLE_ADMIN hoặc quyền SYSTEM_STATS
    const isAdmin =
        user.roles?.includes("ROLE_ADMIN") || user.authorities?.includes("SYSTEM_STATS");

    if (!isAdmin) {
        // Chuyển hướng người dùng thường về trang Dashboard cá nhân kèm thông báo lỗi
        redirect({
            href: {
                pathname: "/dashboard",
                query: { error: "unauthorized" },
            },
            locale,
        });
    }

    // 3. Nạp dữ liệu ban đầu song song từ Backend
    const [statsRes, usersRes] = await Promise.all([
        getAdminStatsAction(),
        getAdminUsersAction(),
    ]);

    return (
        <AdminConsoleView
            currentUser={user}
            initialStats={statsRes.data ?? null}
            initialUsers={usersRes.data ?? null}
        />
    );
}
