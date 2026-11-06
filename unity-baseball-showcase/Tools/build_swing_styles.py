"""Build original gameplay swing variations, not measured player motion."""
from pathlib import Path
import sys
import json

sys.path.insert(0, str(Path(__file__).resolve().parent))
from build_player_review import build
from player_motion import load_profiles, swing_variant
from validate_player_equipment import check_bat


def main() -> None:
    root = Path(__file__).resolve().parents[1]
    source = root / "SourceArt/Source-GenericAthlete.blend"
    base = load_profiles(root / "player-profiles.json")[0]
    results = []
    for style in (1, 2, 3):
        profile = swing_variant(base, style)
        build(profile, source, root / "SourceArt/Players")
        result = {"id": profile["id"], **check_bat()}
        results.append(result)
        if not result["attachment_passed"]:
            raise RuntimeError(str(result))
    (root / "Reports/swing-style-validation.json").write_text(
        json.dumps(results, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
