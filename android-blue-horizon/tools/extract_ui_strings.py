"""List Android UI literals or preview a patch for a reviewed selection."""
import argparse
import difflib
import json
from pathlib import Path
import re
import sys
from typing import Any
import xml.etree.ElementTree as ET

TOKEN = re.compile(
    r'(?P<comment>//[^\r\n]*|/\*[\s\S]*?\*/)|"""[\s\S]*?"""|'
    r'"(?:\\.|[^"\\\r\n])*"|\'(?:\\.|[^\'\\\r\n])*\'|[A-Za-z_$][\w$]*|[^\s]'
)
SETTERS = {
    "setTitle", "setMessage", "setText", "setHint", "setContentDescription",
    "setPositiveButton", "setNeutralButton", "setNegativeButton",
}
RECEIVERS = {"MainActivity": "", "TradeDialog": "activity."}
ROOT = Path(__file__).resolve().parents[1]


def candidates(source: str) -> list[dict[str, Any]]:
    if re.search(r"\\u+[0-9a-fA-F]{4}", source):
        raise ValueError("Review Java Unicode escapes manually")
    tokens = [token for token in TOKEN.finditer(source) if not token.group("comment")]
    found = []
    for index in range(3, len(tokens) - 1):
        literal = tokens[index]
        dot, method, opening = (part.group() for part in tokens[index - 3:index])
        value = literal.group()
        if is_candidate(dot, method, opening, value, tokens[index + 1].group()):
            found.append({"line": source.count("\n", 0, literal.start()) + 1,
                          "literal": value, "method": method,
                          "start": literal.start(), "end": literal.end()})
    return found


def is_candidate(dot: str, method: str, opening: str, value: str, following: str) -> bool:
    return (dot == "." and method in SETTERS and opening == "(" and following in (")", ",")
            and value.startswith('"') and not value.startswith('"""')
            and bool(re.search("[\uac00-\ud7a3]", value)))


def convert(source: str, selected: dict[str, str], receiver: str) -> tuple[str, dict[str, str]]:
    matches = [entry for entry in candidates(source) if entry["literal"] in selected]
    if set(selected) != {entry["literal"] for entry in matches}:
        raise ValueError("Selection includes a stale or unsupported literal")
    additions = {}
    for entry in reversed(matches):
        literal = entry["literal"]
        key = selected[literal]
        if not re.fullmatch("[a-z][a-z0-9_]*", key):
            raise ValueError(f"Invalid resource key: {key}")
        value = json.loads(literal)
        if key in additions and additions[key] != value:
            raise ValueError(f"Conflicting resource key: {key}")
        additions[key] = value
        replacement = f"{receiver}getString(R.string.{key})"
        source = source[:entry["start"]] + replacement + source[entry["end"]:]
    return source, additions


def android_value(value: str) -> str:
    escaped = value.replace("\\", "\\\\").replace('"', '\\"')
    escaped = escaped.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
    return '"' + escaped + '"'


def merge_strings(source: str, additions: dict[str, str]) -> str:
    root = ET.fromstring(source, parser=ET.XMLParser(target=ET.TreeBuilder(insert_comments=True)))
    existing = {node.get("name"): node.text for node in root.findall("string")}
    for key, value in sorted(additions.items()):
        escaped = android_value(value)
        if key in existing:
            if existing[key] != escaped:
                raise ValueError(f"Resource already exists with another value: {key}")
            continue
        node = ET.SubElement(root, "string", name=key, formatted="false")
        node.text = escaped
    ET.indent(root, space="    ")
    return ET.tostring(root, encoding="utf-8", xml_declaration=True).decode("utf-8") + "\n"


def preview(path: str, before: str, after: str) -> str:
    return "".join(difflib.unified_diff(before.splitlines(True), after.splitlines(True),
                                        fromfile=f"a/{path}", tofile=f"b/{path}"))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("screen", choices=RECEIVERS)
    parser.add_argument("--selection", type=Path, help="JSON object mapping reviewed Java literals to resource keys")
    parser.add_argument("--output", type=Path, help="Write the candidate list or patch as UTF-8")
    args = parser.parse_args()
    path = f"src/com/bluehorizon/{args.screen}.java"
    source = (ROOT / path).read_text(encoding="utf-8")
    if args.selection is None:
        output_text(json.dumps(candidates(source), ensure_ascii=False, indent=2) + "\n", args.output)
        return
    selection = json.loads(args.selection.read_text(encoding="utf-8"))
    updated, additions = convert(source, selection, RECEIVERS[args.screen])
    strings_path = "res/values/strings.xml"
    strings = (ROOT / strings_path).read_text(encoding="utf-8")
    patch = preview(path, source, updated) + preview(strings_path, strings, merge_strings(strings, additions))
    output_text(patch, args.output)


def output_text(value: str, destination: Path | None) -> None:
    if destination is not None:
        destination.write_text(value, encoding="utf-8", newline="\n")
        return
    sys.stdout.buffer.write(value.encode("utf-8"))


if __name__ == "__main__":
    main()
