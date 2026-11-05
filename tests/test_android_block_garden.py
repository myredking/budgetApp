from pathlib import Path
import subprocess


def test_block_engine(tmp_path: Path) -> None:
    root = Path(__file__).resolve().parents[1]
    java = Path.home() / '.codex/android-deckball-tools/jdk/bin'
    source = root / 'android-block-garden'
    subprocess.run([str(java / 'javac.exe'), '-encoding', 'UTF-8', '-d', str(tmp_path), str(source / 'src/com/blockgarden/Engine.java'), str(source / 'src/com/blockgarden/GameSnapshot.java'), str(source / 'src/com/blockgarden/EndSummary.java'), str(source / 'src/com/blockgarden/Mission.java'), str(source / 'src/com/blockgarden/SavedState.java'), str(source / 'tests/EngineTest.java')], check=True)
    subprocess.run([str(java / 'java.exe'), '-cp', str(tmp_path), 'com.blockgarden.EngineTest'], check=True)


def test_block_touch(tmp_path: Path) -> None:
    root = Path(__file__).resolve().parents[1] / 'android-block-garden'
    java = Path.home() / '.codex/android-deckball-tools/jdk/bin'
    subprocess.run([str(java / 'javac.exe'), '-encoding', 'UTF-8', '-d', str(tmp_path), str(root / 'src/com/blockgarden/TouchControl.java'), str(root / 'tests/TouchTest.java')], check=True)
    subprocess.run([str(java / 'java.exe'), '-cp', str(tmp_path), 'com.blockgarden.TouchTest'], check=True)


def test_start_navigation(tmp_path: Path) -> None:
    root = Path(__file__).resolve().parents[1] / 'android-block-garden'
    java = Path.home() / '.codex/android-deckball-tools/jdk/bin'
    subprocess.run([str(java / 'javac.exe'), '-encoding', 'UTF-8', '-d', str(tmp_path), str(root / 'src/com/blockgarden/Navigation.java'), str(root / 'tests/NavigationTest.java')], check=True)
    subprocess.run([str(java / 'java.exe'), '-cp', str(tmp_path), 'com.blockgarden.NavigationTest'], check=True)


def test_placement_guide(tmp_path: Path) -> None:
    root = Path(__file__).resolve().parents[1] / 'android-block-garden'
    java = Path.home() / '.codex/android-deckball-tools/jdk/bin'
    sources = [root / 'src/com/blockgarden/Engine.java', root / 'src/com/blockgarden/PlacementGuide.java', root / 'tests/GuideTest.java']
    subprocess.run([str(java / 'javac.exe'), '-encoding', 'UTF-8', '-d', str(tmp_path), *map(str, sources)], check=True)
    subprocess.run([str(java / 'java.exe'), '-cp', str(tmp_path), 'com.blockgarden.GuideTest'], check=True)


def test_block_dealer(tmp_path: Path) -> None:
    root = Path(__file__).resolve().parents[1] / 'android-block-garden'
    java = Path.home() / '.codex/android-deckball-tools/jdk/bin'
    subprocess.run([str(java / 'javac.exe'), '-encoding', 'UTF-8', '-d', str(tmp_path), str(root / 'src/com/blockgarden/Engine.java'), str(root / 'src/com/blockgarden/BlockDealer.java'), str(root / 'tests/DealerTest.java')], check=True)
    subprocess.run([str(java / 'java.exe'), '-cp', str(tmp_path), 'com.blockgarden.DealerTest'], check=True)


def test_screen_and_deals(tmp_path: Path) -> None:
    root = Path(__file__).resolve().parents[1] / 'android-block-garden'
    java = Path.home() / '.codex/android-deckball-tools/jdk/bin'
    names = ['Engine', 'BlockDealer', 'GameLayout']
    sources = [root / ('src/com/blockgarden/' + name + '.java') for name in names]
    subprocess.run([str(java / 'javac.exe'), '-encoding', 'UTF-8', '-d', str(tmp_path), *map(str, sources), str(root / 'tests/FeelTest.java')], check=True)
    subprocess.run([str(java / 'java.exe'), '-cp', str(tmp_path), 'com.blockgarden.FeelTest'], check=True)
