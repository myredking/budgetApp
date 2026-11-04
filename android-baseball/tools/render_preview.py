from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
JAVA = Path.home() / '.codex/android-deckball-tools/jdk/bin'

def render() -> None:
    classes = ROOT / 'build/preview'
    classes.mkdir(parents=True, exist_ok=True)
    sources = list((ROOT / 'tools/preview').rglob('*.java'))
    sources += list((ROOT / 'src/com/deckball').glob('*.java'))
    sources = [p for p in sources if p.name == 'FieldView.java' or 'import android.' not in p.read_text(encoding='utf-8-sig')]
    subprocess.run([str(JAVA / 'javac.exe'), '-encoding', 'UTF-8', '-d', str(classes), *map(str,sources)], check=True)
    output = ROOT / 'previews/field-2026.png'
    output.parent.mkdir(exist_ok=True)
    subprocess.run([str(JAVA / 'java.exe'), '-cp', str(classes), 'com.deckball.Preview', str(output)], check=True)
    print(output)

if __name__ == '__main__':
    render()
