import { notFound } from "next/navigation";
import { isReceiveUuid } from "@/features/wallet/receive-qr";
import { ReceiveRecipientView } from "@/features/wallet/components/receive-recipient-view";

export const metadata = { robots: { index: false, follow: false } };

export default async function ReceivePage({
    params,
}: {
    params: Promise<{ locale: string; token: string }>;
}) {
    const { locale, token } = await params;
    if (!isReceiveUuid(token)) notFound();

    const returnTo = `/${locale}/receive/${token}`;

    return (
        <ReceiveRecipientView
            key={token}
            token={token}
            loginHref={`/?auth=login&returnTo=${encodeURIComponent(returnTo)}`}
        />
    );
}
