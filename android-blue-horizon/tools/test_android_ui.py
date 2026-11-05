"""Run real Activity import/arrival regression without replacing user progress."""
from pathlib import Path
import subprocess

PROJECT = Path(__file__).resolve().parents[1]
BUILD = PROJECT / "build/ui-smoke"
SDK = Path("C:/ProgramData/BlueHorizonPreview/sdk")
JAVA = SDK / "jdk/bin"
BT = SDK / "build-tools/35.0.0"


def run(args: list[str | Path]) -> None:
    subprocess.run([str(arg) for arg in args], check=True, cwd=PROJECT)


def build() -> Path:
    BUILD.mkdir(parents=True, exist_ok=True)
    classes = BUILD / "classes"
    classes.mkdir(exist_ok=True)
    android = SDK / "platforms/android-35/android.jar"
    run([JAVA / "javac.exe", "-encoding", "UTF-8", "-source", "8", "-target", "8",
         "-cp", f"{android};{PROJECT / 'build/classes'}", "-d", classes, PROJECT / "androidtests/UiSmoke.java"])
    run([JAVA / "java.exe", "-cp", BT / "lib/d8.jar", "com.android.tools.r8.D8", "--lib", android,
         "--min-api", "26", "--output", BUILD, *classes.rglob("*.class")])
    apk = BUILD / "test.apk"
    run([BT / "aapt2.exe", "link", "-I", android, "--manifest", PROJECT / "androidtests/AndroidManifest.xml", "-o", apk])
    import zipfile
    with zipfile.ZipFile(apk, "a") as package:
        package.write(BUILD / "classes.dex", "classes.dex")
    run([JAVA / "java.exe", "-jar", BT / "lib/apksigner.jar", "sign", "--ks", SDK / "blue-horizon-debug.jks",
         "--ks-pass", "pass:android", "--key-pass", "pass:android", apk])
    return apk


def main() -> None:
    apk = build()
    adb = SDK / "platform-tools/adb.exe"
    run([adb, "-s", "emulator-5554", "install", "-r", apk])
    result = subprocess.run([str(adb), "-s", "emulator-5554", "shell", "am", "instrument", "-w",
                             "com.bluehorizon.tests/com.bluehorizon.UiSmoke"], capture_output=True, text=True, check=True)
    print(result.stdout)
    if "PASS: in-transit import" not in result.stdout:
        raise RuntimeError("Android Activity regression failed")


if __name__ == "__main__":
    main()
