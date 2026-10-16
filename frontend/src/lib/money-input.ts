export function formatMoneyInput(value: number, locale: string): string {
    return value ? new Intl.NumberFormat(locale, { maximumFractionDigits: 0 }).format(value) : "";
}

export function parseMoneyInput(value: string): number | null {
    value = value.trim();
    if (value && !/^\d+$/.test(value) && !/^\d{1,3}([.,\s])\d{3}(?:\1\d{3})*$/.test(value))
        return null;

    const amount = Number(value.replace(/[.,\s]/g, ""));

    return Number.isSafeInteger(amount) && amount >= 0 ? amount : null;
}
