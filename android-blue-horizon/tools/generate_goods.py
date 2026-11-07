"""Validate the goods data and compile it for Android and desktop JVM tests."""
import json
from pathlib import Path
from typing import Any
import xml.etree.ElementTree as ET

LEGACY_IDS = ("wood", "cloth", "ceramics", "spices", "tea", "silver")
PORT_COUNT = 56


def validate_slots(goods: Any) -> None:
    expected = {good: slot for slot, good in enumerate(LEGACY_IDS)}
    if not isinstance(goods, list) or len(goods) != len(expected):
        raise ValueError("Expected the six legacy goods")
    actual = {}
    for good in goods:
        if not isinstance(good, dict):
            raise ValueError("Expected a good object")
        good_id, slot = good.get("id"), good.get("legacySlot")
        if not isinstance(good_id, str) or type(slot) is not int or good_id in actual:
            raise ValueError("Invalid or duplicate good ID or slot")
        actual[good_id] = slot
    if actual != expected:
        raise ValueError("Legacy goods IDs and slots must match the save format")


def validated_prices(data: Any) -> list[list[int]]:
    if not isinstance(data, dict) or type(data.get("schema")) is not int or data["schema"] != 1:
        raise ValueError("Unsupported goods schema")
    validate_slots(data.get("goods"))
    ports = data.get("ports")
    if not isinstance(ports, list) or len(ports) != PORT_COUNT:
        raise ValueError("Expected 56 port price rows")
    return [price_row(index, row) for index, row in enumerate(ports)]


def price_row(index: int, row: Any) -> list[int]:
    if not isinstance(row, dict) or set(row) != set(LEGACY_IDS):
        raise ValueError(f"Port {index} needs exactly the six known goods")
    prices = [row[good] for good in LEGACY_IDS]
    if any(type(price) is not int or not 1 <= price <= 10000 for price in prices):
        raise ValueError(f"Port {index} has an invalid price")
    return prices


def unique_fields(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    values = {}
    for key, value in pairs:
        if key in values:
            raise ValueError(f"Duplicate JSON field: {key}")
        values[key] = value
    return values


def localized_values(path: Path) -> tuple[list[str], list[str]]:
    strings = {}
    for node in ET.parse(path).getroot().findall("string"):
        key = node.get("name")
        if key in strings:
            raise ValueError(f"Duplicate string: {key}")
        strings[key] = node.text
    return text_values(strings, "name"), text_values(strings, "description")


def text_values(strings: dict[str | None, str | None], suffix: str) -> list[str]:
    values = []
    for good in LEGACY_IDS:
        key = f"good_{good}_{suffix}"
        value = strings.get(key)
        if not value or not value.strip():
            raise ValueError(f"Missing goods string: {key}")
        values.append(value)
    return values


def java_list(values: list[str] | list[int] | tuple[str, ...]) -> str:
    return "{" + ", ".join(json.dumps(value, ensure_ascii=False) for value in values) + "}"


def generate(project: Path, output: Path) -> Path:
    source = (project / "data/goods.json").read_text(encoding="utf-8")
    prices = validated_prices(json.loads(source, object_pairs_hook=unique_fields))
    names, descriptions = localized_values(project / "res/values/strings.xml")
    rows = ",\n        ".join(java_list(row) for row in prices)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(f"""// Generated from goods.json and strings.xml. Edit the data files.
package com.bluehorizon;

public final class GoodsCatalog {{
    private static final String[] IDS={java_list(LEGACY_IDS)};
    private static final String[] NAMES={java_list(names)};
    private static final String[] DESCRIPTIONS={java_list(descriptions)};
    private static final int[][] PRICES={{
        {rows}
    }};

    public static int slot(String id) {{
        for(int index=0;index<IDS.length;index++) {{
            if(IDS[index].equals(id)) return index;
        }}
        return -1;
    }}
    public static String[] names() {{ return NAMES.clone(); }}
    public static String description(int good) {{ return DESCRIPTIONS[good]; }}
    public static int price(int port,int good) {{ return PRICES[port][good]; }}
}}
""", encoding="utf-8")
    return output
