"""Create original, short game effects without external audio sources."""
from array import array
import math
from pathlib import Path
import random
import sys
import wave
from generate_music import RATE, voice

CLIPS = (
    ('action', .14, ((0, 79, .12), (.025, 84, .10))),
    ('error', .22, ((0, 48, .18), (.04, 43, .16))),
    ('hit', .16, ((0, 43, .12),)),
    ('capture', .43, ((0, 72, .22), (.08, 76, .22), (.16, 79, .22))),
    ('reinforce', .38, ((0, 67, .25), (.06, 74, .25))),
    ('win', .85, ((0, 72, .4), (.12, 76, .4), (.24, 79, .4), (.36, 84, .4))),
    ('loss', .7, ((0, 67, .3), (.14, 63, .3), (.28, 60, .3))),
    ('skill_reinforce', .6, ((0, 76, .26), (.09, 79, .28), (.19, 84, .32))),
    ('skill_shield', .7, ((0, 60, .5), (.05, 67, .5), (.14, 84, .45))),
    ('skill_haste', .5, ((0, 67, .16), (.05, 72, .16), (.10, 79, .18), (.16, 86, .24))),
)


def impact(buffer: array) -> None:
    randomizer = random.Random(63)
    for index in range(round(.12 * RATE)):
        seconds = index / RATE
        envelope = (1 - math.exp(-seconds * 400)) * math.exp(-seconds * 45)
        envelope *= min(1, (.12 - seconds) * 80)
        buffer[index] += randomizer.uniform(-.22, .22) * envelope


def write(name: str, buffer: array) -> None:
    peak = max(abs(sample) for sample in buffer)
    pcm = array('h', (round(sample / peak * 21000) for sample in buffer))
    if sys.byteorder != 'little':
        pcm.byteswap()
    output = Path(__file__).resolve().parents[1] / 'res/raw' / f'sfx_{name}.wav'
    output.parent.mkdir(exist_ok=True)
    with wave.open(str(output), 'wb') as sound:
        sound.setnchannels(1)
        sound.setsampwidth(2)
        sound.setframerate(RATE)
        sound.writeframes(pcm.tobytes())


def main() -> None:
    for name, duration, notes in CLIPS:
        buffer = array('f', [0]) * round(duration * RATE)
        for start, note, length in notes:
            voice(buffer, start, length, note, .3, False)
        if name == 'hit':
            impact(buffer)
        write(name, buffer)
    print('Generated ten original game effects')


if __name__ == '__main__':
    main()
