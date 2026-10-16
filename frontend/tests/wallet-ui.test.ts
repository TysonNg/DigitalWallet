import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";

function files(dir: string): string[] {
    return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
        const path = join(dir, entry.name);
        return entry.isDirectory() ? files(path) : /\.(tsx?|json)$/.test(path) ? [path] : [];
    });
}

test("literal UI translation keys exist in both languages", () => {
    for (const locale of ["vi", "en"]) {
        const messages = JSON.parse(readFileSync("messages/" + locale + ".json", "utf8"));
        const missing = new Set<string>();
        for (const path of files("src")) {
            const source = readFileSync(path, "utf8");
            for (const [, name, namespace] of source.matchAll(
                /(\w+)\s*=\s*useTranslations\("([^"]+)"\)/g
            )) {
                for (const [, key] of source.matchAll(
                    new RegExp(String.raw`\b` + name + String.raw`\("([^"]+)"`, "g")
                )) {
                    const value = key
                        .split(".")
                        .reduce((entry, part) => entry?.[part], messages[namespace]);
                    if (value === undefined) missing.add(namespace + "." + key);
                }
            }
        }
        assert.deepEqual([...missing].sort(), [], locale + " has missing UI labels");
    }
});
test("Vietnamese messages retain accents and contain no replacement characters", () => {
    for (const path of [
        ...files("messages/vi").filter((path) => path.endsWith(".json")),
        "messages/vi.json",
    ]) {
        const source = readFileSync(path, "utf8");
        assert.doesNotMatch(
            source,
            /\uFFFD|\?(?=[\p{L}\p{M}])|[\p{L}\p{M}]\?(?=[\p{L}\p{M}])/u,
            path
        );
    }
    for (const path of ["messages/vi/wallet.json", "messages/vi.json"]) {
        const { wallet } = JSON.parse(readFileSync(path, "utf8"));
        assert.equal(wallet.amount, "S\u1ed1 ti\u1ec1n");
        assert.equal(wallet.currentBalance, "S\u1ed1 d\u01b0 hi\u1ec7n t\u1ea1i");
    }
});
