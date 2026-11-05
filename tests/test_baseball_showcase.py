from pathlib import Path
import importlib.util
import pytest

ROOT = Path(__file__).resolve().parents[1] / "unity-baseball-showcase"


def validator() -> object:
    path = ROOT / "Tools/validate_assets.py"
    assert path.exists(), "Visual quality preparation tool is missing"
    spec = importlib.util.spec_from_file_location("showcase_validation", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_missing_art_is_not_reported_as_playable(tmp_path: Path) -> None:
    module = validator()
    result = module.inspect_assets(tmp_path)
    assert not result["ready"]
    assert "Batter.prefab" in " ".join(result["missing"])
    assert "Stadium.prefab" in " ".join(result["missing"])


def test_visual_contract_requires_real_assets() -> None:
    import json
    contract = json.loads((ROOT / "visual-contract.json").read_text(encoding="utf-8"))
    assert contract["scope"] == {"batters": 1, "pitchers": 1, "stadiums": 1}
    assert contract["generated_primitive_players_allowed"] is False
    assert contract["acceptance"]["preview_resolution"] == [1920, 1080]


def test_motion_review_is_required_even_with_visual_approval(tmp_path: Path) -> None:
    import json
    module = validator()
    art = tmp_path / "Assets/Showcase/Art"
    art.mkdir(parents=True)
    for name in ("Batter.prefab", "Pitcher.prefab", "Stadium.prefab", "Bat.prefab", "Ball.prefab"):
        (art / name).write_text("fixture", encoding="utf-8")
    report = tmp_path / "Reports/editor-validation.json"
    report.parent.mkdir()
    report.write_text(json.dumps({"passed": True, "visual_review_passed": True}), encoding="utf-8")
    assert not module.inspect_assets(tmp_path)["ready"]


@pytest.mark.parametrize("content", ["[]", "null", "false", '"unexpected"'])
def test_non_object_report_is_not_ready(tmp_path: Path, content: str) -> None:
    module = validator()
    report = tmp_path / "Reports/editor-validation.json"
    report.parent.mkdir()
    report.write_text(content, encoding="utf-8")
    assert not module.inspect_assets(tmp_path)["ready"]
