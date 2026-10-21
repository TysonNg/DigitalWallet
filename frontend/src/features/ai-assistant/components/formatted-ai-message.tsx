"use client";

import React from "react";

interface FormattedAiMessageProps {
    content: string;
}

export function FormattedAiMessage({ content }: FormattedAiMessageProps) {
    if (!content) return null;

    // Phân tách các khối văn bản theo dòng
    const lines = content.split("\n");
    const elements: React.ReactNode[] = [];
    let currentTableRows: string[][] = [];
    let inTable = false;

    const flushTable = (keyPrefix: number) => {
        if (currentTableRows.length > 0) {
            const [header, ...rows] = currentTableRows;
            const validRows = rows.filter((r) => !r.every((c) => /^-+$/.test(c.trim())));

            elements.push(
                <div key={`table-${keyPrefix}`} className="my-2.5 overflow-x-auto rounded-xl border border-slate-200">
                    <table className="w-full text-left text-xs border-collapse">
                        {header && (
                            <thead className="bg-slate-100/90 text-slate-800 font-semibold border-b border-slate-200">
                                <tr>
                                    {header.map((col, idx) => (
                                        <th key={idx} className="px-3 py-1.5">
                                            {formatInline(col.trim())}
                                        </th>
                                    ))}
                                </tr>
                            </thead>
                        )}
                        <tbody className="divide-y divide-slate-100 bg-white">
                            {validRows.map((row, rIdx) => (
                                <tr key={rIdx} className="hover:bg-slate-50/70 transition-colors">
                                    {row.map((cell, cIdx) => (
                                        <td key={cIdx} className="px-3 py-1.5 text-slate-700">
                                            {formatInline(cell.trim())}
                                        </td>
                                    ))}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            );
            currentTableRows = [];
            inTable = false;
        }
    };

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        const trimmed = line.trim();

        // Kiểm tra dòng bảng Markdown (| col1 | col2 |)
        if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            inTable = true;
            const cells = trimmed
                .slice(1, -1)
                .split("|")
                .map((c) => c.trim());
            currentTableRows.push(cells);
            continue;
        } else if (inTable) {
            flushTable(i);
        }

        // Dòng phân cách ---
        if (/^---+$/.test(trimmed)) {
            elements.push(<hr key={i} className="my-2 border-slate-200" />);
            continue;
        }

        // Tiêu đề ### hoặc ##
        if (trimmed.startsWith("### ")) {
            elements.push(
                <h4 key={i} className="font-bold text-slate-900 text-xs mt-2 mb-1">
                    {formatInline(trimmed.slice(4))}
                </h4>
            );
            continue;
        }
        if (trimmed.startsWith("## ")) {
            elements.push(
                <h3 key={i} className="font-bold text-slate-900 text-sm mt-2.5 mb-1 text-indigo-950">
                    {formatInline(trimmed.slice(3))}
                </h3>
            );
            continue;
        }

        // Danh sách gạch đầu dòng (- hoặc *)
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            elements.push(
                <div key={i} className="flex items-start gap-1.5 my-0.5 ml-1">
                    <span className="w-1.5 h-1.5 rounded-full bg-indigo-500 mt-1.5 shrink-0" />
                    <span className="text-slate-800 leading-relaxed">{formatInline(trimmed.slice(2))}</span>
                </div>
            );
            continue;
        }

        // Danh sách số (1. , 2. )
        const numMatch = trimmed.match(/^(\d+)\.\s+(.+)$/);
        if (numMatch) {
            elements.push(
                <div key={i} className="flex items-start gap-1.5 my-0.5 ml-1">
                    <span className="font-bold text-indigo-600 text-[11px] shrink-0">{numMatch[1]}.</span>
                    <span className="text-slate-800 leading-relaxed">{formatInline(numMatch[2])}</span>
                </div>
            );
            continue;
        }

        // Dòng trống
        if (!trimmed) {
            elements.push(<div key={i} className="h-1.5" />);
            continue;
        }

        // Đoạn văn thông thường
        elements.push(
            <p key={i} className="text-slate-800 leading-relaxed my-0.5">
                {formatInline(trimmed)}
            </p>
        );
    }

    if (inTable) {
        flushTable(lines.length);
    }

    return <div className="text-xs leading-relaxed space-y-0.5">{elements}</div>;
}

/**
 * Xử lý các định dạng inline: in đậm (**text**), inline code (`code`), và italic (*text*)
 */
function formatInline(text: string): React.ReactNode[] {
    const parts: React.ReactNode[] = [];
    // Regex cho: **bold** | `code`
    const regex = /(\*\*[^*]+\*\*|`[^`]+`)/g;
    let lastIndex = 0;
    let match: RegExpExecArray | null;

    while ((match = regex.exec(text)) !== null) {
        if (match.index > lastIndex) {
            parts.push(text.slice(lastIndex, match.index));
        }

        const raw = match[0];
        if (raw.startsWith("**") && raw.endsWith("**")) {
            parts.push(
                <strong key={match.index} className="font-semibold text-slate-900">
                    {raw.slice(2, -2)}
                </strong>
            );
        } else if (raw.startsWith("`") && raw.endsWith("`")) {
            parts.push(
                <code key={match.index} className="px-1 py-0.5 bg-slate-100 text-indigo-700 rounded font-mono text-[11px]">
                    {raw.slice(1, -1)}
                </code>
            );
        }
        lastIndex = regex.lastIndex;
    }

    if (lastIndex < text.length) {
        parts.push(text.slice(lastIndex));
    }

    return parts;
}
