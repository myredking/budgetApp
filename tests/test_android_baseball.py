import os
from pathlib import Path
import shutil
import subprocess

import pytest

ROOT = Path(__file__).resolve().parents[1]


def java_tools() -> tuple[str, str]:
    java_home = os.environ.get('JAVA_HOME')
    local_tools = Path.home() / '.codex/android-deckball-tools/jdk'
    roots = [Path(java_home)] if java_home else []
    roots.append(local_tools)
    for root in roots:
        suffix = '.exe' if os.name == 'nt' else ''
        compiler, runtime = root / ('bin/javac' + suffix), root / ('bin/java' + suffix)
        if compiler.exists() and runtime.exists():
            return str(compiler), str(runtime)
    compiler, runtime = shutil.which('javac'), shutil.which('java')
    if compiler and runtime:
        return compiler, runtime
    pytest.skip('JDK required: set JAVA_HOME or run android-baseball/tools/bootstrap.py on Windows')


def test_android_game_engine(tmp_path: Path) -> None:
    source = ROOT / 'android-baseball/src/com/deckball/GameEngine.java'
    assert source.exists(), 'GameEngine implementation is missing (TDD red)'
    compiler, runtime = java_tools()
    subprocess.run([compiler, '-encoding', 'UTF-8', '-d', str(tmp_path), *[str(p) for p in source.parent.glob('*.java') if p.name not in ('MainActivity.java', 'FieldView.java')], str(ROOT / 'android-baseball/tests/EngineTest.java')], check=True)
    subprocess.run([runtime, '-cp', str(tmp_path), 'com.deckball.EngineTest'], check=True)
