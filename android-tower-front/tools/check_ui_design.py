"""Validate the redesigned native menu on an explicit QA emulator."""
import argparse
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from check_navigation import adb


def nodes(serial: str) -> list[ET.Element]:
    for _ in range(4):
        adb(serial, 'shell', 'rm', '-f', '/sdcard/tower-design.xml')
        adb(serial, 'shell', 'uiautomator', 'dump', '/sdcard/tower-design.xml')
        try:
            xml = adb(serial, 'shell', 'cat', '/sdcard/tower-design.xml')
            return list(ET.fromstring(xml).iter('node'))
        except (subprocess.CalledProcessError, ET.ParseError):
            time.sleep(.5)
    raise AssertionError('Could not capture a fresh UI hierarchy')


def tap(serial: str, text: str) -> None:
    node = next(n for n in nodes(serial) if n.get('text') == text)
    a, b, c, d = map(int, re.findall(r'\d+', node.get('bounds', '')))
    adb(serial, 'shell', 'input', 'tap', str((a+c)//2), str((b+d)//2))


def check_menu(serial: str) -> None:
    texts = {node.get('text', '') for node in nodes(serial)}
    assert '작전 선택' in texts, 'Missing mode selection heading'
    assert '미션 모드 · 100스테이지' in texts, 'Missing mission card'
    assert '자유전 · 무작위 게임' in texts, 'Missing skirmish card'
    assert '목표를 달성하며 100개 작전에 도전하세요' in texts
    assert '맵과 적 진영 수를 골라 나만의 전투를 만드세요' in texts


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--serial', required=True)
    serial = parser.parse_args().serial
    adb(serial, 'shell', 'am', 'force-stop', 'com.towerfront')
    adb(serial, 'shell', 'am', 'start', '-n', 'com.towerfront/.MainActivity')
    check_menu(serial)
    tap(serial, '미션 모드 · 100스테이지')
    assert any(n.get('text') == '미션 지도' for n in nodes(serial))
    adb(serial, 'shell', 'input', 'keyevent', '4')
    check_menu(serial)
    tap(serial, '자유전 · 무작위 게임')
    assert any(n.get('text') == '전투 시작' for n in nodes(serial))
    adb(serial, 'shell', 'input', 'keyevent', '4')
    check_menu(serial)
    print('PASS mode cards and navigation')


if __name__ == '__main__':
    main()
