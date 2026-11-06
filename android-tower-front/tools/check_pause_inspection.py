"""Inspect a paused battle without changing time, troops, dispatches or skills."""
import argparse
from pathlib import Path
import re
import struct
import subprocess
from check_ui_design import adb, nodes, tap
from check_navigation import saved_battle


def positions(serial: str) -> dict[str, tuple[int, int]]:
    view = next(n for n in nodes(serial) if n.get('content-desc', '').startswith('전투 일시정지'))
    left, top, right, bottom = map(int, re.findall(r'\d+', view.get('bounds', '')))
    density = re.findall(r'density:\s*(\d+)', adb(serial, 'shell', 'wm', 'density'))[-1]
    scale = adb(serial, 'shell', 'settings', 'get', 'system', 'font_scale').strip()
    font = float(scale) if scale != 'null' else 1
    project = Path(__file__).resolve().parents[1]
    classes = project / 'build/pause-probe'
    classes.mkdir(parents=True, exist_ok=True)
    java = Path.home() / '.codex/android-deckball-tools/jdk/bin'
    sources = [project / f'src/com/towerfront/{name}.java' for name in ('ScreenLayout', 'PauseMenuLayout')]
    subprocess.run([str(java / 'javac.exe'), '-encoding', 'UTF-8', '-d', str(classes),
                    *map(str, sources), str(project / 'tools/PlanProbe.java')], check=True)
    output = subprocess.check_output([str(java / 'java.exe'), '-cp', str(classes), 'com.towerfront.PlanProbe',
                                      str(right-left), str(bottom-top), str(int(density)/160), str(font)], text=True)
    result = {}
    for line in output.splitlines():
        name, x, y = line.split(',')
        result[name] = (round(left+float(x)), round(top+float(y)))
    return result


def touch(serial: str, point: tuple[int, int]) -> None:
    adb(serial, 'shell', 'input', 'tap', *map(str, point))


def check_inspection(serial: str, points: dict[str, tuple[int, int]]) -> bytes:
    before = saved_battle(serial)
    touch(serial, points['inspect'])
    assert any(n.get('content-desc', '').startswith('전장 확인 중') for n in nodes(serial))
    touch(serial, points['zoom'])
    adb(serial, 'shell', 'input', 'swipe', *map(str, (*points['map1'], *points['map2'], 300)))
    touch(serial, points['map1'])
    touch(serial, points['map2'])
    touch(serial, points['skill'])
    assert any(n.get('content-desc', '').startswith('전장 확인 중') for n in nodes(serial))
    adb(serial, 'shell', 'input', 'keyevent', '4')
    assert any(n.get('content-desc', '').startswith('전투 일시정지') for n in nodes(serial))
    assert saved_battle(serial) == before, 'Inspection changed battle state'
    print('PASS inspection zoom/pan/tower/skill taps preserve exact battle state')
    return before


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--serial', required=True)
    serial = parser.parse_args().serial
    adb(serial, 'shell', 'am', 'force-stop', 'com.towerfront')
    adb(serial, 'shell', 'am', 'start', '-n', 'com.towerfront/.MainActivity')
    resume = next(n.get('text') for n in nodes(serial) if n.get('text', '').endswith('이어하기'))
    tap(serial, resume)
    adb(serial, 'shell', 'input', 'keyevent', '4')
    points = positions(serial)
    before = check_inspection(serial, points)
    touch(serial, points['inspect'])
    nodes(serial)
    adb(serial, 'shell', 'input', 'keyevent', '3')
    adb(serial, 'shell', 'am', 'start', '-n', 'com.towerfront/.MainActivity')
    assert any(n.get('content-desc', '').startswith('전투 일시정지') for n in nodes(serial))
    assert saved_battle(serial) == before, 'Backgrounding inspection advanced battle'
    print('PASS background/foreground safely restores the paused menu')
    touch(serial, points['inspect'])
    nodes(serial)
    touch(serial, points['resume'])
    assert any(n.get('content-desc', '').startswith('타워 프론트 전장') for n in nodes(serial))
    adb(serial, 'shell', 'input', 'keyevent', '4')
    nodes(serial)
    after = saved_battle(serial)
    assert struct.unpack_from('>d', after, 12)[0] > struct.unpack_from('>d', before, 12)[0]
    assert after[32:36] == before[32:36], 'Inspection dispatched troops'
    print('PASS explicit resume advances time without extra dispatches')


if __name__ == '__main__':
    main()
