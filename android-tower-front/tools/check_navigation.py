"""Android emulator regression: Back must return from campaign to the start screen."""
import argparse
import base64
import re
from pathlib import Path
import subprocess
import struct
import xml.etree.ElementTree as ET

ADB = Path.home() / '.codex/android-deckball-tools/platform-tools/adb.exe'


def adb(serial: str, *args: str) -> str:
    return subprocess.check_output([str(ADB), '-s', serial, *args], encoding='utf-8')


def nodes(serial: str) -> list[ET.Element]:
    adb(serial, 'shell', 'uiautomator', 'dump', '/sdcard/tower-nav.xml')
    return list(ET.fromstring(adb(serial, 'shell', 'cat', '/sdcard/tower-nav.xml')).iter('node'))


def tap(serial: str, text: str) -> None:
    node = next(n for n in nodes(serial) if n.get('text') == text)
    a, b, c, d = map(int, re.findall(r'\d+', node.get('bounds', '')))
    adb(serial, 'shell', 'input', 'tap', str((a+c)//2), str((b+d)//2))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--serial', required=True)
    serial = parser.parse_args().serial
    adb(serial, 'shell', 'am', 'force-stop', 'com.towerfront')
    adb(serial, 'shell', 'am', 'start', '-n', 'com.towerfront/.MainActivity')
    tap(serial, '미션 모드 · 100스테이지')
    adb(serial, 'shell', 'input', 'keyevent', '4')
    assert any(n.get('text') == 'TOWER FRONT' for n in nodes(serial)), 'Back exited campaign instead of returning home'
    print('PASS campaign Back returns to start screen')
    check_battle_back(serial)


def saved_battle(serial: str) -> bytes:
    xml = adb(serial, 'shell', 'cat', '/data/data/com.towerfront/shared_prefs/MainActivity.xml')
    text = next(n.text for n in ET.fromstring(xml) if n.get('name') == 'battle')
    return base64.b64decode(text)


def check_battle_back(serial: str) -> None:
    resume = next(n.get('text') for n in nodes(serial) if n.get('text', '').endswith('이어하기'))
    tap(serial, resume)
    adb(serial, 'shell', 'input', 'keyevent', '4')
    assert any(n.get('content-desc', '').startswith('전투 일시정지') for n in nodes(serial))
    before = saved_battle(serial)
    adb(serial, 'shell', 'input', 'keyevent', '4')
    assert any(n.get('text') == 'TOWER FRONT' for n in nodes(serial))
    assert saved_battle(serial) == before, 'Paused battle changed when returning to menu'
    tap(serial, resume)
    adb(serial, 'shell', 'input', 'keyevent', '4')
    nodes(serial)
    after = saved_battle(serial)
    assert after[4:12] == before[4:12], 'Battle identity changed'
    assert struct.unpack_from('>d', after, 12)[0] >= struct.unpack_from('>d', before, 12)[0]
    print('PASS Back pauses, second Back opens menu, continue preserves battle')


if __name__ == '__main__':
    main()
