import copy
import json
from pathlib import Path
import runpy

import pytest

ROOT = Path(__file__).resolve().parents[1]


def test_duplicate_market_rule_is_rejected() -> None:
    tool, data = load()
    data["marketRules"].append(data["marketRules"][0])
    with pytest.raises(ValueError, match="Duplicate market rule"):
        tool["market_rows"](data)


def load() -> tuple[dict, dict]:
    tool = runpy.run_path(str(ROOT / "tools" / "generate_goods.py"))
    return tool, json.loads((ROOT / "data" / "goods.json").read_text(encoding="utf-8"))


def test_reordering_goods_does_not_change_save_slots() -> None:
    tool, data = load()
    baseline = tool["validated_prices"](data)
    reordered = copy.deepcopy(data)
    reordered["goods"].reverse()
    assert tool["validated_prices"](reordered) == baseline


@pytest.mark.parametrize("defect", ["unknown_id", "duplicate_slot", "missing_price", "negative_price"])
def test_invalid_catalog_is_rejected(defect: str) -> None:
    tool, data = load()
    if defect == "unknown_id":
        data["goods"][0]["id"] = "unknown"
    elif defect == "duplicate_slot":
        data["goods"][1]["legacySlot"] = 0
    elif defect == "missing_price":
        del data["ports"][0]["wood"]
    else:
        data["ports"][0]["wood"] = -5
    with pytest.raises(ValueError):
        tool["validated_prices"](data)
