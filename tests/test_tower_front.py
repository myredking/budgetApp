"""Run the Android game's pure Java regression suites without a device."""
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def test_tower_front(tmp_path: Path) -> None:
    project = ROOT / "android-tower-front"
    java = Path.home() / ".codex/android-deckball-tools/jdk/bin"
    suffix = ".exe" if os.name == "nt" else ""
    names = ("Engine", "Campaign", "Profile", "SaveCodec", "BattleControls", "BattleEffects", "SceneStyle", "Skirmish")
    sources = [project / f"src/com/towerfront/{name}.java" for name in names]
    suites = sorted((project / "tests").glob("*.java"))
    subprocess.run([str(java / f"javac{suffix}"), "-encoding", "UTF-8",
                    "-d", str(tmp_path), *map(str, sources + suites)], check=True)
    for suite in suites:
        subprocess.run([str(java / f"java{suffix}"), "-cp", str(tmp_path),
                        f"com.towerfront.{suite.stem}"], check=True)
