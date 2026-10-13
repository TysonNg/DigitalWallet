"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
    X,
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
    User as UserIcon,
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

interface BeneficiariesModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSelectBeneficiary?: (beneficiary: BeneficiaryDto) => void;
}

export function BeneficiariesModal({
    isOpen,
    onClose,
    onSelectBeneficiary,
}: BeneficiariesModalProps) {
    const t = useTranslations("wallet");
    const apiErr = useTranslations("apiErrors");
    const common = useTranslations("common");

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

    if (!isOpen) return null;

    const filteredBeneficiaries = beneficiaries.filter((b) => {
        const q = searchQuery.toLowerCase().trim();
        if (!q) return true;
        return (
            (b.fullName && b.fullName.toLowerCase().includes(q)) ||
            (b.nickname && b.nickname.toLowerCase().includes(q)) ||
            (b.phoneNumber && b.phoneNumber.includes(q)) ||
            (b.email && b.email.toLowerCase().includes(q))
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
            toast.success(t("beneficiarySaved"));
            setEditingId(null);
            refetch();
        } catch (err: unknown) {
            toast.error(resolveErrorMessage(err, t("walletErrors.unableToConnectToTheServer")));
        }
    };

    const handleDelete = async (beneficiaryId: string) => {
        try {
            await deleteMutation.mutateAsync(beneficiaryId);
            toast.success(t("beneficiaryDeleted"));
            setDeletingId(null);
            refetch();
        } catch (err: unknown) {
            toast.error(resolveErrorMessage(err, t("walletErrors.unableToConnectToTheServer")));
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto">
            <div
                className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs transition-opacity duration-150"
                aria-label={common("close")}
                onClick={onClose}
            />

            <div className="relative w-full max-w-lg bg-white rounded-2xl shadow-xl border border-slate-200/90 p-5 sm:p-7 z-10 duration-150 my-auto max-h-[90vh] flex flex-col">
                {/* Header */}
                <div className="flex items-center justify-between pb-4 border-b border-slate-100">
                    <div className="flex items-center space-x-3">
                        <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
                            <BookUser className="w-5 h-5 stroke-[2]" />
                        </div>
                        <div>
                            <h3 className="text-base font-bold text-slate-900 tracking-tight">
                                {t("beneficiariesTitle")}
                            </h3>
                            <p className="text-xs text-slate-500">
                                {t("beneficiariesSubtitle")}
                            </p>
                        </div>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
                    >
                        <X className="w-4 h-4 stroke-[1.75]" />
                    </button>
                </div>

                {/* Sub-header: Search & Add Button */}
                <div className="mt-4 flex items-center gap-2">
                    <div className="relative flex-1">
                        <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                        <Input
                            placeholder={t("searchBeneficiaries")}
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="pl-9 text-xs h-9"
                        />
                    </div>
                    <Button
                        size="sm"
                        variant={isAddFormOpen ? "outline" : "primary"}
                        onClick={() => {
                            setIsAddFormOpen(!isAddFormOpen);
                            setLookupResult(null);
                            setLookupError(null);
                        }}
                        className="h-9 text-xs px-3 shrink-0 flex items-center gap-1.5"
                    >
                        <UserPlus className="w-3.5 h-3.5" />
                        <span>{isAddFormOpen ? common("cancel") : t("addBeneficiary")}</span>
                    </Button>
                </div>

                {/* Inline Add Beneficiary Form */}
                {isAddFormOpen && (
                    <form
                        onSubmit={handleAddBeneficiary}
                        className="mt-3.5 p-4 rounded-xl bg-slate-50 border border-slate-200/80 space-y-3 transition-all"
                    >
                        <div className="text-xs font-semibold text-slate-800">
                            {t("addBeneficiaryModalTitle")}
                        </div>

                        <div>
                            <Label htmlFor="add-identifier" className="text-xs">
                                {t("recipientIdentifier")}
                            </Label>
                            <div className="flex gap-2 mt-1">
                                <Input
                                    id="add-identifier"
                                    placeholder={t("searchRecipientPlaceholder")}
                                    value={addIdentifier}
                                    onChange={(e) => {
                                        setAddIdentifier(e.target.value);
                                        setLookupResult(null);
                                        setLookupError(null);
                                    }}
                                    className="text-xs h-8.5 font-medium"
                                    required
                                />
                                <Button
                                    type="button"
                                    size="sm"
                                    variant="outline"
                                    onClick={handleLookup}
                                    disabled={!addIdentifier.trim() || isLookingUp}
                                    className="h-8.5 text-xs px-3 shrink-0"
                                >
                                    {isLookingUp ? (
                                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                    ) : (
                                        t("lookup")
                                    )}
                                </Button>
                            </div>
                        </div>

                        {lookupError && (
                            <div className="p-2 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs">
                                {lookupError}
                            </div>
                        )}

                        {lookupResult && (
                            <div className="p-2.5 rounded-lg bg-emerald-50 border border-emerald-200 text-xs flex items-center justify-between">
                                <div className="space-y-0.5">
                                    <div className="font-bold text-slate-900 uppercase flex items-center gap-1.5">
                                        <span>{lookupResult.fullName}</span>
                                        <span className="inline-flex items-center gap-0.5 text-[10px] text-emerald-700 font-semibold bg-emerald-100 px-1.5 py-0.5 rounded-full">
                                            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                                            {t("verifiedRecipient")}
                                        </span>
                                    </div>
                                    <div className="text-slate-500 text-[11px]">
                                        {lookupResult.phoneNumber || lookupResult.email}
                                    </div>
                                </div>
                            </div>
                        )}

                        {lookupResult && (
                            <div>
                                <Label htmlFor="add-nickname" className="text-xs">
                                    {t("nicknameOptional")}
                                </Label>
                                <Input
                                    id="add-nickname"
                                    placeholder={t("nicknamePlaceholder")}
                                    value={addNickname}
                                    onChange={(e) => setAddNickname(e.target.value)}
                                    className="text-xs h-8.5 mt-1"
                                />
                            </div>
                        )}

                        {lookupResult && (
                            <div className="flex justify-end gap-2 pt-1">
                                <Button
                                    type="submit"
                                    size="sm"
                                    variant="primary"
                                    isLoading={addMutation.isPending}
                                    className="h-8 text-xs"
                                >
                                    {t("save")}
                                </Button>
                            </div>
                        )}
                    </form>
                )}

                {/* Beneficiaries List */}
                <div className="mt-4 flex-1 overflow-y-auto space-y-2 pr-1 min-h-[220px]">
                    {isLoading ? (
                        <div className="h-40 flex items-center justify-center text-xs text-slate-400">
                            <Loader2 className="w-5 h-5 animate-spin mr-2 text-blue-600" />
                            {t("beneficiariesLoading")}
                        </div>
                    ) : filteredBeneficiaries.length === 0 ? (
                        <div className="h-40 flex flex-col items-center justify-center text-center p-4 rounded-xl border border-dashed border-slate-200">
                            <UserIcon className="w-8 h-8 text-slate-300 mb-1.5" />
                            <p className="text-xs text-slate-600 font-medium">
                                {t("noBeneficiariesYet")}
                            </p>
                            <p className="text-[11px] text-slate-400 mt-0.5">
                                {t("beneficiariesEmptyNotice")}
                            </p>
                        </div>
                    ) : (
                        filteredBeneficiaries.map((b) => {
                            const isEditing = editingId === b.id;
                            const isDeleting = deletingId === b.id;

                            return (
                                <div
                                    key={b.id}
                                    className="p-3 rounded-xl border border-slate-100 hover:border-slate-200 bg-white hover:bg-slate-50/70 transition-colors flex items-center justify-between gap-3 group"
                                >
                                    {/* Avatar & Information */}
                                    <div className="flex items-center gap-3 min-w-0 flex-1">
                                        <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-500 text-white font-bold text-xs flex items-center justify-center uppercase shrink-0 shadow-xs">
                                            {(b.fullName || b.nickname || "?")
                                                .charAt(0)
                                                .toUpperCase()}
                                        </div>

                                        <div className="min-w-0 flex-1">
                                            {isEditing ? (
                                                <div className="flex items-center gap-1.5">
                                                    <Input
                                                        value={editNicknameValue}
                                                        onChange={(e) =>
                                                             setEditNicknameValue(e.target.value)
                                                        }
                                                        placeholder={t("nicknamePlaceholder")}
                                                        className="h-7 text-xs py-0 px-2 font-medium"
                                                        autoFocus
                                                    />
                                                    <button
                                                        onClick={() => handleSaveEdit(b.id)}
                                                        className="p-1 rounded bg-blue-600 text-white hover:bg-blue-700 cursor-pointer"
                                                        title={common("save")}
                                                    >
                                                        <Check className="w-3.5 h-3.5" />
                                                    </button>
                                                    <button
                                                        onClick={() => setEditingId(null)}
                                                        className="p-1 rounded bg-slate-200 text-slate-600 hover:bg-slate-300 cursor-pointer"
                                                        title={common("cancel")}
                                                    >
                                                        <X className="w-3.5 h-3.5" />
                                                    </button>
                                                </div>
                                            ) : (
                                                <div className="flex items-center gap-1.5">
                                                    <span className="text-xs font-bold text-slate-900 truncate">
                                                        {b.nickname || b.fullName}
                                                    </span>
                                                    {b.nickname && (
                                                        <span className="text-[11px] text-slate-400 truncate">
                                                            ({b.fullName})
                                                        </span>
                                                    )}
                                                </div>
                                            )}

                                            <div className="flex items-center gap-2 mt-0.5 text-[11px] text-slate-500">
                                                {b.phoneNumber && (
                                                    <span className="flex items-center gap-1">
                                                        <Phone className="w-2.5 h-2.5 text-slate-400" />
                                                        {b.phoneNumber}
                                                    </span>
                                                )}
                                                {b.email && (
                                                    <span className="flex items-center gap-1 truncate">
                                                        <Mail className="w-2.5 h-2.5 text-slate-400" />
                                                        {b.email}
                                                    </span>
                                                )}
                                            </div>
                                        </div>
                                    </div>

                                    {/* Actions */}
                                    <div className="flex items-center gap-1.5 shrink-0">
                                        {isDeleting ? (
                                            <div className="flex items-center gap-1">
                                                <span className="text-[10px] text-rose-600 font-medium">
                                                    {t("confirmDelete")}
                                                </span>
                                                <button
                                                    onClick={() => handleDelete(b.id)}
                                                    className="px-2 py-0.5 rounded text-[10px] font-semibold bg-rose-600 text-white hover:bg-rose-700 cursor-pointer"
                                                >
                                                    {common("yes")}
                                                </button>
                                                <button
                                                    onClick={() => setDeletingId(null)}
                                                    className="px-1.5 py-0.5 rounded text-[10px] bg-slate-100 text-slate-600 hover:bg-slate-200 cursor-pointer"
                                                >
                                                    {common("no")}
                                                </button>
                                            </div>
                                        ) : (
                                            <>
                                                {/* Edit nickname button */}
                                                {!isEditing && (
                                                    <button
                                                        onClick={() => handleStartEdit(b)}
                                                        className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors cursor-pointer opacity-70 group-hover:opacity-100"
                                                        title={t("editNickname")}
                                                    >
                                                        <Edit2 className="w-3.5 h-3.5" />
                                                    </button>
                                                )}

                                                {/* Delete button */}
                                                <button
                                                    onClick={() => setDeletingId(b.id)}
                                                    className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer opacity-70 group-hover:opacity-100"
                                                    title={t("deleteBeneficiary")}
                                                >
                                                    <Trash2 className="w-3.5 h-3.5" />
                                                </button>

                                                {/* Quick transfer button */}
                                                {onSelectBeneficiary && (
                                                    <Button
                                                        size="sm"
                                                        variant="primary"
                                                        onClick={() => onSelectBeneficiary(b)}
                                                        className="h-7 text-xs px-2.5 font-medium flex items-center gap-1"
                                                    >
                                                        <Send className="w-3 h-3" />
                                                        <span>{t("quickTransfer")}</span>
                                                    </Button>
                                                )}
                                            </>
                                        )}
                                    </div>
                                </div>
                            );
                        })
                    )}
                </div>
            </div>
        </div>
    );
}
