"""Generate the game's original soft island theme with only the standard library."""
from array import array
import math
from pathlib import Path
import sys
import wave

RATE = 22050
BEAT = 60 / 90


def voice(buffer: array, start: float, duration: float, midi: int, gain: float, pad: bool) -> None:
    frequency = 440 * 2 ** ((midi - 69) / 12)
    offset = round(start * RATE)
    for index in range(round(duration * RATE)):
        seconds = index / RATE
        phase = 2 * math.pi * frequency * seconds
        if pad:
            envelope = math.sin(math.pi * seconds / duration) ** .8
            signal = math.sin(phase) + .15 * math.sin(phase * 2)
        else:
            envelope = (1 - math.exp(-seconds * 180)) * math.exp(-seconds * 3.2)
            envelope *= min(1, (duration - seconds) * 20)
            signal = math.sin(phase) + .35 * math.sin(phase * 2) + .12 * math.sin(phase * 3)
        at = (offset + index) % len(buffer)
        buffer[at] += gain * envelope * signal


def compose() -> array:
    buffer = array('f', [0]) * round(64 * BEAT * RATE)
    chords = ((48, 52, 55, 59), (45, 48, 52, 55), (41, 45, 48, 52), (43, 47, 50, 55))
    melodies = ((72, 76, 79, 76, 74, 72, 76, 79), (72, 76, 81, 79, 76, 72, 69, 72),
                (72, 77, 76, 72, 69, 72, 76, 77), (74, 79, 77, 74, 71, 74, 79, 74))
    for section in range(8):
        start = section * 8 * BEAT
        chord = chords[section % 4]
        for note in chord:
            voice(buffer, start, 8 * BEAT, note, .025, True)
        for beat, note in enumerate(melodies[section % 4]):
            if beat not in (3, 7):
                voice(buffer, start + beat * BEAT, 1.8, note, .065, False)
        for beat in (0, 4):
            voice(buffer, start + beat * BEAT, 2.0, chord[0] - 12, .05, False)
    return buffer


def main() -> None:
    buffer = compose()
    peak = max(abs(sample) for sample in buffer)
    pcm = array('h', (round(sample / peak * 22000) for sample in buffer))
    if sys.byteorder != 'little':
        pcm.byteswap()
    output = Path(__file__).resolve().parents[1] / 'res/raw/island_theme.wav'
    output.parent.mkdir(exist_ok=True)
    with wave.open(str(output), 'wb') as sound:
        sound.setnchannels(1)
        sound.setsampwidth(2)
        sound.setframerate(RATE)
        sound.writeframes(pcm.tobytes())
    print(output)


if __name__ == '__main__':
    main()
