"""Build a clean, signed Android APK with the locally installed SDK."""
import os
from pathlib import Path
import subprocess
import shutil
import tempfile
import zipfile

PROJECT = Path(__file__).resolve().parents[1]
TOOLS = Path.home() / '.codex/android-deckball-tools'
JAVA = TOOLS / 'jdk/bin'
BUILD_TOOLS = TOOLS / 'build-tools/35.0.0'
ANDROID = TOOLS / 'platforms/android-35/android.jar'
BUILD = PROJECT / 'build'


def run(args: list[str | Path]) -> None:
    env = os.environ.copy()
    env['JAVA_HOME'] = str(TOOLS / 'jdk')
    subprocess.run([str(a) for a in args], check=True, env=env)


def compile_package(directory: Path) -> Path:
    classes, dex = directory / 'classes', directory / 'dex'
    classes.mkdir()
    dex.mkdir()
    sources = list((PROJECT / 'src').rglob('*.java'))
    run([JAVA / 'javac.exe', '-encoding', 'UTF-8', '-source', '8', '-target', '8',
         '-classpath', ANDROID, '-d', classes, *sources])
    run([JAVA / 'java.exe', '-cp', BUILD_TOOLS / 'lib/d8.jar', 'com.android.tools.r8.D8',
         '--lib', ANDROID, '--min-api', '26', '--output', dex, *classes.rglob('*.class')])
    resources, unsigned = directory / 'resources.zip', directory / 'unsigned.apk'
    shutil.copytree(PROJECT / 'res', directory / 'res')
    shutil.copy2(PROJECT / 'AndroidManifest.xml', directory / 'AndroidManifest.xml')
    shutil.copy2(ANDROID, directory / 'android.jar')
    run([BUILD_TOOLS / 'aapt2.exe', 'compile', '--dir', directory / 'res', '-o', resources])
    run([BUILD_TOOLS / 'aapt2.exe', 'link', '-I', directory / 'android.jar', '--manifest',
         directory / 'AndroidManifest.xml', '-o', unsigned, resources])
    with zipfile.ZipFile(unsigned, 'a', compression=zipfile.ZIP_DEFLATED) as archive:
        archive.write(dex / 'classes.dex', 'classes.dex')
    aligned = directory / 'aligned.apk'
    run([BUILD_TOOLS / 'zipalign.exe', '-f', '4', unsigned, aligned])
    return aligned


def sign_package(aligned: Path) -> None:
    key = TOOLS / 'deckball-debug.jks'
    if not key.exists():
        raise FileNotFoundError('Existing signing key is required for compatible updates')
    output = PROJECT / 'TowerFront.apk'
    run([JAVA / 'java.exe', '-jar', BUILD_TOOLS / 'lib/apksigner.jar', 'sign', '--ks', key,
         '--ks-pass', 'pass:android', '--out', output, aligned])
    run([JAVA / 'java.exe', '-jar', BUILD_TOOLS / 'lib/apksigner.jar', 'verify', '--verbose', output])
    print(output)


def build() -> None:
    BUILD.mkdir(exist_ok=True)
    scratch = Path(os.environ.get('PUBLIC', 'C:/Users/Public')) / 'CodexTowerBuilds'
    scratch.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='tower-', dir=scratch) as directory:
        sign_package(compile_package(Path(directory)))


if __name__ == '__main__':
    build()
