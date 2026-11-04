from pathlib import Path
import subprocess


def test_block_engine(tmp_path: Path) -> None:
    root = Path(__file__).resolve().parents[1]
    java = Path.home() / '.codex/android-deckball-tools/jdk/bin'
    source = root / 'android-block-garden'
    subprocess.run([str(java / 'javac.exe'), '-encoding', 'UTF-8', '-d', str(tmp_path), str(source / 'src/com/blockgarden/Engine.java'), str(source / 'src/com/blockgarden/SavedState.java'), str(source / 'tests/EngineTest.java')], check=True)
    subprocess.run([str(java / 'java.exe'), '-cp', str(tmp_path), 'com.blockgarden.EngineTest'], check=True)
