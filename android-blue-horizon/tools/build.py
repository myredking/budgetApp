"""Build a self-contained development APK using the local Android SDK."""
import os
from pathlib import Path
import subprocess
import zipfile

PROJECT = Path(__file__).resolve().parents[1]
TOOLS = Path.home() / ".codex/android-deckball-tools"
JAVA = TOOLS / "jdk/bin"
SDK = TOOLS / "build-tools/35.0.0"
ANDROID = TOOLS / "platforms/android-35/android.jar"
BUILD = PROJECT / "build"


def run(args: list[str | Path]) -> None:
    env = os.environ.copy()
    env["JAVA_HOME"] = str(TOOLS / "jdk")
    subprocess.run([str(arg) for arg in args], check=True, env=env, cwd=PROJECT)


def test() -> None:
    target = BUILD / "test"
    target.mkdir(parents=True, exist_ok=True)
    names = ["Engine", "Story", "SaveCodec", "World", "WorldRoutes", "Route", "MapProjection", "MapLabels", "SeaPaths", "AudioSession", "Crew", "TradeHint",
             "AdventureState", "Adventure", "VoyageEvents", "SeaBattle", "EventCatalog", "Contract", "Contracts", "Achievements", "CrewState", "CrewAbilities", "RefitState", "Refits", "RivalState", "Rivals", "TradeRace", "TownState", "TravelFlow", "TownLayout", "TownRpg", "ShipMotion", "TradeOrder", "CaptainPlan", "TownWalk", "ExpeditionState", "Expeditions", "CrewConversations", "RegionBoss", "ShipPerformance"]
    sources = [PROJECT / f"src/com/bluehorizon/{name}.java" for name in names]
    run([JAVA / "javac.exe", "-encoding", "UTF-8", "-d", target,
         *sources, *list((PROJECT / "tests").glob("*.java"))])
    run([JAVA / "java.exe", "-cp", target, "com.bluehorizon.EngineTest"])
    run([JAVA / "java.exe", "-cp", target, "com.bluehorizon.WorldTest"])
    run([JAVA / "java.exe", "-cp", target, "com.bluehorizon.SeaRouteTest"])
    run([JAVA / "java.exe", "-cp", target, "com.bluehorizon.PresentationTest"])
    run([JAVA / "java.exe", "-cp", target, "com.bluehorizon.AdventureTest"])
    run([JAVA / "java.exe", "-cp", target, "com.bluehorizon.CrewAbilitiesTest"])
    run([JAVA / "java.exe", "-cp", target, "com.bluehorizon.RefitTest"])
    run([JAVA / "java.exe", "-cp", target, "com.bluehorizon.RivalTest"])
    run([JAVA / "java.exe", "-cp", target, "com.bluehorizon.TownTest"])
    run([JAVA / "java.exe", "-cp", target, "com.bluehorizon.TradeOrderTest"])
    run([JAVA / "java.exe", "-cp", target, "com.bluehorizon.PortPlayTest"])
    run([JAVA / "java.exe", "-cp", target, "com.bluehorizon.MotionRpgTest"])
    run([JAVA / "java.exe", "-cp", target, "com.bluehorizon.ExpeditionTest"])
    run([JAVA / "java.exe", "-cp", target, "com.bluehorizon.ShipPerformanceTest"])
    run([JAVA / "java.exe", "-cp", target, "com.bluehorizon.CampaignAudit"])


def compile_app() -> Path:
    classes, dex = BUILD / "classes", BUILD / "dex"
    classes.mkdir(parents=True, exist_ok=True)
    dex.mkdir(parents=True, exist_ok=True)
    sources = list((PROJECT / "src").rglob("*.java"))
    run([JAVA / "javac.exe", "-encoding", "UTF-8", "-source", "8", "-target", "8",
         "-classpath", ANDROID, "-d", classes, *sources])
    run([JAVA / "java.exe", "-cp", SDK / "lib/d8.jar", "com.android.tools.r8.D8",
         "--lib", ANDROID, "--min-api", "26", "--output", dex, *classes.rglob("*.class")])
    resources = BUILD / "resources.zip"
    run([SDK / "aapt2.exe", "compile", "--dir", "res", "-o", "build/resources.zip"])
    unsigned = BUILD / "unsigned.apk"
    run([SDK / "aapt2.exe", "link", "-I", ANDROID, "--manifest",
         "AndroidManifest.xml", "-o", "build/unsigned.apk", "build/resources.zip"])
    with zipfile.ZipFile(unsigned, "a", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.write(dex / "classes.dex", "classes.dex")
        for asset in (PROJECT / "assets").glob("*"):
            compression = zipfile.ZIP_STORED if asset.suffix == ".wav" else zipfile.ZIP_DEFLATED
            archive.write(asset, "assets/" + asset.name, compress_type=compression)
    aligned = BUILD / "aligned.apk"
    run([SDK / "zipalign.exe", "-f", "-p", "4", unsigned, aligned])
    return aligned


def sign(aligned: Path) -> None:
    key = TOOLS / "blue-horizon-debug.jks"
    if not key.exists():
        run([JAVA / "keytool.exe", "-genkeypair", "-keystore", key, "-storepass", "android",
             "-keypass", "android", "-alias", "bluehorizon", "-dname", "CN=Blue Horizon Development",
             "-keyalg", "RSA", "-validity", "10000"])
    output = PROJECT / "BlueHorizon.apk"
    signer = [JAVA / "java.exe", "-jar", SDK / "lib/apksigner.jar"]
    run([*signer, "sign", "--ks", key, "--ks-pass", "pass:android", "--out", output, aligned])
    run([*signer, "verify", "--verbose", output])
    print(f"APK: {output}")


def build() -> None:
    test()
    sign(compile_app())


if __name__ == "__main__":
    build()
