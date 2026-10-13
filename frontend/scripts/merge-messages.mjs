import fs from "node:fs";
for (const locale of ["vi", "en"]) {
    const messages = {};
    for (const name of fs
        .readdirSync("messages/" + locale)
        .filter((n) => n.endsWith(".json"))
        .sort()) {
        const data = JSON.parse(fs.readFileSync("messages/" + locale + "/" + name, "utf8"));
        for (const key of Object.keys(data)) {
            if (key in messages) throw new Error("Duplicate namespace " + key);
            messages[key] = data[key];
        }
    }
    fs.writeFileSync("messages/" + locale + ".json", JSON.stringify(messages, null, 4) + "\n");
}
function keys(value, prefix = "") {
    return Object.entries(value)
        .flatMap(([key, entry]) =>
            typeof entry === "object" ? keys(entry, prefix + key + ".") : [prefix + key]
        )
        .sort();
}
const vi = JSON.parse(fs.readFileSync("messages/vi.json", "utf8"));
const en = JSON.parse(fs.readFileSync("messages/en.json", "utf8"));
if (JSON.stringify(keys(vi)) !== JSON.stringify(keys(en)))
    throw new Error("Translation key mismatch");
console.log("Translation keys match");
