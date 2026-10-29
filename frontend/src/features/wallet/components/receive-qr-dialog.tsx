"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import Image from "next/image";
import { Button } from "@/components/ui/button";
import { getReceiveQrAction } from "../actions/receive-qr.actions";
import { isReceiveUuid } from "../receive-qr";

export function ReceiveQrDialog({ onClose }: { onClose: () => void }) {
    const t = useTranslations("receiveQr");
    const dialog = useRef<HTMLDialogElement>(null);
    const closeButton = useRef<HTMLButtonElement>(null);
    const [attempt, setAttempt] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [imageUrl, setImageUrl] = useState("");
    const [receiveUrl, setReceiveUrl] = useState("");
    const [notice, setNotice] = useState("");

    useEffect(() => {
        const previous =
            document.activeElement instanceof HTMLElement ? document.activeElement : null;
        const element = dialog.current;
        element?.showModal();
        closeButton.current?.focus();

        return () => {
            element?.close();
            previous?.focus();
        };
    }, []);

    useEffect(() => {
        let active = true;
        let objectUrl = "";
        const controller = new AbortController();

        async function load() {
            try {
                const result = await getReceiveQrAction();
                if (!active) return;
                if (!result.success || !result.data) throw Error(result.message || t("failed"));
                if (!isReceiveUuid(result.data.mediaId)) throw Error(t("failed"));

                // Never use the upstream contentUrl: the browser only talks to our authenticated BFF.
                const response = await fetch(`/api/media/${result.data.mediaId}/content`, {
                    cache: "no-store",
                    credentials: "same-origin",
                    signal: controller.signal,
                });
                if (!response.ok || response.headers.get("Content-Type") !== "image/png") {
                    throw Error(t("imageFailed"));
                }

                const blob = await response.blob();
                if (!active) return;
                objectUrl = URL.createObjectURL(blob);
                setImageUrl(objectUrl);
                setReceiveUrl(result.data.receiveUrl);
            } catch (failure) {
                if (active) setError(failure instanceof Error ? failure.message : t("failed"));
            } finally {
                if (active) setLoading(false);
            }
        }

        void load();

        return () => {
            active = false;
            controller.abort();
            if (objectUrl) URL.revokeObjectURL(objectUrl);
        };
    }, [attempt, t]);

    function retry() {
        setLoading(true);
        setError("");
        setNotice("");
        setImageUrl("");
        setReceiveUrl("");
        setAttempt((value) => value + 1);
    }

    async function copyLink() {
        try {
            await navigator.clipboard.writeText(receiveUrl);
            setNotice(t("copied"));
        } catch {
            setNotice(t("copyFailed"));
        }
    }

    return (
        <dialog
            ref={dialog}
            aria-labelledby="receive-qr-title"
            aria-describedby="receive-qr-description"
            onCancel={(event) => {
                event.preventDefault();
                onClose();
            }}
            className="m-auto w-[calc(100%-2rem)] max-w-md max-h-[90dvh] overflow-y-auto rounded-2xl border border-slate-200 bg-white p-6 text-slate-900 backdrop:bg-slate-900/40"
        >
            <div className="flex items-center justify-between gap-4">
                <h2 id="receive-qr-title" className="text-lg font-semibold">
                    {t("title")}
                </h2>
                <Button ref={closeButton} variant="ghost" onClick={onClose}>
                    {t("close")}
                </Button>
            </div>
            <p id="receive-qr-description" className="mt-2 text-sm text-slate-600">
                {t("description")}
            </p>

            <div className="my-4" aria-busy={loading}>
                {loading && <p role="status">{t("loading")}</p>}
                {error && (
                    <p role="alert" className="text-sm text-rose-700">
                        {error}
                    </p>
                )}
                {imageUrl && !error && (
                    <Image
                        src={imageUrl}
                        alt={t("imageAlt")}
                        width={360}
                        height={360}
                        unoptimized
                        className="mx-auto h-auto max-w-full"
                        onError={() => setError(t("imageFailed"))}
                    />
                )}
            </div>

            {error && (
                <Button variant="outline" onClick={retry}>
                    {t("retry")}
                </Button>
            )}
            {!loading && !error && imageUrl && (
                <div className="flex flex-wrap gap-3">
                    <a
                        href={imageUrl}
                        download="receive-qr.png"
                        className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium focus-visible:outline-blue-600"
                    >
                        {t("download")}
                    </a>
                    <Button variant="outline" onClick={copyLink}>
                        {t("copyLink")}
                    </Button>
                </div>
            )}
            <p role="status" className="mt-3 text-sm text-slate-600">
                {notice}
            </p>
        </dialog>
    );
}
