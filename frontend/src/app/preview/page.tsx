import { DashboardView } from "@/features/dashboard/dashboard-view";

export default function PreviewPage() {
    const mockUser = {
        id: "usr-88a99b-preview",
        email: "minhanh@example.com",
        fullName: "Nguyễn Minh Anh",
        phoneNumber: "0912345678",
        dateOfBirth: "1998-05-15",
        status: "ACTIVE" as const,
    };

    const mockWallet = {
        id: "wal-8910-preview",
        userId: mockUser.id,
        balance: 12450000,
        currency: "VND",
        status: "ACTIVE" as const,
    };

    return (
        <DashboardView
            user={mockUser}
            initialWallet={mockWallet}
            initialTransactions={[]}
        />
    );
}
