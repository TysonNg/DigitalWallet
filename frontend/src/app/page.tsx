import { getSession } from "@/server/session";
import { redirect } from "next/navigation";
import { LandingView } from "@/features/landing/landing-view";

export const metadata = {
    title: "Digital Wallet | Hệ thống Ví Điện Tử Bảo Mật & Thông Minh",
    description: "Ví điện tử cá nhân với kiến trúc BFF và bảo mật ngân hàng số",
};

export default async function HomePage() {
    const session = await getSession();

    if (session.isLoggedIn) {
        redirect("/dashboard");
    }

    return <LandingView />;
}
