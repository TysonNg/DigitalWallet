import { getSession } from "@/server/session";
import { redirect } from "next/navigation";

export default async function DashboardLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    const session = await getSession();

    if (!session.isLoggedIn || !session.user) {
        redirect("/?auth=login");
    }

    return <>{children}</>;
}
