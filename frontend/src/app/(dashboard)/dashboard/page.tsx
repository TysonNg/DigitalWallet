import { getSession } from "@/server/session";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { formatVND } from "@/lib/utils";
import {
    ArrowDownLeft,
    ArrowUpRight,
    Send,
    CreditCard,
    Lock,
    Layers,
    CheckCircle2,
} from "lucide-react";

export const metadata = {
    title: "Bảng điều khiển | Digital Wallet",
    description: "Quản lý số dư và giao dịch ví điện tử an toàn",
};

export default async function DashboardPage() {
    const session = await getSession();
    const user = session.user!;

    return (
        <div className="space-y-6">
            {/* Top Welcome Section */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-zinc-200 dark:border-zinc-800 pb-5">
                <div>
                    <h1 className="text-xl sm:text-2xl font-semibold tracking-tight text-zinc-900 dark:text-zinc-50">
                        Xin chào, {user.fullName}
                    </h1>
                    <p className="text-xs sm:text-sm text-zinc-500 dark:text-zinc-400 mt-0.5">
                        Tài khoản ví điện tử đang hoạt động và được bảo vệ bởi BFF Architecture.
                    </p>
                </div>
                <div className="flex items-center space-x-2">
                    <Badge variant="success" className="h-6">
                        <CheckCircle2 className="w-3 h-3 mr-1 text-emerald-600 dark:text-emerald-400" />
                        {user.status || "ACTIVE"}
                    </Badge>
                    <Badge variant="outline" className="h-6">
                        ID: {user.id.slice(0, 8)}...
                    </Badge>
                </div>
            </div>

            {/* Main Wallet Card & Quick Actions */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {/* Wallet Balance Card */}
                <Card className="md:col-span-2">
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <div>
                            <CardTitle className="text-sm font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                                Số dư khả dụng
                            </CardTitle>
                            <CardDescription>Ví chính (VND Wallet)</CardDescription>
                        </div>
                        <div className="p-2 rounded-md bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300">
                            <CreditCard className="w-5 h-5 stroke-[1.75]" />
                        </div>
                    </CardHeader>
                    <CardContent className="pt-2">
                        <div className="text-3xl sm:text-4xl font-semibold tracking-tight text-zinc-900 dark:text-zinc-50">
                            {formatVND(0)}
                        </div>
                        <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-2">
                            Sẵn sàng cho các giao dịch nạp, rút và chuyển tiền tức thời.
                        </p>

                        <div className="grid grid-cols-3 gap-2 mt-6 pt-4 border-t border-zinc-100 dark:border-zinc-900">
                            <Button variant="outline" size="sm" className="w-full text-xs">
                                <ArrowDownLeft className="w-3.5 h-3.5 mr-1 text-emerald-600 dark:text-emerald-400" />
                                Nạp tiền
                            </Button>
                            <Button variant="outline" size="sm" className="w-full text-xs">
                                <ArrowUpRight className="w-3.5 h-3.5 mr-1 text-amber-600 dark:text-amber-400" />
                                Rút tiền
                            </Button>
                            <Button variant="primary" size="sm" className="w-full text-xs">
                                <Send className="w-3.5 h-3.5 mr-1" />
                                Chuyển ví
                            </Button>
                        </div>
                    </CardContent>
                </Card>

                {/* User Profile Summary Card */}
                <Card className="flex flex-col justify-between">
                    <CardHeader className="pb-2">
                        <CardTitle className="text-sm font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                            Hồ sơ định danh
                        </CardTitle>
                        <CardDescription>Thông tin liên kết ví</CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-3 text-xs">
                        <div className="flex justify-between py-1.5 border-b border-zinc-100 dark:border-zinc-900">
                            <span className="text-zinc-500">Email:</span>
                            <span className="font-medium text-zinc-800 dark:text-zinc-200">
                                {user.email}
                            </span>
                        </div>
                        <div className="flex justify-between py-1.5 border-b border-zinc-100 dark:border-zinc-900">
                            <span className="text-zinc-500">Số điện thoại:</span>
                            <span className="font-medium text-zinc-800 dark:text-zinc-200">
                                {user.phoneNumber || "Chưa cập nhật"}
                            </span>
                        </div>
                        <div className="flex justify-between py-1.5 border-b border-zinc-100 dark:border-zinc-900">
                            <span className="text-zinc-500">Bảo mật xác thực:</span>
                            <span className="font-medium text-emerald-600 dark:text-emerald-400 flex items-center">
                                <Lock className="w-3 h-3 mr-1" /> Argon2 + OTP
                            </span>
                        </div>
                    </CardContent>
                    <div className="p-4 pt-0">
                        <Button variant="secondary" size="sm" className="w-full text-xs">
                            Quản lý hồ sơ
                        </Button>
                    </div>
                </Card>
            </div>

            {/* Security Architecture Info Callout */}
            <div className="rounded-md border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-950 p-4 sm:p-5">
                <div className="flex items-start space-x-3">
                    <div className="p-2 rounded-md bg-zinc-100 dark:bg-zinc-900 text-zinc-700 dark:text-zinc-300 shrink-0">
                        <Layers className="w-4 h-4" />
                    </div>
                    <div className="text-xs space-y-1">
                        <h4 className="font-semibold text-zinc-900 dark:text-zinc-100">
                            Kiến trúc bảo mật Backend-For-Frontend (BFF) đang hoạt động
                        </h4>
                        <p className="text-zinc-500 dark:text-zinc-400 leading-relaxed">
                            Trình duyệt của bạn đang kết nối an toàn với máy chủ Next.js BFF qua
                            phiên mã hóa AES-256. Hoàn toàn{" "}
                            <strong>không có mã JWT Token nào lưu tại trình duyệt</strong> (loại trừ
                            100% rủi ro XSS). Mọi yêu cầu ví tiền sẽ được BFF tự động đính kèm Token
                            ký số sang Backend Spring Boot qua mạng nội bộ.
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
}
