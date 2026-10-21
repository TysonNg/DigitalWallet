export interface DraftTransferPayload {
    recipientUserId?: string | null;
    recipientWalletId?: string | null;
    recipientName: string;
    recipientPhoneOrEmail: string;
    amount: number;
    note: string;
    fee: number;
    canProceed: boolean;
    message: string;
}

export interface ChatAssistantResponse {
    answer: string;
    draftTransfer?: DraftTransferPayload | null;
}

export interface ChatMessage {
    id: string;
    role: "user" | "assistant";
    content: string;
    timestamp: Date;
    draftTransfer?: DraftTransferPayload | null;
}
