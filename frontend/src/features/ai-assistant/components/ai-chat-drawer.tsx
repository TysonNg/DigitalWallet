"use client";

import { useState, useRef, useEffect } from "react";
import {
    Sparkles,
    X,
    Send,
    Bot,
    User as UserIcon,
    RotateCcw,
    ChevronDown,
    Zap,
    CheckCircle2,
    AlertCircle,
    Loader2,
} from "lucide-react";
import { toast } from "sonner";
import { clearAiConversationAction } from "../actions/ai.actions";
import { streamAiChat } from "../services/ai-stream";
import { ChatMessage, DraftTransferPayload } from "../types/ai.types";
import { DraftTransferCard } from "./draft-transfer-card";
import { FormattedAiMessage } from "./formatted-ai-message";
import { TransferModal } from "@/features/wallet/components/transfer-modal";

interface AiChatDrawerProps {
    walletId?: string;
    currentBalance?: number;
    currency?: string;
}

const QUICK_SUGGESTIONS = [
    { label: "💳 Phí nạp tiền Stripe", prompt: "Phí nạp tiền qua cổng Stripe là bao nhiêu?" },
    {
        label: "📊 Hạn mức giao dịch",
        prompt: "Hạn mức chuyển tiền trong ngày của tài khoản là bao nhiêu?",
    },
    {
        label: "🛡️ Quy định an toàn mã PIN",
        prompt: "Nhập sai mã PIN bao nhiêu lần thì ví bị khóa?",
    },
    { label: "📜 Lịch sử giao dịch", prompt: "Cho tôi xem 3 giao dịch gần nhất của tôi" },
    { label: "💸 Soạn lệnh chuyển tiền", prompt: "Chuyển 50.000đ cho Nam nội dung ăn trưa" },
];

const createConversationId = () => "conv-" + crypto.randomUUID();

