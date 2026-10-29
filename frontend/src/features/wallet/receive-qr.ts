/** Canonical UUIDs only: never allow path, query, or encoded separators. */
export function isReceiveUuid(value: string): boolean {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);
}

export interface ReceiveQr {
    mediaId: string;
    contentUrl: string;
    receiveUrl: string;
}

export interface ReceiveRecipient {
    walletId: string;
    fullName: string;
    currency: string;
}
