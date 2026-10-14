"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useRouter } from "@/i18n/navigation";
import {
    Search,
    UserPlus,
    Edit2,
    Trash2,
    Send,
    Loader2,
    BookUser,
    CheckCircle2,
    Check,
    Phone,
    Mail,
    X,
    Copy,
    AlertCircle,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import {
    useBeneficiariesQuery,
    useAddBeneficiaryMutation,
    useUpdateBeneficiaryMutation,
    useDeleteBeneficiaryMutation,
    ActionError,
} from "../hooks/use-wallet";
import { lookupRecipientAction } from "../actions/wallet.actions";
import { BeneficiaryDto, RecipientLookupDto } from "../types/wallet.types";

export function BeneficiariesView() {
    const t = useTranslations("wallet");
    const dashT = useTranslations("dashboard");
    const apiErr = useTranslations("apiErrors");
    const common = useTranslations("common");
    const router = useRouter();

    const { data: beneficiaries = [], isLoading, refetch } = useBeneficiariesQuery();
    const addMutation = useAddBeneficiaryMutation();
    const updateMutation = useUpdateBeneficiaryMutation();
    const deleteMutation = useDeleteBeneficiaryMutation();

    const [searchQuery, setSearchQuery] = useState("");
    const [isAddFormOpen, setIsAddFormOpen] = useState(false);
    const [addIdentifier, setAddIdentifier] = useState("");
    const [addNickname, setAddNickname] = useState("");
    const [isLookingUp, setIsLookingUp] = useState(false);
    const [lookupResult, setLookupResult] = useState<RecipientLookupDto | null>(null);
    const [lookupError, setLookupError] = useState<string | null>(null);

    // Editing nickname state
    const [editingId, setEditingId] = useState<string | null>(null);
    const [editNicknameValue, setEditNicknameValue] = useState("");

    // Deleting state
    const [deletingId, setDeletingId] = useState<string | null>(null);

    const filteredBeneficiaries = beneficiaries.filter((b) => {
        const q = searchQuery.toLowerCase().trim();
        if (!q) return true;
        return (
            (b.fullName && b.fullName.toLowerCase().includes(q)) ||
            (b.nickname && b.nickname.toLowerCase().includes(q)) ||
            (b.phoneNumber && b.phoneNumber.includes(q)) ||
            (b.email && b.email.toLowerCase().includes(q)) ||
            (b.beneficiaryWalletId && b.beneficiaryWalletId.toLowerCase().includes(q))
        );
    });

    const resolveErrorMessage = (err: unknown, defaultMessage: string): string => {
        if (err instanceof ActionError && err.errorCode && apiErr.has(err.errorCode as any)) {
            return apiErr(err.errorCode as any);
        }
        if (err instanceof Error && err.message) {
            return err.message;
        }
        return defaultMessage;
    };

    const handleLookup = async () => {
        if (!addIdentifier.trim()) return;
        setIsLookingUp(true);
        setLookupError(null);
        setLookupResult(null);

        try {
            const res = await lookupRecipientAction(addIdentifier.trim());
            if (res.success && res.data) {
                setLookupResult(res.data);
            } else {
                const message =
                    res.errorCode && apiErr.has(res.errorCode as any)
                        ? apiErr(res.errorCode as any)
                        : res.message || t("recipientNotFound");
                setLookupError(message);
            }
        } catch (err: unknown) {
            setLookupError(resolveErrorMessage(err, t("recipientNotFound")));
        } finally {
            setIsLookingUp(false);
        }
    };

    const handleAddBeneficiary = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!lookupResult) {
            await handleLookup();
            return;
        }

        try {
            await addMutation.mutateAsync({
                beneficiaryUserId: lookupResult.userId,
                beneficiaryWalletId: lookupResult.walletId,
                nickname: addNickname.trim() || undefined,
            });
            toast.success(t("beneficiarySaved"));
            setIsAddFormOpen(false);
            setAddIdentifier("");
            setAddNickname("");
            setLookupResult(null);
            refetch();
        } catch (err: unknown) {
            toast.error(resolveErrorMessage(err, t("anErrorOccurredWhileTransferringMoney")));
        }
    };

    const handleStartEdit = (b: BeneficiaryDto) => {
        setEditingId(b.id);
        setEditNicknameValue(b.nickname || "");
    };

    const handleSaveEdit = async (beneficiaryId: string) => {
        try {
            await updateMutation.mutateAsync({
                beneficiaryId,
                nickname: editNicknameValue.trim() || undefined,
            });
            toast.success(t("nicknameUpdated"));
            setEditingId(null);
            refetch();
        } catch (err: unknown) {
            toast.error(resolveErrorMessage(err, t("cannotUpdateNickname")));
        }
    };

    const handleDelete = async (beneficiaryId: string) => {
        setDeletingId(beneficiaryId);
        try {
            await deleteMutation.mutateAsync(beneficiaryId);
            toast.success(t("beneficiaryDeleted"));
            refetch();
        } catch (err: unknown) {
            toast.error(resolveErrorMessage(err, t("cannotDeleteBeneficiary")));
        } finally {
            setDeletingId(null);
        }
    };

    const handleQuickTransfer = (b: BeneficiaryDto) => {
        const identifier = b.phoneNumber || b.email || b.beneficiaryWalletId;
        router.push(`/dashboard/transfer?recipient=${encodeURIComponent(identifier)}`);
    };

    const handleCopy = (text: string) => {
        navigator.clipboard.writeText(text);
        toast.success("Đã sao chép vào bộ nhớ tạm");
    };

    return (
        <main className="flex-1 p-4 sm:p-8 max-w-7xl w-full mx-auto space-y-6">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
                        {dashT("beneficiaries")}
                    </h1>
                    <p className="text-xs sm:text-sm text-slate-500 mt-1">
                        {t("beneficiariesSubtitle")}
                    </p>
                </div>
                <Button
                    onClick={() => setIsAddFormOpen(!isAddFormOpen)}
                    variant="primary"
                    className="flex items-center space-x-1.5 cursor-pointer shadow-xs self-start sm:self-auto"
                >
                    <UserPlus className="w-4 h-4" />
                    <span>{t("addNewBeneficiary")}</span>
                </Button>
            </div>

            {/* Add Beneficiary Panel (if open) */}
            {isAddFormOpen && (
                <div className="bg-white rounded-2xl border border-blue-200/90 p-6 shadow-xs animate-in fade-in duration-200">
                    <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-4">
                        <div className="flex items-center space-x-2">
                            <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
                                <UserPlus className="w-4 h-4" />
                            </div>
                            <h2 className="text-sm font-bold text-slate-900">{t("addNewBeneficiary")}</h2>
                        </div>
                        <button
                            onClick={() => {
                                setIsAddFormOpen(false);
                                setLookupResult(null);
                                setLookupError(null);
                            }}
                            className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg cursor-pointer"
                        >
                            <X className="w-4 h-4" />
                        </button>
                    </div>

                    <form onSubmit={handleAddBeneficiary} className="space-y-4 max-w-xl">
                        <div className="space-y-1.5">
                            <Label className="text-xs font-semibold text-slate-700">
                                {t("recipientPhoneOrEmail")}
                            </Label>
                            <div className="flex gap-2">
                                <Input
                                    placeholder={t("recipientPlaceholder")}
                                    value={addIdentifier}
                                    onChange={(e) => {
                                        setAddIdentifier(e.target.value);
                                        setLookupResult(null);
                                        setLookupError(null);
                                    }}
                                    className="text-xs"
                                    required
                                />
                                <Button
                                    type="button"
                                    variant="outline"
                                    onClick={handleLookup}
                                    disabled={isLookingUp || !addIdentifier.trim()}
                                    className="text-xs cursor-pointer shrink-0"
                                >
                                    {isLookingUp ? (
                                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                    ) : (
                                        t("verify")
                                    )}
                                </Button>
                            </div>
                        </div>

                        {lookupError && (
                            <div className="flex items-center space-x-1.5 text-xs text-rose-600 bg-rose-50 p-2.5 rounded-lg border border-rose-200">
                                <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                                <span>{lookupError}</span>
                            </div>
                        )}

                        {lookupResult && (
                            <div className="p-3.5 rounded-xl bg-emerald-50/70 border border-emerald-200/80 flex items-center space-x-3">
                                <div className="w-9 h-9 rounded-full bg-emerald-600 text-white flex items-center justify-center font-bold text-xs">
                                    {lookupResult.fullName ? lookupResult.fullName.slice(0, 2).toUpperCase() : "US"}
                                </div>
                                <div className="min-w-0">
                                    <div className="flex items-center space-x-1">
                                        <p className="text-xs font-bold text-slate-900">{lookupResult.fullName}</p>
                                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                                    </div>
                                    <p className="text-[11px] text-slate-500 font-mono">
                                        Ví: {lookupResult.walletId}
                                    </p>
                                </div>
                            </div>
                        )}

                        <div className="space-y-1.5">
                            <Label className="text-xs font-semibold text-slate-700">
                                {t("nicknameOptional")}
                            </Label>
                            <Input
                                placeholder="Ví dụ: Bạn thân, Tiền nhà..."
                                value={addNickname}
                                onChange={(e) => setAddNickname(e.target.value)}
                                className="text-xs"
                            />
                        </div>

                        <div className="flex gap-2 pt-2">
                            <Button
                                type="submit"
                                variant="primary"
                                disabled={addMutation.isPending || (!lookupResult && !addIdentifier.trim())}
                                className="text-xs cursor-pointer"
                            >
                                {addMutation.isPending ? (
                                    <>
                                        <Loader2 className="w-3.5 h-3.5 animate-spin mr-1.5" />
                                        {t("saving")}
                                    </>
                                ) : (
                                    t("saveBeneficiary")
                                )}
                            </Button>
                            <Button
                                type="button"
                                variant="outline"
                                onClick={() => setIsAddFormOpen(false)}
                                className="text-xs cursor-pointer"
                            >
                                {common("cancel")}
                            </Button>
                        </div>
                    </form>
                </div>
            )}

            {/* Search Filter Bar */}
            <div className="bg-white rounded-2xl border border-slate-200/90 p-3.5 shadow-xs flex items-center justify-between gap-4">
                <div className="relative flex-1 max-w-md">
                    <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <Input
                        placeholder={t("searchBeneficiaries")}
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="pl-9 text-xs h-9"
                    />
                    {searchQuery && (
                        <button
                            onClick={() => setSearchQuery("")}
                            className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                        >
                            <X className="w-3.5 h-3.5" />
                        </button>
                    )}
                </div>
                <div className="text-xs font-medium text-slate-500 shrink-0">
                    Tổng số: <span className="font-bold text-slate-800">{filteredBeneficiaries.length}</span> người nhận
                </div>
            </div>

            {/* Beneficiaries Grid */}
            {isLoading ? (
                <div className="py-20 text-center text-xs text-slate-400 flex flex-col items-center justify-center gap-2">
                    <Loader2 className="w-6 h-6 animate-spin text-blue-600" />
                    <span>{t("beneficiariesLoading")}</span>
                </div>
            ) : filteredBeneficiaries.length === 0 ? (
                <div className="py-20 bg-white rounded-2xl border border-slate-200/90 text-center p-8 space-y-3 shadow-xs">
                    <div className="w-14 h-14 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center mx-auto">
                        <BookUser className="w-7 h-7 stroke-[1.5]" />
                    </div>
                    <div>
                        <h3 className="text-sm font-bold text-slate-900">{t("noBeneficiariesYet")}</h3>
                        <p className="text-xs text-slate-500 mt-1 max-w-md mx-auto">
                            {t("beneficiariesEmptyNotice")}
                        </p>
                    </div>
                    <Button
                        onClick={() => setIsAddFormOpen(true)}
                        variant="primary"
                        size="sm"
                        className="cursor-pointer"
                    >
                        <UserPlus className="w-3.5 h-3.5 mr-1.5" />
                        {t("addNewBeneficiary")}
                    </Button>
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {filteredBeneficiaries.map((b) => {
                        const isEditing = editingId === b.id;
                        const isDeleting = deletingId === b.id;

                        return (
                            <div
                                key={b.id}
                                className="bg-white rounded-2xl border border-slate-200/90 p-5 shadow-xs hover:border-blue-200 hover:shadow-sm transition-all flex flex-col justify-between space-y-4"
                            >
                                <div>
                                    {/* Top Row: Avatar & Actions */}
                                    <div className="flex items-start justify-between">
                                        <div className="flex items-center space-x-3">
                                            <div className="w-11 h-11 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold text-sm shadow-2xs">
                                                {b.fullName ? b.fullName.slice(0, 2).toUpperCase() : "US"}
                                            </div>
                                            <div className="min-w-0">
                                                <h3 className="text-xs font-bold text-slate-900 truncate">
                                                    {b.fullName || "Người thụ hưởng"}
                                                </h3>
                                                {/* Nickname */}
                                                {isEditing ? (
                                                    <div className="flex items-center space-x-1 mt-1">
                                                        <Input
                                                            value={editNicknameValue}
                                                            onChange={(e) => setEditNicknameValue(e.target.value)}
                                                            className="text-[11px] h-6 px-1.5 w-28"
                                                            autoFocus
                                                        />
                                                        <button
                                                            onClick={() => handleSaveEdit(b.id)}
                                                            className="p-1 text-emerald-600 hover:bg-emerald-50 rounded cursor-pointer"
                                                        >
                                                            <Check className="w-3 h-3" />
                                                        </button>
                                                        <button
                                                            onClick={() => setEditingId(null)}
                                                            className="p-1 text-slate-400 hover:bg-slate-100 rounded cursor-pointer"
                                                        >
                                                            <X className="w-3 h-3" />
                                                        </button>
                                                    </div>
                                                ) : (
                                                    <div className="flex items-center space-x-1 mt-0.5">
                                                        <span className="text-[11px] text-blue-600 font-medium bg-blue-50/70 px-1.5 py-0.2 rounded">
                                                            {b.nickname || "Chưa đặt biệt danh"}
                                                        </span>
                                                        <button
                                                            onClick={() => handleStartEdit(b)}
                                                            className="text-slate-400 hover:text-slate-600 p-0.5 rounded cursor-pointer"
                                                            title="Sửa biệt danh"
                                                        >
                                                            <Edit2 className="w-2.5 h-2.5" />
                                                        </button>
                                                    </div>
                                                )}
                                            </div>
                                        </div>

                                        {/* Delete Button */}
                                        <button
                                            onClick={() => handleDelete(b.id)}
                                            disabled={isDeleting}
                                            className="text-slate-300 hover:text-rose-600 p-1.5 rounded-lg hover:bg-rose-50 transition-colors cursor-pointer"
                                            title="Xóa khỏi danh bạ"
                                        >
                                            {isDeleting ? (
                                                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                            ) : (
                                                <Trash2 className="w-3.5 h-3.5" />
                                            )}
                                        </button>
                                    </div>

                                    {/* Middle info */}
                                    <div className="mt-4 pt-3 border-t border-slate-100 space-y-1.5 text-xs text-slate-500">
                                        {b.phoneNumber && (
                                            <div className="flex items-center space-x-2">
                                                <Phone className="w-3.5 h-3.5 text-slate-400" />
                                                <span>{b.phoneNumber}</span>
                                            </div>
                                        )}
                                        {b.email && (
                                            <div className="flex items-center space-x-2">
                                                <Mail className="w-3.5 h-3.5 text-slate-400" />
                                                <span className="truncate">{b.email}</span>
                                            </div>
                                        )}
                                        <div className="flex items-center justify-between text-[11px] text-slate-400 font-mono bg-slate-50 px-2 py-1 rounded-md">
                                            <span className="truncate">Ví: {b.beneficiaryWalletId}</span>
                                            <button
                                                onClick={() => handleCopy(b.beneficiaryWalletId)}
                                                className="text-slate-400 hover:text-slate-600 ml-1 p-0.5 cursor-pointer"
                                                title="Sao chép mã ví"
                                            >
                                                <Copy className="w-3 h-3" />
                                            </button>
                                        </div>
                                    </div>
                                </div>

                                {/* Bottom Quick Transfer Button */}
                                <Button
                                    variant="outline"
                                    onClick={() => handleQuickTransfer(b)}
                                    className="w-full h-8.5 text-xs rounded-xl flex items-center justify-center space-x-1.5 hover:bg-blue-50 hover:text-blue-600 hover:border-blue-200 transition-colors cursor-pointer"
                                >
                                    <Send className="w-3.5 h-3.5" />
                                    <span>{t("transfer")}</span>
                                </Button>
                            </div>
                        );
                    })}
                </div>
            )}
        </main>
    );
}