export function AiChatDrawer({
    walletId,
    currentBalance = 0,
    currency = "VND",
}: AiChatDrawerProps) {
    const [isOpen, setIsOpen] = useState(false);
    const [input, setInput] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [conversationId, setConversationId] = useState(createConversationId);
    const requestInFlight = useRef(false);
    const streamController = useRef<AbortController | null>(null);

    useEffect(() => () => streamController.current?.abort(), []);

    // State cho Modal chuyển tiền an toàn (Human-in-the-loop)
    const [isTransferModalOpen, setIsTransferModalOpen] = useState(false);
    const [selectedRecipient, setSelectedRecipient] = useState<{
        identifier: string;
        walletId: string;
        fullName: string;
        amount: number;
        description: string;
    } | null>(null);

    const messagesEndRef = useRef<HTMLDivElement>(null);
    const inputRef = useRef<HTMLInputElement>(null);

    // Tự động cuộn xuống tin nhắn mới nhất
    useEffect(() => {
        if (isOpen) {
            messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
        }
    }, [messages, isOpen, isLoading]);

    // Focus input khi mở chat
    useEffect(() => {
        if (isOpen) {
            setTimeout(() => inputRef.current?.focus(), 150);
        }
    }, [isOpen]);

    const handleSendMessage = async (textToSend?: string) => {
        const queryText = (textToSend || input).trim();
        if (!queryText || requestInFlight.current) return;

        requestInFlight.current = true;
        const controller = new AbortController();
        streamController.current = controller;

        const userMsg: ChatMessage = {
            id: "msg-" + Date.now(),
            role: "user",
            content: queryText,
            timestamp: new Date(),
        };

        const assistantMsgId = "msg-" + (Date.now() + 1);
        const initialAssistantMsg: ChatMessage = {
            id: assistantMsgId,
            role: "assistant",
            content: "",
            timestamp: new Date(),
        };

        setMessages((prev) => [...prev, userMsg, initialAssistantMsg]);
        setInput("");
        setIsLoading(true);

        let streamedText = "";

        try {
            await streamAiChat(
                queryText,
                conversationId,
                {
                    onToken: (token) => {
                        streamedText += token;
                        setMessages((prev) =>
                            prev.map((m) =>
                                m.id === assistantMsgId ? { ...m, content: streamedText } : m
                            )
                        );
                    },
                    onDraftTransfer: (draft) => {
                        setMessages((prev) =>
                            prev.map((m) =>
                                m.id === assistantMsgId ? { ...m, draftTransfer: draft } : m
                            )
                        );
                    },
                    onError: (error) => {
                        toast.error(error);
                    },
                    onDone: () => {
                        setIsLoading(false);
                    },
                },
                controller.signal
            );
        } catch (streamErr) {
            if (controller.signal.aborted) return;
            console.warn("AI stream failed:", streamErr);
            // Do not replay a request whose tools or memory may already have been processed.
            const errorText = "Không thể hoàn tất phản hồi AI. Vui lòng thử lại.";
            setMessages((prev) =>
                prev.map((message) =>
                    message.id === assistantMsgId
                        ? {
                              ...message,
                              content: streamedText
                                  ? `${streamedText}\n\n⚠️ ${errorText}`
                                  : `⚠️ ${errorText}`,
                              draftTransfer: undefined,
                          }
                        : message
                )
            );
        } finally {
            streamController.current = null;
            requestInFlight.current = false;
            setIsLoading(false);
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    const handleConfirmDraftTransfer = (payload: DraftTransferPayload) => {
        if (
            !payload ||
            payload.canProceed !== true ||
            typeof payload.recipientWalletId !== "string" ||
            !payload.recipientWalletId.trim() ||
            payload.recipientWalletId.trim() === walletId ||
            !Number.isSafeInteger(payload.amount) ||
            payload.amount <= 0 ||
            typeof payload.note !== "string" ||
            typeof payload.recipientPhoneOrEmail !== "string" ||
            typeof payload.recipientName !== "string"
        ) {
            toast.error("Không tìm thấy ví người nhận hợp lệ để chuyển.");
            return;
        }

        setSelectedRecipient({
            identifier: payload.recipientPhoneOrEmail,
            walletId: payload.recipientWalletId,
            fullName: payload.recipientName,
            amount: payload.amount,
            description: payload.note,
        });

        setIsTransferModalOpen(true);
    };

    const handleReset = () => {
        if (requestInFlight.current) return;

        const oldConvId = conversationId;
        setMessages([]);
        setInput("");
        setSelectedRecipient(null);
        setIsTransferModalOpen(false);
        setConversationId(createConversationId());

        if (oldConvId) {
            clearAiConversationAction(oldConvId).catch((err) =>
                console.warn("Failed to clear AI conversation in Redis:", err)
            );
        }
    };

    const handleTransferSuccess = () => {
        setIsTransferModalOpen(false);
        toast.success("Chuyển tiền thành công!");
        setMessages((prev) => [
            ...prev,
            {
                id: "msg-success-" + Date.now(),
                role: "assistant",
                content:
                    "✅ **Giao dịch đã được xác thực mã PIN và chuyển tiền thành công!** Số dư ví của bạn đã được cập nhật.",
                timestamp: new Date(),
            },
        ]);
    };

    return (
        <>
            {/* 1. Nút mở Floating Chat Widget góc dưới bên phải */}
            <div className="fixed bottom-6 right-6 z-40">
                {!isOpen && (
                    <button
                        onClick={() => setIsOpen(true)}
                        className="group relative flex items-center gap-2.5 px-4 py-3.5 bg-gradient-to-r from-indigo-600 via-indigo-700 to-blue-600 hover:from-indigo-500 hover:to-blue-500 text-white rounded-full shadow-xl shadow-indigo-500/25 transition-all duration-300 hover:scale-105 active:scale-95 cursor-pointer"
                        aria-label="Mở Trợ lý AI"
                    >
                        <div className="relative">
                            <Sparkles className="w-5 h-5 text-amber-300 animate-pulse" />
                            <span className="absolute -top-1 -right-1 w-2.5 h-2.5 bg-emerald-400 rounded-full border-2 border-indigo-700" />
                        </div>
                        <span className="font-semibold text-xs tracking-wide">Trợ lý AI</span>
                    </button>
                )}
            </div>

            {/* 2. Cửa sổ Chat Drawer */}
            {isOpen && (
                <div className="fixed bottom-6 right-6 z-50 w-[420px] max-w-[calc(100vw-2rem)] h-[620px] max-h-[85vh] bg-white rounded-3xl shadow-2xl border border-slate-200/90 flex flex-col overflow-hidden animate-in fade-in slide-in-from-bottom-5 duration-200">
                    {/* Header */}
                    <div className="px-5 py-4 bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 text-white flex items-center justify-between shadow-sm">
                        <div className="flex items-center gap-3">
                            <div className="relative w-9 h-9 rounded-2xl bg-indigo-500/20 border border-indigo-400/30 flex items-center justify-center text-indigo-300 shadow-inner">
                                <Bot className="w-5 h-5 text-indigo-300" />
                                <span className="absolute bottom-0 right-0 w-2.5 h-2.5 bg-emerald-400 rounded-full border-2 border-slate-900" />
                            </div>
                            <div>
                                <div className="flex items-center gap-1.5">
                                    <h3 className="font-bold text-sm tracking-tight">
                                        Trợ lý AI Digital Wallet
                                    </h3>
                                    <span className="text-[10px] px-1.5 py-0.2 bg-indigo-500/30 text-indigo-200 rounded font-mono">
                                        RAG + Agent
                                    </span>
                                </div>
                                <p className="text-[11px] text-slate-400">
                                    Tư vấn biểu phí, hạn mức & điều phối ví
                                </p>
                            </div>
                        </div>

                        <div className="flex items-center gap-1">
                            <button
                                onClick={handleReset}
                                disabled={isLoading}
                                title="Làm mới đoạn chat"
                                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-white/10 transition-colors cursor-pointer"
                            >
                                <RotateCcw className="w-4 h-4" />
                            </button>
                            <button
                                onClick={() => setIsOpen(false)}
                                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-white/10 transition-colors cursor-pointer"
                            >
                                <X className="w-5 h-5" />
                            </button>
                        </div>
                    </div>

                    {/* Messages Body */}
                    <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-slate-50/50">
                        {/* Welcome State khi chưa có tin nhắn */}
                        {messages.length === 0 && (
                            <div className="py-6 px-3 text-center space-y-4">
                                <div className="w-12 h-12 mx-auto rounded-2xl bg-gradient-to-br from-indigo-100 to-blue-100 text-indigo-600 flex items-center justify-center shadow-inner">
                                    <Zap className="w-6 h-6 text-indigo-600" />
                                </div>
                                <div className="space-y-1">
                                    <h4 className="font-bold text-sm text-slate-900">
                                        Tôi có thể giúp gì cho bạn hôm nay?
                                    </h4>
                                    <p className="text-xs text-slate-500 max-w-[280px] mx-auto">
                                        Tra cứu chính sách biểu phí, hạn mức giao dịch hoặc ra lệnh
                                        chuyển tiền trực tiếp.
                                    </p>
                                </div>

                                {/* Gợi ý câu hỏi nhanh */}
                                <div className="pt-2 grid grid-cols-1 gap-2 text-left">
                                    {QUICK_SUGGESTIONS.map((item, idx) => (
                                        <button
                                            key={idx}
                                            onClick={() => handleSendMessage(item.prompt)}
                                            className="px-3 py-2 bg-white hover:bg-indigo-50/80 border border-slate-200/80 hover:border-indigo-200 rounded-xl text-xs text-slate-700 hover:text-indigo-900 transition-all text-left shadow-2xs cursor-pointer flex items-center justify-between group"
                                        >
                                            <span>{item.label}</span>
                                            <span className="text-slate-400 group-hover:text-indigo-600 transition-colors">
                                                →
                                            </span>
                                        </button>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Danh sách tin nhắn */}
                        {messages.map((msg) => (
                            <div
                                key={msg.id}
                                className={`flex gap-2.5 ${msg.role === "user" ? "justify-end" : "justify-start"}`}
                            >
                                {msg.role === "assistant" && (
                                    <div className="w-7 h-7 rounded-xl bg-indigo-600 text-white flex items-center justify-center shrink-0 text-xs shadow-xs mt-0.5">
                                        <Bot className="w-3.5 h-3.5" />
                                    </div>
                                )}

                                <div className={`max-w-[85%] space-y-1.5`}>
                                    <div
                                        className={`px-4 py-3 rounded-2xl text-xs leading-relaxed shadow-2xs ${
                                            msg.role === "user"
                                                ? "bg-indigo-600 text-white rounded-tr-xs whitespace-pre-line"
                                                : "bg-white text-slate-800 border border-slate-200/80 rounded-tl-xs"
                                        }`}
                                    >
                                        {msg.role === "user" ? (
                                            msg.content
                                        ) : !msg.content && isLoading ? (
                                            <div className="flex items-center gap-1.5 py-0.5 text-slate-400">
                                                <span className="w-1.5 h-1.5 rounded-full bg-indigo-500 animate-bounce" />
                                                <span className="w-1.5 h-1.5 rounded-full bg-indigo-500 animate-bounce [animation-delay:0.2s]" />
                                                <span className="w-1.5 h-1.5 rounded-full bg-indigo-500 animate-bounce [animation-delay:0.4s]" />
                                                <span className="text-[11px] text-slate-400 ml-1 font-medium">
                                                    Đang trả lời...
                                                </span>
                                            </div>
                                        ) : (
                                            <FormattedAiMessage content={msg.content} />
                                        )}
                                    </div>

                                    {/* Render Confirmation Card nếu có lệnh chuyển tiền nháp */}
                                    {msg.draftTransfer && msg.draftTransfer.canProceed && (
                                        <DraftTransferCard
                                            payload={msg.draftTransfer}
                                            onConfirm={handleConfirmDraftTransfer}
                                        />
                                    )}

                                    <span
                                        className={`text-[10px] text-slate-400 block px-1 ${
                                            msg.role === "user" ? "text-right" : "text-left"
                                        }`}
                                    >
                                        {new Date(msg.timestamp).toLocaleTimeString([], {
                                            hour: "2-digit",
                                            minute: "2-digit",
                                        })}
                                    </span>
                                </div>

                                {msg.role === "user" && (
                                    <div className="w-7 h-7 rounded-xl bg-slate-200 text-slate-600 flex items-center justify-center shrink-0 text-xs mt-0.5">
                                        <UserIcon className="w-3.5 h-3.5" />
                                    </div>
                                )}
                            </div>
                        ))}

                        <div ref={messagesEndRef} />
                    </div>

                    {/* Input Bar */}
                    <div className="p-3 bg-white border-t border-slate-200/80 flex items-center gap-2">
                        <input
                            ref={inputRef}
                            type="text"
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            onKeyDown={handleKeyDown}
                            placeholder="Hỏi biểu phí, hạn mức hoặc chuyển tiền..."
                            disabled={isLoading}
                            className="flex-1 px-3.5 py-2.5 bg-slate-100 hover:bg-slate-100/80 focus:bg-white text-xs text-slate-900 placeholder:text-slate-400 rounded-xl border border-transparent focus:border-indigo-500 outline-none transition-all disabled:opacity-50"
                        />
                        <button
                            onClick={() => handleSendMessage()}
                            disabled={!input.trim() || isLoading}
                            className="w-9 h-9 rounded-xl bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-200 text-white disabled:text-slate-400 flex items-center justify-center transition-all cursor-pointer disabled:cursor-not-allowed shrink-0"
                            aria-label="Gửi tin nhắn"
                        >
                            {isLoading ? (
                                <Loader2 className="w-4 h-4 animate-spin" />
                            ) : (
                                <Send className="w-4 h-4" />
                            )}
                        </button>
                    </div>
                </div>
            )}

            {/* 3. Modal chuyển tiền an toàn (Human-in-the-loop) kích hoạt từ AI */}
            {walletId && isTransferModalOpen && (
                <TransferModal
                    isOpen={isTransferModalOpen}
                    onClose={() => setIsTransferModalOpen(false)}
                    senderWalletId={walletId}
                    currentBalance={currentBalance}
                    currency={currency}
                    initialRecipient={selectedRecipient}
                    initialAmount={selectedRecipient?.amount}
                    initialDescription={selectedRecipient?.description}
                    onSuccess={handleTransferSuccess}
                />
            )}
        </>
    );
}
