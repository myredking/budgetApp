"""Check native MediaPlayer state, mute persistence and Activity ownership."""
import argparse
import re
import time
from check_ui_design import adb, nodes, tap


def playing(serial: str, uid: str) -> bool:
    output = adb(serial, 'shell', 'dumpsys', 'audio')
    players = [line for line in output.splitlines()
               if 'AudioPlaybackConfiguration' in line and 'type:android.media.MediaPlayer' in line
               and f'u/pid:{uid}/' in line]
    return any('state:started' in line for line in players)


def wait_playing(serial: str, uid: str, expected: bool) -> None:
    for _ in range(10):
        if playing(serial, uid) == expected:
            return
        time.sleep(.25)
    raise AssertionError(f'Music playing should become {expected}')


def launch(serial: str) -> None:
    adb(serial, 'shell', 'am', 'force-stop', 'com.towerfront')
    adb(serial, 'shell', 'am', 'start', '-n', 'com.towerfront/.MainActivity')
    nodes(serial)


def simulated_call(serial: str, uid: str) -> None:
    assert serial.startswith('emulator-'), 'Simulated calls require a dedicated emulator'
    try:
        adb(serial, 'emu', 'gsm', 'call', '5551234')
        wait_playing(serial, uid, False)
        print('PASS simulated incoming call pauses music')
    finally:
        adb(serial, 'emu', 'gsm', 'cancel', '5551234')
    adb(serial, 'shell', 'am', 'start', '-n', 'com.towerfront/.MainActivity')
    nodes(serial)
    wait_playing(serial, uid, True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--serial', required=True)
    parser.add_argument('--simulate-call', action='store_true')
    args = parser.parse_args()
    serial = args.serial
    uid = re.search(r'uid:(\d+)', adb(serial, 'shell', 'pm', 'list', 'packages', '-U', 'com.towerfront')).group(1)
    launch(serial)
    tap(serial, '소리·진동·연출 설정')
    item = next(n for n in nodes(serial) if n.get('text') == '배경음악')
    if item.get('checked') == 'false':
        tap(serial, '배경음악')
    wait_playing(serial, uid, True)
    tap(serial, '배경음악')
    wait_playing(serial, uid, False)
    tap(serial, '완료')
    launch(serial)
    tap(serial, '소리·진동·연출 설정')
    item = next(n for n in nodes(serial) if n.get('text') == '배경음악')
    assert item.get('checked') == 'false', 'Music mute was not saved'
    wait_playing(serial, uid, False)
    tap(serial, '배경음악')
    wait_playing(serial, uid, True)
    tap(serial, '완료')
    adb(serial, 'shell', 'input', 'keyevent', '3')
    wait_playing(serial, uid, False)
    adb(serial, 'shell', 'am', 'start', '-n', 'com.towerfront/.MainActivity')
    nodes(serial)
    wait_playing(serial, uid, True)
    if args.simulate_call:
        simulated_call(serial, uid)
    print('PASS music mute persistence, background pause and foreground resume')


if __name__ == '__main__':
    main()
