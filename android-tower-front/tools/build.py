import os
from pathlib import Path
import subprocess
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

def build() -> None:
    BUILD.mkdir(exist_ok=True)
    classes = BUILD / 'classes'
    classes.mkdir(exist_ok=True)
    sources = list((PROJECT / 'src').rglob('*.java'))
    run([JAVA / 'javac.exe', '-encoding', 'UTF-8', '-source', '8', '-target', '8', '-classpath', ANDROID, '-d', classes, *sources])
    dex = BUILD / 'dex'
    dex.mkdir(exist_ok=True)
    run([JAVA / 'java.exe', '-cp', BUILD_TOOLS / 'lib/d8.jar', 'com.android.tools.r8.D8', '--lib', ANDROID, '--min-api', '26', '--output', dex, *classes.rglob('*.class')])
    unsigned = BUILD / 'unsigned.apk'
    run([BUILD_TOOLS / 'aapt2.exe', 'link', '-I', ANDROID, '--manifest', PROJECT / 'AndroidManifest.xml', '-o', unsigned])
    with zipfile.ZipFile(unsigned, 'a', compression=zipfile.ZIP_DEFLATED) as archive:
        archive.write(dex / 'classes.dex', 'classes.dex')
    aligned = BUILD / 'aligned.apk'
    run([BUILD_TOOLS / 'zipalign.exe', '-f', '4', unsigned, aligned])
    key = TOOLS / 'deckball-debug.jks'
    if not key.exists():
        run([JAVA / 'keytool.exe','-genkeypair','-keystore',key,'-storepass','android','-keypass','android','-alias','deckball','-dname','CN=Deckball Development','-keyalg','RSA','-validity','10000'])
    output = PROJECT / 'TowerFront.apk'
    run([JAVA / 'java.exe', '-jar', BUILD_TOOLS / 'lib/apksigner.jar', 'sign', '--ks', key, '--ks-pass', 'pass:android', '--out', output, aligned])
    run([JAVA / 'java.exe', '-jar', BUILD_TOOLS / 'lib/apksigner.jar', 'verify', '--verbose', output])
    print(output)

if __name__ == '__main__':
    build()


