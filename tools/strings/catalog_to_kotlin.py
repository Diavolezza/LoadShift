"""One-off migration: writes the German translations of the old string catalog (LoadShift/Localizable.xcstrings)
as a Kotlin map into composeApp/.../Strings.kt. From then on Strings.kt is maintained by hand.

    python3 tools/strings/catalog_to_kotlin.py
"""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[2]
catalog = json.loads((root / "LoadShift/Localizable.xcstrings").read_text(encoding="utf-8"))


def literal(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


lines = [
    "package de.wannerit.loadshift.app",
    "",
    "/**",
    " * German texts of the app, keyed by the English text (the English text is shown as it is). Placeholders as in",
    " * the old string catalog: %@ for text, %lld for whole numbers, positional %1\\$@ … in the translation; a",
    " * literal % in a text with placeholders is written %%.",
    " */",
    "internal val GERMAN: Map<String, String> = mapOf(",
]
for key, entry in sorted(catalog["strings"].items(), key=lambda kv: kv[0].lower()):
    de = entry.get("localizations", {}).get("de", {}).get("stringUnit", {}).get("value")
    if de is None or de == key:
        continue
    lines.append(f"    {literal(key)} to\n        {literal(de)},")
lines.append(")")
lines.append("")
out = root / "composeApp/src/commonMain/kotlin/de/wannerit/loadshift/app/Strings.kt"
out.write_text("\n".join(lines), encoding="utf-8")
print(f"written {out.relative_to(root)}: {len(lines)} lines")
